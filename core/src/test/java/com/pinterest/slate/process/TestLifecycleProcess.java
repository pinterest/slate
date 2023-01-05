package com.pinterest.slate.process;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import com.pinterest.slate.process.taskdefinitions.FailProcessTask;
import com.pinterest.slate.process.taskdefinitions.SucceedProcessTask;

public class TestLifecycleProcess {

  private TaskRuntime runtime = new CoreTaskRuntime();

  public TestLifecycleProcess() {
    TaskFactory.INSTANCE.registerTask(new TestSuccessTaskDef());
    TaskFactory.INSTANCE.registerTask(new TestFailureTaskDef());
    TaskFactory.INSTANCE.registerTask(new TestCancelledTaskDef());
    TaskFactory.INSTANCE.registerTask(new SucceedProcessTask());
    TaskFactory.INSTANCE.registerTask(new FailProcessTask());
  }

  @Test
  public void testLinearExecutionThreeStageSuccess() throws Exception {
    LifecycleProcess process = new LifecycleProcess();
    process.addTaskNode(new Task("two", "s", ImmutableList.of(Task.SUCCEED_PROCESS_TASK),
        ImmutableList.of(Task.FAIL_PROCESS_TASK), ImmutableList.of(Task.FAIL_PROCESS_TASK)));
    process.addTaskNode(new Task("one", "s", ImmutableList.of("two"),
        ImmutableList.of(Task.FAIL_PROCESS_TASK), ImmutableList.of(Task.FAIL_PROCESS_TASK)));
    process.setStartTaskId("one");
    process.setProcessContext(new JsonObject());
    process.init();
    assertEquals(Status.NOT_STARTED, process.getEndStatus());
    assertEquals(1, process.getCurrenTaskSet().size());
    process.continueExecution(runtime);
    assertEquals(1, process.getCurrenTaskSet().size());
    assertEquals(Status.RUNNING, process.getEndStatus());
    process.continueExecution(runtime);
    assertEquals(1, process.getCurrenTaskSet().size());
    process.continueExecution(runtime);
    assertEquals(1, process.getCurrenTaskSet().size());
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    assertEquals(Status.SUCCEEDED, process.getEndStatus());
    assertEquals(0, process.getCurrenTaskSet().size());
  }

  @Test
  public void testLinearExecutionThreeStageFailure() throws Exception {
    LifecycleProcess process = new LifecycleProcess();
    process.addTaskNode(new Task("three", Task.SUCCEED_PROCESS_TASK, ImmutableList.of(),
        ImmutableList.of(), ImmutableList.of()));
    process.addTaskNode(new Task("two", "f", ImmutableList.of("three"),
        ImmutableList.of(Task.FAIL_PROCESS_TASK), ImmutableList.of(Task.FAIL_PROCESS_TASK)));
    process.addTaskNode(new Task("one", "s", ImmutableList.of("two"),
        ImmutableList.of(Task.FAIL_PROCESS_TASK), ImmutableList.of(Task.FAIL_PROCESS_TASK)));
    process.setStartTaskId("one");
    process.setProcessContext(new JsonObject());
    process.init();
    assertEquals(Status.NOT_STARTED, process.getEndStatus());
    assertEquals(1, process.getCurrenTaskSet().size());
    process.continueExecution(runtime);
    assertEquals(1, process.getCurrenTaskSet().size());
    assertEquals(Status.RUNNING, process.getEndStatus());
    process.continueExecution(runtime);
    assertEquals(1, process.getCurrenTaskSet().size());
    process.continueExecution(runtime);
    assertEquals(1, process.getCurrenTaskSet().size());
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    process.continueExecution(runtime);
    assertEquals(Status.FAILED, process.getEndStatus());
    assertEquals(0, process.getCurrenTaskSet().size());
  }

  public static class TestSuccessTaskDef extends TaskDefinition {

    public TestSuccessTaskDef() {
      super("s");
    }

    @Override
    public StatusUpdate startExecution(TaskRuntime runtime,
                                       String taskId,
                                       LifecycleProcess process,
                                       JsonObject processContext,
                                       JsonObject taskContext) throws Exception {
      return StatusUpdate.create(Status.RUNNING);
    }

    @Override
    public StatusUpdate checkStatus(TaskRuntime runtime,
                                    String taskId,
                                    LifecycleProcess process,
                                    JsonObject processContext,
                                    JsonObject taskContext) throws Exception {
      return StatusUpdate.create(Status.SUCCEEDED);
    }

    @Override
    public void validate(String taskInstanceId,
                         LifecycleProcess process,
                         JsonObject processContext,
                         JsonObject taskContext) throws Exception {
    }

  }

  public static class TestFailureTaskDef extends TaskDefinition {

    public TestFailureTaskDef() {
      super("f");
    }

    @Override
    public StatusUpdate startExecution(TaskRuntime runtime,
                                       String taskId,
                                       LifecycleProcess process,
                                       JsonObject processContext,
                                       JsonObject taskContext) throws Exception {
      return StatusUpdate.create(Status.RUNNING);
    }

    @Override
    public StatusUpdate checkStatus(TaskRuntime runtime,
                                    String taskId,
                                    LifecycleProcess process,
                                    JsonObject processContext,
                                    JsonObject taskContext) throws Exception {
      return StatusUpdate.create(Status.FAILED);
    }

    @Override
    public void validate(String taskInstanceId,
                         LifecycleProcess process,
                         JsonObject processContext,
                         JsonObject taskContext) throws Exception {
    }

  }

  public static class TestCancelledTaskDef extends TaskDefinition {

    public TestCancelledTaskDef() {
      super("c");
    }

    @Override
    public StatusUpdate startExecution(TaskRuntime runtime,
                                       String taskId,
                                       LifecycleProcess process,
                                       JsonObject processContext,
                                       JsonObject taskContext) throws Exception {
      return StatusUpdate.create(Status.RUNNING);
    }

    @Override
    public StatusUpdate checkStatus(TaskRuntime runtime,
                                    String taskId,
                                    LifecycleProcess process,
                                    JsonObject processContext,
                                    JsonObject taskContext) throws Exception {
      return StatusUpdate.create(Status.CANCELLED);
    }

    @Override
    public void validate(String taskInstanceId,
                         LifecycleProcess process,
                         JsonObject processContext,
                         JsonObject taskContext) throws Exception {
    }

  }

}
