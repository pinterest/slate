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

import org.apache.commons.configuration2.Configuration;

import com.google.gson.JsonObject;

public class CoreTaskRuntime implements TaskRuntime {

  @Override
  public void configure(Configuration configuration) throws Exception {
  }

  @Override
  public StatusUpdate startExecution(String taskDefinitionId,
                                     String taskInstanceId,
                                     LifecycleProcess workflow) {
    TaskDefinition task = TaskFactory.INSTANCE.getTask(taskDefinitionId);
    try {
      JsonObject processContext = workflow.getProcessContext();
      return task.startExecution(this, taskInstanceId, workflow, processContext,
          processContext.has(taskInstanceId)
              ? processContext.get(taskInstanceId).getAsJsonObject()
              : null);
    } catch (Exception e) {
      e.printStackTrace();
      return StatusUpdate.create(Status.FAILED, "Task failed to start", e);
    }
  }

  @Override
  public StatusUpdate checkStatus(String taskTemplateId,
                                  String taskInstanceId,
                                  LifecycleProcess workflow) {
    TaskDefinition runningTask = TaskFactory.INSTANCE.getTask(taskTemplateId);
    try {
      JsonObject processContext = workflow.getProcessContext();
      return runningTask.checkStatus(this, taskInstanceId, workflow, processContext,
          processContext.has(taskInstanceId) ? processContext.get(taskInstanceId).getAsJsonObject()
              : null);
    } catch (Exception e) {
      e.printStackTrace();
      return StatusUpdate.create(Status.FAILED, "Status check failed", e);
    }
  }

}
