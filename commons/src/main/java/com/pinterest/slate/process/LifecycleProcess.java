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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.pinterest.slate.resources.PlanException;

public final class LifecycleProcess {

  public static enum ProcessType {
                                  CREATE,
                                  UPDATE,
                                  DELETE
  }

  private String executionId;
  private ProcessType processType;
  private String processId;
  private int maxConcurrentTasks;
  private JsonObject processContext;
  private Set<String> currenTaskSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private String startTaskId;
  private Map<String, Task> allTasks = new ConcurrentHashMap<>();
  private Status endStatus = Status.NOT_STARTED;
  private long startTimeMs;
  private long endTimeMs;

  public LifecycleProcess() {
    allTasks.put(Task.SUCCEED_PROCESS_TASK, new Task(Task.SUCCEED_PROCESS_TASK,
        Task.SUCCEED_PROCESS_TASK, ImmutableList.of(), ImmutableList.of(), ImmutableList.of()));
    allTasks.put(Task.FAIL_PROCESS_TASK, new Task(Task.FAIL_PROCESS_TASK, Task.FAIL_PROCESS_TASK,
        ImmutableList.of(), ImmutableList.of(), ImmutableList.of()));
  }

  public LifecycleProcess(String processId) {
    this();
    this.processId = processId;
  }

  public void init() throws Exception {
    if (startTaskId == null) {
      throw new Exception("Process hasn't been initialized correctly");
    }
    currenTaskSet.add(startTaskId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(processContext, allTasks);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof LifecycleProcess)) {
      return false;
    }
    LifecycleProcess p = (LifecycleProcess) obj;
    if (!p.getProcessContext().equals(processContext) && !p.getAllTasks().equals((allTasks))) {
      return false;
    }
    return true;
  }

  public JsonObject getProcessContext() {
    return processContext;
  }

  public void setProcessContext(JsonObject processContext) {
    this.processContext = processContext;
  }

  public void addTaskNode(Task task) {
    if (allTasks.containsKey(task.getInstanceId())) {
      throw new IllegalArgumentException("Duplicate task exception");
    }
    allTasks.put(task.getInstanceId(), task);
  }

  /**
   * Dry run and validate whether or not the dependencies and inputs for tasks
   * wired in this process has been satisfied.
   * 
   * If they are not satisfied this method will throw exception and we can catch
   * errors during planning rather than in execution phase of a graph.
   * 
   * @throws Exception
   */
  public void validateTasks() throws Exception {
    if (processContext == null) {
      throw new PlanException(
          "Process context cannot be null, there is an error in this process design. Please talk to the ResourceDefinition owner");
    }
    if (startTaskId == null) {
      throw new PlanException("Missing start task id, please notify the ResourceDefinition owner");
    }
    if (!allTasks.containsKey(startTaskId)) {
      throw new PlanException("Invalid start task id:" + startTaskId);
    }
    for (Task task : allTasks.values()) {
      for (Entry<Status, List<String>> entry : task.getNextPointers().entrySet()) {
        for (String taskid : entry.getValue()) {
          if (!allTasks.containsKey(taskid)) {
            throw new IllegalArgumentException(
                "Missing task id:" + taskid + " which this task points to");
          }
        }
      }
      String taskDefinitionId = task.getTaskDefinitionId();
      if (Arrays.asList(Task.SUCCEED_PROCESS_TASK, Task.FAIL_PROCESS_TASK)
          .contains(taskDefinitionId)) {
        continue;
      }
      try {
        TaskDefinition td = TaskFactory.INSTANCE.getTask(taskDefinitionId);
        if (td == null) {
          throw new PlanException("Task definition(" + taskDefinitionId + ") not found");
        }
        td.validate(task.getInstanceId(), this, processContext,
            processContext.has(task.getInstanceId())
                ? processContext.get(task.getInstanceId()).getAsJsonObject()
                : null);
      } catch (Exception e) {
        e.printStackTrace();
        throw new PlanException("Plan failure, task validation for:" + task.getInstanceId()
            + " of type:" + taskDefinitionId
            + " failed, please contact the author of this resource definition", e);
      }
    }
  }

  public void continueExecution(TaskRuntime taskRuntime) throws Exception {
    if (endStatus == Status.NOT_STARTED) {
      endStatus = Status.RUNNING;
    }
    List<String> enqueueTasks = new ArrayList<>();
    for (Iterator<String> iterator = currenTaskSet.iterator(); iterator.hasNext();) {
      String taskNodeId = iterator.next();
      Task taskNode = allTasks.get(taskNodeId);
      String taskDefinitionId = taskNode.getTaskDefinitionId();
      switch (taskNode.getStatus()) {
      case CANCELLED:
        taskNode.setEndTimeMs(System.currentTimeMillis());
        taskNode.appendStdErr("Task has been cancelled");
        enqueueTasks.addAll(enqueueNextTaskNodes(taskNode, iterator));
        break;
      case FAILED:
        taskNode.setEndTimeMs(System.currentTimeMillis());
        taskNode.appendStdErr("Task failed");
        enqueueTasks.addAll(enqueueNextTaskNodes(taskNode, iterator));
        break;
      case SUCCEEDED:
        taskNode.setEndTimeMs(System.currentTimeMillis());
        taskNode.appendStdOut("Task completed");
        enqueueTasks.addAll(enqueueNextTaskNodes(taskNode, iterator));
        break;
      case NOT_STARTED:
        // submit task for execution
        StatusUpdate update = taskRuntime.startExecution(taskDefinitionId, taskNode.getInstanceId(),
            this);
        taskNode.setStatus(Status.RUNNING);
        taskNode.setStartTimeMs(System.currentTimeMillis());
        if (update != null) {
          // if trigger itself completes the task
          taskNode.setStatus(update.getStatus());
          if (update.getStdErr() != null) {
            taskNode.appendStdErr(update.getStdErr());
          }
          if (update.getStdOut() != null) {
            taskNode.appendStdOut(update.getStdOut());
          }

          if (!Status.END_STATUS.contains(update.getStatus())) {
            taskNode.appendStdOut("Task started");
          }
        }
        break;
      case RUNNING:
        // if the task is running fetch the latest status of the task to see if the
        // status has changed
        StatusUpdate statusUpdate = taskRuntime.checkStatus(taskDefinitionId,
            taskNode.getInstanceId(), this);
        if (statusUpdate != null) {
          Status newStatus = statusUpdate.getStatus();
          if (newStatus != taskNode.getStatus()) {
            // status has changed; update
            taskNode.setStatus(newStatus);
          }
          if (statusUpdate.getStdErr() != null) {
            taskNode.appendStdErr(statusUpdate.getStdErr());
          }
          if (statusUpdate.getStdOut() != null) {
            taskNode.appendStdOut(statusUpdate.getStdOut());
          }
          if (statusUpdate.getProcessContextUpdate() != null) {
            statusUpdate.getProcessContextUpdate().apply(processContext);
          }
          // // if there are no new items in the json
          // if (statusUpdate.getContextUpdates() != null
          // & !statusUpdate.getContextUpdates().entrySet().isEmpty()) {
          // deepMerge(statusUpdate.getContextUpdates(), planContext);
          // }
        }
        break;
      default:
        break;
      }
    }
    currenTaskSet.addAll(enqueueTasks);
  }

  private List<String> enqueueNextTaskNodes(Task taskNode,
                                            Iterator<String> iterator) throws InstantiationException,
                                                                       IllegalAccessException {
    List<String> nextTasks = taskNode.getNextPointers().get(taskNode.getStatus());
    if (nextTasks == null) {
      throw new IllegalAccessException("Unhandled task status for:" + taskNode.getTaskDefinitionId()
          + " status:" + taskNode.getStatus());
    }

    iterator.remove();
    return nextTasks;
  }

  public Status getEndStatus() {
    return endStatus;
  }

  public void setEndStatus(Status endStatus) {
    this.endStatus = endStatus;
  }

  public int getMaxConcurrentTasks() {
    return maxConcurrentTasks;
  }

  public Task getFlow() {
    return allTasks.get(startTaskId);
  }

  public Map<String, Task> getAllTasks() {
    return allTasks;
  }

  public Set<String> getCurrenTaskSet() {
    return currenTaskSet;
  }

  public void setCurrenTaskSet(Set<String> currenTaskSet) {
    this.currenTaskSet = currenTaskSet;
  }

  public void setAllTasks(Map<String, Task> allTasks) {
    this.allTasks = allTasks;
  }

  public String getStartTaskId() {
    return startTaskId;
  }

  public void setStartTaskId(String startTaskId) {
    this.startTaskId = startTaskId;
  }

  public void setMaxConcurrentTasks(int maxConcurrentTasks) {
    this.maxConcurrentTasks = maxConcurrentTasks;
  }

  public void setAllNodes(Map<String, Task> allNodes) {
    this.allTasks = allNodes;
  }

  public String getProcessId() {
    return processId;
  }

  public void setProcessId(String processId) {
    this.processId = processId;
  }

  public ProcessType getProcessType() {
    return processType;
  }

  public void setProcessType(ProcessType procesType) {
    this.processType = procesType;
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

  /**
   * A process is complete if there are currently no pending tasks and if all
   * tasks have either completed (SUCCESS, FAILED or CANCELLED)
   * 
   * @return
   */
  public boolean isComplete() {
    // TODO check if task queue should be empty
    return Status.isComplete(endStatus);
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @Override
  public String toString() {
    return "LifecycleProcess [" + hashCode() + " executionId=" + executionId + ", processType="
        + processType + ", processId=" + processId + ", maxConcurrentTasks=" + maxConcurrentTasks
        + ", planContext=" + processContext + "(" + processContext.hashCode() + "), currenTaskSet="
        + currenTaskSet + ", startTaskId=" + startTaskId + ", allTasks=" + allTasks + "("
        + allTasks.hashCode() + "), endStatus=" + endStatus + "]";
  }
}
