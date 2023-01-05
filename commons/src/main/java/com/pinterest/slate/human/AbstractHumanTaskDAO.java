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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.pinterest.slate.graph.GraphExecutor;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.taskdefinitions.SlackTask;
import com.pinterest.slate.utils.LDAPUtils;

public interface AbstractHumanTaskDAO {

  public static final Logger logger = Logger.getLogger(AbstractHumanTaskDAO.class.getName());

  List<HumanTask> listPendingTasksForAssigneeGroup(Set<String> groups);

  List<HumanTask> listPendingTasksForAssigneeUser(String user);

  HumanTask get(HumanTaskId id);

  HumanTask persist(HumanTask task);

  default HumanTask create(HumanTask task) {
    validate(task);
    logger.info("New task:" + task.toString());
    if (task.getAssigneeUser() == null) {
      // try to find an ldap group user and assign task to them
      try {
        List<String> ldapMembers = LDAPUtils.getLDAPMembers(task.getAssigneeGroupName());
        String user = ldapMembers.get(ThreadLocalRandom.current().nextInt(ldapMembers.size()));
        task.setAssigneeUser(user);
      } catch (Exception e) {
      }
    }
    if (task.getAssigneeUser() != null) {
      try {
        SlackTask.sendSlackMessage("New manual task has been assigned to you:" + task.getSummary(),
            task.getSummary(), GraphExecutor.BASE_URL+"/tasks", task.getAssigneeUser());
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to send slack notification for task:" + task.toString(),
            e);
      }
    }
    task.setCreateTime(new Date());
    task.setUpdateTime(new Date());
    return persist(task);
  }

  default void validate(HumanTask task) {
    if (task.getProcessId() == null || task.getTaskId() == null) {
      throw new IllegalArgumentException("Proces and Task ids can't be null");
    }
  }

  default HumanTask getTask(String processId, String taskId) {
    return get(new HumanTaskId(processId, taskId));
  }

  default void updateStatus(String processId, String taskId, Status status, String comment) {
    HumanTask task = get(new HumanTaskId(processId, taskId));
    task.setComment(comment);
    task.setTaskStatus(status);
    task.setUpdateTime(new Date());
    persist(task);
  }

  default void updateAssignee(String processId, String taskId, String assigneeUser) {
    HumanTask task = get(new HumanTaskId(processId, taskId));
    task.setAssigneeUser(assigneeUser);
    task.setUpdateTime(new Date());
    persist(task);
  }

}
