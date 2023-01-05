/**
 * Copyright 2023 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.slate.process;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ServiceUnavailableException;

import com.google.gson.JsonObject;
import com.pinterest.slate.utils.HttpUtils;

public class RPCTaskDefinition extends TaskDefinition {

  private static final Logger logger = Logger.getLogger(RPCTaskDefinition.class.getCanonicalName());
  private String url;

  public RPCTaskDefinition(String url, String taskDefinitionId) {
    this.url = url;
    this.taskDefinitionId = taskDefinitionId;
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    try {
      StatusUpdate makeHttpPost = HttpUtils.makeHttpPost(url + "/" + taskId + "/execution", process,
          StatusUpdate.class, true);
      return makeHttpPost;
    } catch (ServiceUnavailableException e) {
      logger.log(Level.SEVERE, "Failed to start task execution for taskdef(" + taskDefinitionId + ")", e);
      return StatusUpdate.create(Status.RUNNING);
    }
  }

  @Override
  public StatusUpdate checkStatus(TaskRuntime runtime,
                                  String taskId,
                                  LifecycleProcess process,
                                  JsonObject processContext,
                                  JsonObject taskContext) throws Exception {
    try {
      return HttpUtils.makeHttpPost(url + "/" + taskId + "/status", process, StatusUpdate.class,
          true);
    } catch (ServiceUnavailableException e) {
      logger.log(Level.SEVERE, "Failed to check task status for taskdef(" + taskDefinitionId + ")", e);
      return StatusUpdate.create(Status.RUNNING);
    }
  }

  @Override
  public void validate(String taskId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    HttpUtils.makeHttpPost(url + "/" + taskId + "/validation", process, Void.class);
  }

  public String getUrl() {
    return url;
  }

}
