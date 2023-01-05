package com.pinterest.slate.process.taskdefinitions;

import com.google.gson.JsonObject;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;

public abstract class ExecuteBashCommand extends TaskDefinition {

  public static final String TASK_DEFINITION_ID = "executeBashCommand";
  public static final String COMMAND = "command";

  public ExecuteBashCommand() {
    super(TASK_DEFINITION_ID);
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    ProcessBuilder processBuilder = new ProcessBuilder(
        taskContext.get(COMMAND).getAsString().split(" "));
    processBuilder.redirectErrorStream(true);
    processBuilder.redirectOutput();
    return null;
  }

  @Override
  public StatusUpdate checkStatus(TaskRuntime runtime,
                                  String taskId,
                                  LifecycleProcess process,
                                  JsonObject processContext,
                                  JsonObject taskContext) throws Exception {
    return null;
  }

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
  }

}
