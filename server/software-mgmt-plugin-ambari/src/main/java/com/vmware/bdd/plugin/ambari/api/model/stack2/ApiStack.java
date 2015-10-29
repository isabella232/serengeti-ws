/***************************************************************************
 * Copyright (c) 2014-2015 VMware, Inc. All Rights Reserved.
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
package com.vmware.bdd.plugin.ambari.api.model.stack2;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ApiStack {
   @Expose
   private String href;

   @Expose
   @SerializedName("Stacks")
   private ApiStackName apiStackName;

   @Expose
   @SerializedName("versions")
   private List<ApiStackVersion> apiStackVersions;

   public String getHref() {
      return href;
   }

   public void setHref(String href) {
      this.href = href;
   }

   public ApiStackName getApiStackName() {
      return apiStackName;
   }

   public void setApiStackName(ApiStackName apiStackName) {
      this.apiStackName = apiStackName;
   }

   public List<ApiStackVersion> getApiStackVersions() {
      return apiStackVersions;
   }

   public void setApiStackVersions(List<ApiStackVersion> apiStackVersions) {
      this.apiStackVersions = apiStackVersions;
   }
}
