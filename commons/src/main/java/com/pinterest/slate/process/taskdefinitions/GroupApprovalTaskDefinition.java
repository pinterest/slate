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
package com.pinterest.slate.process.taskdefinitions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pinterest.slate.human.HumanTask;
import com.pinterest.slate.human.HumanTaskSystem;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskRuntime;

public class GroupApprovalTaskDefinition extends HumanTaskDefinition {

  public static final String ASSIGNEE_USER = "assigneeUser";
  public static final String TASK_DEFINITION_ID = "groupApprovalTask";
  public static final String SUMMARY = "summary";
  public static final String DESCRIPTION = "description";
  public static final String ASSIGNEE_GROUP = "approvalGroup";
  public static final String ADDITIONAL_DATA = "diff";
  private HumanTaskSystem hts;

  public GroupApprovalTaskDefinition() {
    super(TASK_DEFINITION_ID);
  }

  @Override
  public void init(TaskSystem engine) throws Exception {
    hts = engine.getHumanTaskSystem();
  }

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    if (!taskContext.has(ASSIGNEE_GROUP)) {
      throw new Exception("Missing approvalGroup: " + taskContext);
    }
    if (!taskContext.get(ASSIGNEE_GROUP).isJsonPrimitive()) {
      throw new Exception("Approval group must be a string");
    }
    if (!taskContext.has(DESCRIPTION)) {
      throw new Exception("Missing description");
    }
    if (!taskContext.has(SUMMARY)) {
      throw new Exception("Missing summary");
    }
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    JsonElement group = taskContext.get(ASSIGNEE_GROUP);
    if (group == null || group.isJsonObject()) {
      return StatusUpdate.create(Status.FAILED,
          "Invalid approval task as it's missing required approvalGroup");
    }
    String groupName = group.getAsString();
    String additionalData = "";
    if (taskContext.has(ADDITIONAL_DATA)) {
      additionalData = taskContext.get(ADDITIONAL_DATA).getAsString();
    }
    String userName = null;
    if (taskContext.has(ASSIGNEE_USER)) {
      JsonElement jsonElement = taskContext.get(ASSIGNEE_USER);
      if (jsonElement != null) {
        userName = jsonElement.getAsString();
      }
    }
    // make an entry in approval system
    hts.createApprovalTask(process.getProcessId(), taskId, process.getExecutionId(),
        taskContext.get(SUMMARY).getAsString(), taskContext.get(DESCRIPTION).getAsString(),
        groupName, userName, additionalData);
    return StatusUpdate.create(Status.RUNNING);
  }

  @Override
  public StatusUpdate checkStatus(TaskRuntime runtime,
                                  String taskId,
                                  LifecycleProcess process,
                                  JsonObject processContext,
                                  JsonObject taskContext) throws Exception {
    HumanTask task = hts.getTask(process.getProcessId(), taskId);
    if (task == null) {
      return StatusUpdate.create(Status.FAILED);
    }
    return StatusUpdate.create(task.getTaskStatus());
  }

}