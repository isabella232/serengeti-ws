/***************************************************************************
 * Copyright (c) 2012-2013 VMware, Inc. All Rights Reserved.
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

package com.vmware.aurora.composition;

import com.vmware.aurora.vc.VcVirtualMachine;
import com.vmware.aurora.vc.VcVirtualMachine.GuestVarReturnCode;

public class QueryAgentReturn implements IPrePostPowerOn {
   protected VcVirtualMachine vm;
   protected GuestVarReturnCode returnCode;
   protected int timeOutSecs;

   public QueryAgentReturn(int timeOutSecs) {
      this.timeOutSecs = timeOutSecs;
   }

   @Override
   public void setVm(VcVirtualMachine vm) {
      this.vm = vm;
   }

   @Override
   public VcVirtualMachine getVm() {
     return vm;
   }

   @Override
   public Void call() throws Exception {
      returnCode = vm.waitForPowerOnResult(timeOutSecs == 0 ? null : Integer.valueOf(timeOutSecs));
      return null;
   }
}
