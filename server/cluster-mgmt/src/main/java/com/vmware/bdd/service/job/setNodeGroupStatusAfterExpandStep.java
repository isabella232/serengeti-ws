/***************************************************************************
 * Copyright (c) 2012-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.service.job;

import com.vmware.bdd.apitypes.NodeStatus;
import com.vmware.bdd.entity.NodeEntity;
import com.vmware.bdd.entity.NodeGroupEntity;
import com.vmware.bdd.exception.TaskException;
import com.vmware.bdd.service.impl.ClusterLdapUserMgmtCfgService;
import com.vmware.bdd.service.job.software.ManagementOperation;
import com.vmware.bdd.utils.CommonUtil;
import org.apache.log4j.Logger;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.ArrayList;
import java.util.List;

public class setNodeGroupStatusAfterExpandStep extends TrackableTasklet {
   private static final Logger logger = Logger.getLogger(setNodeGroupStatusAfterExpandStep.class);

   @Override
   public RepeatStatus executeStep(ChunkContext chunkContext, JobExecutionStatusHolder jobExecutionStatusHolder) {
      String clusterName = getJobParameters(chunkContext).getString(JobConstants.CLUSTER_NAME_JOB_PARAM);
      String expandNodeGroupList =
              TrackableTasklet.getJobParameters(chunkContext).getString(
                      JobConstants.NEW_NODE_GROUP_LIST_JOB_PARAM);

      for (String nodeGroupName: expandNodeGroupList.split(",")) {
         NodeGroupEntity group =
                 clusterEntityMgr.findByName(clusterName, nodeGroupName);
         group.setIsProvisioning(false);
         clusterEntityMgr.update(group);
      }

      return RepeatStatus.FINISHED;
   }

}

