package com.pinterest.slate.process.taskdefinitions;

import com.google.gson.JsonObject;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;

import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

public abstract class DeleteS3ObjectTask extends TaskDefinition {

  public static final String KEY = "key";
  public static final String BUCKET = "bucket";
  public static final String REGION = "region";
  private boolean isDev;

  public DeleteS3ObjectTask(String taskDefinitionId) {
    super(taskDefinitionId);
  }

  @Override
  public void init(TaskSystem engine) throws Exception {
    isDev = engine.isDev();
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    S3Client s3 = S3Client.builder()
        .region(Region.of(taskContext.get(REGION).getAsString().toLowerCase()))
        .credentialsProvider(InstanceProfileCredentialsProvider.create()).build();
    int retries = 5;
    boolean success = false;
    Exception e1 = null;
    if (isDev) {
      success = true;
    }
    while (!success && retries > 0) {
      try {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(taskContext.get(BUCKET).getAsString())
            .key(taskContext.get(KEY).getAsString()).build());
        success = true;
      } catch (Exception e) {
        e1 = e;
        retries--;
      }
    }
    if (!success) {
      return StatusUpdate.create(Status.FAILED, "Failed to deletee key from S3 after 5 attempts",
          e1);
    }
    return StatusUpdate.create(Status.SUCCEEDED);
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
    PutS3ObjectTask.validateBase(taskContext);
  }

}
