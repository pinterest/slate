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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JoinTaskDefinition extends TaskDefinition {

  public static final String BLOCKING_TASKS = "blockingTasks";
  public static final String TASK_DEFINITION_ID = "joinTask";

  public JoinTaskDefinition() {
    super(TASK_DEFINITION_ID);
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    return null;
  }

  @Override
  public StatusUpdate checkStatus(TaskRuntime runtime,
                                  String taskId,
                                  LifecycleProcess process,
                                  JsonObject processContext,
                                  JsonObject taskContext) throws Exception {
    for (JsonElement e : taskContext.get(BLOCKING_TASKS).getAsJsonArray()) {
      Task task = process.getAllTasks().get(e.getAsString());
      Status status = task.getStatus();
      if (!Status.isComplete(status)) {
        return StatusUpdate.create(Status.RUNNING);
      } else if (status == Status.FAILED || status == Status.CANCELLED) {
        return StatusUpdate.create(status, "Failed due to failed blocking task:" + e.getAsString());
      }
    }
    return StatusUpdate.create(Status.SUCCEEDED);
  }

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    if (!taskContext.has(BLOCKING_TASKS)) {
      throw new Exception("Missing blockingTasks");
    }
    if (!taskContext.get(BLOCKING_TASKS).isJsonArray()) {
      throw new Exception("blockingTasks must be an array");
    }
    // check if the task actually exists for Join to block on
    for (JsonElement jsonElement : taskContext.get(BLOCKING_TASKS).getAsJsonArray()) {
      process.getAllTasks().containsKey(jsonElement.getAsString());
    }
  }

}
