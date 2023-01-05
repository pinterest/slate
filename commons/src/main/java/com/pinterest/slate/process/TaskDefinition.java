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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pinterest.slate.human.TaskSystem;

public abstract class TaskDefinition {

  public static final Gson GSON = new Gson();
  protected String taskDefinitionId;

  public TaskDefinition() {
  }

  public TaskDefinition(String taskDefinitionId) {
    this.taskDefinitionId = taskDefinitionId;
  }

  public void init(TaskSystem engine) throws Exception {
  }

  /**
   * @param runtime
   * @param taskId
   * @param process
   * @param processContext entire context for the process
   * @param taskContext    null if no task context was added to this
   * @return
   * @throws Exception
   */
  public abstract StatusUpdate startExecution(TaskRuntime runtime,
                                              String taskId,
                                              LifecycleProcess process,
                                              JsonObject processContext,
                                              JsonObject taskContext) throws Exception;

  public abstract StatusUpdate checkStatus(TaskRuntime runtime,
                                           String taskId,
                                           LifecycleProcess process,
                                           JsonObject processContext,
                                           JsonObject taskContext) throws Exception;

  public abstract void validate(String taskId,
                                LifecycleProcess process,
                                JsonObject processContext,
                                JsonObject taskContext) throws Exception;

  public String getTaskDefinitionId() {
    return taskDefinitionId;
  }

  public void setTaskDefinitionId(String taskDefinitionId) {
    this.taskDefinitionId = taskDefinitionId;
  }

}