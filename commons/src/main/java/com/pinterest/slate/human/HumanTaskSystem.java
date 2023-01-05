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
package com.pinterest.slate.human;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pinterest.slate.human.HumanTask.TaskType;
import com.pinterest.slate.process.Status;

import io.dropwizard.hibernate.UnitOfWork;

public class HumanTaskSystem {

  private static final Logger logger = Logger.getLogger(HumanTaskSystem.class.getCanonicalName());
  private AbstractHumanTaskDAO dao;

  public HumanTaskSystem(AbstractHumanTaskDAO dao) {
    this.dao = dao;
  }

  @UnitOfWork
  public HumanTask create(HumanTask task) throws IOException {
    try {
      return dao.create(task);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to persist " + task, e);
      throw e;
    }
  }

  @UnitOfWork
  public HumanTask create(String processId,
                          String taskId,
                          String executionId,
                          String summary,
                          String description,
                          String assigneeGroupName,
                          String assigneeUser,
                          String additionalData,
                          TaskType type) throws IOException {
    HumanTask task = new HumanTask();
    task.setExecutionId(executionId);
    task.setAssigneeGroupName(assigneeGroupName);
    task.setSummary(summary);
    task.setDescription(description);
    task.setAdditionalData(additionalData);
    task.setAssigneeUser(assigneeUser);
    task.setTaskId(taskId);
    task.setTaskType(type);
    task.setProcessId(processId);
    task.setTaskStatus(Status.RUNNING);
    try {
      task = create(task);
      return task;
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to persist " + task, e);
      throw e;
    }
  }

  @UnitOfWork
  public HumanTask createApprovalTask(String processId,
                                      String taskId,
                                      String executionId,
                                      String summary,
                                      String description,
                                      String assigneeGroupName,
                                      String assigneeUser,
                                      String additionalData) throws IOException {
    
    return create(processId, taskId, executionId, summary, description,
        assigneeGroupName, assigneeUser, additionalData, TaskType.APPROVAL);
  }

  @UnitOfWork
  public HumanTask getTask(String processId, String taskId) throws IOException {
    return dao.getTask(processId, taskId);
  }

  @UnitOfWork
  public void updateStatus(String processId,
                           String taskId,
                           Status status,
                           String comment) throws IOException {
    dao.updateStatus(processId, taskId, status, comment);
  }

  @UnitOfWork
  public void updateAssignee(String processId, String taskId, String assigneeUser) throws IOException {
    dao.updateAssignee(processId, taskId, assigneeUser);
  }

  @UnitOfWork
  public List<HumanTask> listPendingTasksForAssigneeUser(String user) {
    return dao.listPendingTasksForAssigneeUser(user);
  }

  @UnitOfWork
  public List<HumanTask> listPendingTasksForAssigneeGroup(Set<String> groups) {
    return dao.listPendingTasksForAssigneeGroup(groups);
  }
}
