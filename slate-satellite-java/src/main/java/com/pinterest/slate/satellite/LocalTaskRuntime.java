package com.pinterest.slate.satellite;

import org.apache.commons.configuration2.Configuration;

import com.google.gson.JsonObject;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;

public class LocalTaskRuntime implements TaskRuntime {

  @Override
  public void configure(Configuration configuration) throws Exception {
    // TODO Auto-generated method stub
  }

  @Override
  public StatusUpdate startExecution(String taskDefinitionId,
                                     String taskInstanceId,
                                     LifecycleProcess workflow) {
    TaskDefinition task = LocalTaskFactory.INSTANCE.getTask(taskDefinitionId);
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
                                  LifecycleProcess process) {
    TaskDefinition runningTask = LocalTaskFactory.INSTANCE.getTask(taskTemplateId);
    try {
      JsonObject processContext = process.getProcessContext();
      return runningTask.checkStatus(this, taskInstanceId, process, processContext,
          processContext.has(taskInstanceId) ? processContext.get(taskInstanceId).getAsJsonObject()
              : null);
    } catch (Exception e) {
      e.printStackTrace();
      return StatusUpdate.create(Status.FAILED, "Status check failed", e);
    }
  }

}
