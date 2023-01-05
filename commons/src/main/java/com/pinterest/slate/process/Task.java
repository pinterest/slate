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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

public class Task {

  private String instanceId;
  private String taskDefinitionId;
  private long startTimeMs;
  private long endTimeMs;
  // points to task template Ids
  private Map<Status, List<String>> nextPointers;
  private Status status = Status.NOT_STARTED;
  private List<ProcessLogMessage> stdOut = new ArrayList<>();
  private List<ProcessLogMessage> stdErr = new ArrayList<>();
  public static final String FAIL_PROCESS_TASK = "failProcess";
  public static final String SUCCEED_PROCESS_TASK = "succeedProcess";

  public Task() {
  }

  public Task(String instanceId,
              String taskDefinitionId,
              List<String> nextPointersForSucceeded,
              List<String> nextPointersForFailed,
              List<String> nextPointersForCancelled) {
    this.instanceId = instanceId;
    this.taskDefinitionId = taskDefinitionId;
    this.nextPointers = ImmutableMap.of(Status.SUCCEEDED, nextPointersForSucceeded,
        Status.CANCELLED, nextPointersForCancelled, Status.FAILED, nextPointersForFailed);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(status, nextPointers, taskDefinitionId);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Task)) {
      return false;
    }
    Task t = (Task) obj;
    if (!t.getTaskDefinitionId().equals(taskDefinitionId)
        || !t.getNextPointers().equals(nextPointers) || !t.getStatus().equals(status)) {
      return false;
    }
    return true;
  }

  public long getStartTimeMs() {
    return startTimeMs;
  }

  public void setStartTimeMs(long startTimeMs) {
    this.startTimeMs = startTimeMs;
  }

  public long getEndTimeMs() {
    return endTimeMs;
  }

  public void setEndTimeMs(long endTimeMs) {
    this.endTimeMs = endTimeMs;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public void setTaskDefinitionId(String taskDefinitionId) {
    this.taskDefinitionId = taskDefinitionId;
  }

  public void setStdOut(List<ProcessLogMessage> stdOut) {
    this.stdOut = stdOut;
  }

  public void setStdErr(List<ProcessLogMessage> stdErr) {
    this.stdErr = stdErr;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getTaskDefinitionId() {
    return taskDefinitionId;
  }

  public Map<Status, List<String>> getNextPointers() {
    return nextPointers;
  }

  public void setNextPointers(Map<Status, List<String>> nextPointers) {
    this.nextPointers = nextPointers;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void appendStdOut(String msg) {
    stdOut.add(new ProcessLogMessage(System.currentTimeMillis(), msg));
  }

  public void appendStdErr(String msg) {
    stdErr.add(new ProcessLogMessage(System.currentTimeMillis(), msg));
  }

  public void appendStdErr(String msg, Exception e) {
    stdErr
        .add(new ProcessLogMessage(System.currentTimeMillis(), msg + ". Cause:" + e.getMessage()));
  }

  public List<ProcessLogMessage> getStdErr() {
    return stdErr;
  }

  public List<ProcessLogMessage> getStdOut() {
    return stdOut;
  }

  @Override
  public String toString() {
    return "Task [instanceId=" + instanceId + ", taskDefinitionId="
        + taskDefinitionId + ", startTimeMs=" + startTimeMs + ", endTimeMs=" + endTimeMs
        + ", nextPointers=" + nextPointers + ", status=" + status + ", stdOut=" + stdOut
        + ", stdErr=" + stdErr + "]";
  }
}