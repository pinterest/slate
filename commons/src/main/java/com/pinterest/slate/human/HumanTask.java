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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.pinterest.slate.process.Status;

@Entity
@Table(name = "humantask")
@NamedQueries({
    @NamedQuery(name = "tasksPendingForGroups", query = "select t from HumanTask t where t.assigneeGroupName in :assigneeGroupNames and t.taskStatus in (com.pinterest.slate.process.Status.NOT_STARTED, com.pinterest.slate.process.Status.RUNNING)"),
    @NamedQuery(name = "tasksPendingForProcess", query = "select t from HumanTask t where t.processId=:processId and t.taskStatus in (com.pinterest.slate.process.Status.NOT_STARTED, com.pinterest.slate.process.Status.RUNNING)"),
    @NamedQuery(name = "unassignedApprovalTasksByGroup", query = "select t from HumanTask t where t.assigneeGroupName in :assigneeGroupNames and t.taskStatus=com.pinterest.slate.process.Status.NOT_STARTED and t.assigneeUser is null"),
    @NamedQuery(name = "tasksPendingForAssignee", query = "select t from HumanTask t where t.assigneeUser=:assigneeUser and t.taskStatus in (com.pinterest.slate.process.Status.NOT_STARTED, com.pinterest.slate.process.Status.RUNNING)"),
    @NamedQuery(name = "allTasks", query = "select t from HumanTask t"), })
@IdClass(HumanTaskId.class)
public final class HumanTask implements Serializable {

  public static enum TaskType {
                               APPROVAL,
                               NON_VERIFIABLE,
                               VERIFYABLE
  }

  private static final long serialVersionUID = 1L;

  @Column(name = "task_id", unique = true)
  @NotNull
  @Id
  private String taskId;
  @Column(name = "process_id")
  @NotNull
  @Id
  private String processId;
  @NotNull
  @Column(name = "execution_id")
  private String executionId;
  @Column(name = "assignee_group_name")
  @NotNull
  private String assigneeGroupName;
  @Column(name = "assignee_user")
  private String assigneeUser;
  @Column(name = "task_type")
  @NotNull
  @Enumerated(EnumType.STRING)
  private TaskType taskType;
  @Column(name = "task_status")
  @NotNull
  @Enumerated(EnumType.STRING)
  private Status taskStatus = Status.NOT_STARTED;
  @NotNull
  private String summary;
  @NotNull
  private String description;
  @Column(name = "additional_data")
  private String additionalData;
  private String comment;
  @Column(name = "create_time")
  private Date createTime;
  @Column(name = "update_time")
  private Date updateTime;

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Date updateTime) {
    this.updateTime = updateTime;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(String processId) {
    this.processId = processId;
  }

  public String getAssigneeGroupName() {
    return assigneeGroupName;
  }

  public void setAssigneeGroupName(String assigneeGroupName) {
    this.assigneeGroupName = assigneeGroupName;
  }

  public String getAssigneeUser() {
    return assigneeUser;
  }

  public void setAssigneeUser(String assigneeUser) {
    this.assigneeUser = assigneeUser;
  }

  public Status getTaskStatus() {
    return taskStatus;
  }

  public void setTaskStatus(Status taskStatus) {
    this.taskStatus = taskStatus;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getAdditionalData() {
    return additionalData;
  }

  public void setAdditionalData(String additionalData) {
    this.additionalData = additionalData;
  }

  @Override
  public String toString() {
    return "HumanTask [taskId=" + taskId + ", processId=" + processId + ", executionId="
        + executionId + ", assigneeGroupName=" + assigneeGroupName + ", assigneeUser="
        + assigneeUser + ", taskType=" + taskType + ", taskStatus=" + taskStatus + ", summary="
        + summary + ", description=" + description + ", additionalData=" + additionalData
        + ", comment=" + comment + ", createTime=" + createTime + ", updateTime=" + updateTime
        + "]";
  }

}