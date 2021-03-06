/***************************************************************************
 * Copyright (c) 2015 VMware, Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/
package com.vmware.bdd.service.resmgmt.impl;

import com.vmware.aurora.global.Configuration;
import com.vmware.aurora.util.AuAssert;
import com.vmware.aurora.vc.VcDatacenter;
import com.vmware.aurora.vc.VcInventory;
import com.vmware.aurora.vc.VcObject;
import com.vmware.bdd.service.resmgmt.IVcInventorySyncService;
import com.vmware.bdd.service.resmgmt.VC_RESOURCE_TYPE;
import com.vmware.bdd.service.resmgmt.sync.AbstractSyncVcResSP;
import com.vmware.bdd.service.resmgmt.sync.SyncVcResourceSp;
import com.vmware.bdd.service.resmgmt.sync.filter.VcResourceFilters;
import com.vmware.bdd.utils.Constants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.vmware.aurora.global.ThreadPoolConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by xiaoliangl on 7/22/15.
 */
@Service
public class VcInventorySyncService implements IVcInventorySyncService {
   private static final Logger LOGGER = Logger.getLogger(VcInventorySyncService.class);

//   private ExecutorService es = Executors.newFixedThreadPool(10);//mark

   private static final String[] DEFAULT_VALUE = {"10","50","1000"};

   private static final String NAME_PREFIX = "VcInventorySync-";

   private ThreadPoolTaskExecutor es = null;

   private AtomicBoolean inProgress = new AtomicBoolean(false);

   private VcInventorySyncCounters counters = new VcInventorySyncCounters();

   private long waitMilliSecs = 100l;

   public void setCounters(VcInventorySyncCounters counters) {
      this.counters = counters;
   }

   public VcInventorySyncService() {
      ThreadPoolConfig config = new ThreadPoolConfig(Configuration.getThreadConfig(Constants.VCINVENTORYREFRESH_THREADPOOL_CONFIG, DEFAULT_VALUE));
      LOGGER.debug(config.toString());
      es = new ThreadPoolTaskExecutor();
      es.setCorePoolSize(config.getCorePoolSize());
      es.setMaxPoolSize(config.getMaxPoolSize());
      es.setQueueCapacity(config.getWorkQueue());
      es.setThreadNamePrefix(NAME_PREFIX);
      es.initialize();
   }

   /**
    * refresh the vc inventory, some resource will be filtered from refreshing.
    */
   @Override
   public void refreshInventory() throws InterruptedException {
      refreshInventory(null);
   }

   /**
    * @param vcResourceFilters
    * refresh the vc inventory, some resource will be filtered from refreshing.
    */
   public synchronized void refreshInventory(VcResourceFilters vcResourceFilters) throws InterruptedException {
      inProgress.set(true);

      if (LOGGER.isInfoEnabled()) {
         LOGGER.info("start a inventory refresh.");
      }
      counters.setRefreshInProgress(true);
      counters.increaseInvRefresh();
      try {
         List<VcDatacenter> dcList = VcInventory.getDatacenters();
         refresh(dcList, vcResourceFilters);
      } finally {
         counters.setRefreshInProgress(false);
         inProgress.set(false);
         if (LOGGER.isInfoEnabled()) {
            LOGGER.info("the inventory refresh is done.");
         }
      }
   }


   @Override
   public void asyncRefreshInventory(final VcResourceFilters filters) {
      if(LOGGER.isInfoEnabled()) {
         LOGGER.info("trigger asyncRefreshInventory.");
      }
      es.submit(new Runnable() {
         @Override
         public void run() {
            try {
               if(LOGGER.isInfoEnabled()) {
                  LOGGER.info("asyncRefreshInventory started.");
               }
               refreshInventory(filters);
               if(LOGGER.isInfoEnabled()) {
                  LOGGER.info("asyncRefreshInventory end.");
               }
            } catch (InterruptedException e) {
               LOGGER.error("asyncRefreshInventory failed", e);
            }
         }
      });
   }

   @Override
   public void asyncRefreshUsedDatastores(String[] dsNames) {
      VcResourceFilters filters = new VcResourceFilters();
      filters.addFilterByType(VC_RESOURCE_TYPE.HOST)
            .addFilterByType(VC_RESOURCE_TYPE.RESOURCE_POOL)
            .addFilterByType(VC_RESOURCE_TYPE.CLUSTER)
            .addFilterByType(VC_RESOURCE_TYPE.DATA_CENTER);
      filters.addNameFilter(VC_RESOURCE_TYPE.DATA_STORE, dsNames, false);

      asyncRefreshInventory(filters);
   }

   @Override
   public boolean isRefreshInProgress() {
      return inProgress.get();
   }

   /**
    * wait for in progress refresh's completion
    *
    * @throws InterruptedException if the wait is interrupted
    */
   public void waitForCompletion() throws InterruptedException {
      while (inProgress.get()) {
         Thread.sleep(waitMilliSecs);
      }
   }

   /**
    * refresh vc resources concurrently.
    * @param vcResourceFilters vc resource filters.
    * @param vcObjects vc resource objects to refresh
    */
   protected <T extends VcObject> void refresh(List<T> vcObjects, VcResourceFilters vcResourceFilters) {
      AuAssert.check(CollectionUtils.isNotEmpty(vcObjects), "no vc resources to refresh!");


      List<AbstractSyncVcResSP> syncSps = new ArrayList<>();

      for (T vcObject : vcObjects) {
         SyncVcResourceSp syncVcResourceSp = new SyncVcResourceSp(vcObject.getMoRef());
         syncVcResourceSp.setVcResourceFilters(vcResourceFilters);
         syncSps.add(syncVcResourceSp);
      }

      try {
         work(es, syncSps);
      } catch (Exception ex) {
         LOGGER.error("refresh Vc Resources failed.", ex);
      }
   }

   private List<Future<List<AbstractSyncVcResSP>>> submit(ThreadPoolTaskExecutor es, List<AbstractSyncVcResSP> syncSps) {

      if (CollectionUtils.isNotEmpty(syncSps)) {
         List<Future<List<AbstractSyncVcResSP>>> newRefreshTaskList = new ArrayList();
         for (AbstractSyncVcResSP sp : syncSps) {
            counters.increasePendingRefresh();
            newRefreshTaskList.add(es.submit(sp));
         }
         return newRefreshTaskList;
      } else {
         return Collections.EMPTY_LIST;
      }
   }

   private void work(ThreadPoolTaskExecutor es, List<AbstractSyncVcResSP> syncSps) throws ExecutionException, InterruptedException {
      List<Future<List<AbstractSyncVcResSP>>> resultList = submit(es, syncSps);

      while (resultList.size() > 0) {
         Thread.sleep(waitMilliSecs);

         List<AbstractSyncVcResSP> newTaskList = new ArrayList<>();
         for (Iterator<Future<List<AbstractSyncVcResSP>>> resultIterator = resultList.iterator(); resultIterator.hasNext(); ) {
            Future<List<AbstractSyncVcResSP>> result = resultIterator.next();
            if (result.isDone()) {
               counters.decreasePendingRefresh();
               counters.increaseFinishedRefresh();

               newTaskList.addAll(result.get());
               resultIterator.remove();
            }
         }

         resultList.addAll(submit(es, newTaskList));

         if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("remain refresh task count: " + resultList.size());
         }
      }
   }
}
