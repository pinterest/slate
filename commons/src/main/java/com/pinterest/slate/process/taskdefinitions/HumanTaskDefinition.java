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

import com.google.gson.JsonObject;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.TaskDefinition;

public abstract class HumanTaskDefinition extends TaskDefinition {

  public static final String ASSIGNEE_USER = GroupApprovalTaskDefinition.ASSIGNEE_USER;
  public static final String ASSIGNEE_GROUP = GroupApprovalTaskDefinition.ASSIGNEE_GROUP;

  public HumanTaskDefinition(String taskDefinitionId) {
    super(taskDefinitionId);
  }

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    validateTasks(taskContext);
  }

  public static void validateTasks(JsonObject taskContext) throws Exception {
    if (!taskContext.has(ASSIGNEE_USER) || taskContext.get(ASSIGNEE_USER) == null) {
      throw new Exception("Missing assigneeUser");
    }
    if (!taskContext.has(ASSIGNEE_GROUP) || taskContext.get(ASSIGNEE_GROUP) == null) {
      throw new Exception("Missing assigneeGroup");
    }
  }

}