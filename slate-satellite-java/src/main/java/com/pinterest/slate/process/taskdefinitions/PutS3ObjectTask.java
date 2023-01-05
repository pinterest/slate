package com.pinterest.slate.process.taskdefinitions;

import java.io.File;
import java.nio.file.Files;

import com.google.gson.JsonObject;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;

import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public abstract class PutS3ObjectTask extends TaskDefinition {

  public static final String DATA = "data";
  public static final String FILE = "file";
  public static final String KEY = "key";
  public static final String BUCKET = "bucket";
  public static final String REGION = "region";
  private File tmpDir;
  protected boolean isDev;

  public PutS3ObjectTask(String taskDefinitionId) {
    super(taskDefinitionId);
  }

  @Override
  public void init(TaskSystem engine) throws Exception {
    tmpDir = new File(new File(engine.getTmpDirectory()), "puts3object");
    tmpDir.mkdirs();
    isDev = engine.isDev();
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    File val = null;
    if (taskContext.has(FILE)) {
      val = new File(taskContext.get(FILE).getAsString());
    } else {
      tmpDir.mkdirs();
      val = new File(tmpDir, process.getProcessId() + "_" + taskId);
      Files.write(val.toPath(), taskContext.get(DATA).getAsString().getBytes());
    }
    int retries = 5;
    boolean success = false;
    Exception e1 = null;
    if (isDev) {
      System.out.println("Written file:" + val.getAbsolutePath() + " content:"
          + new String(Files.readAllBytes(val.toPath())));
      success = true;
    } else {
      S3Client s3 = S3Client.builder()
          .region(Region.of(taskContext.get(REGION).getAsString().toLowerCase()))
          .credentialsProvider(InstanceProfileCredentialsProvider.create()).build();
      while (!success && retries > 0) {
        try {
          s3.putObject(PutObjectRequest.builder().bucket(taskContext.get(BUCKET).getAsString())
              .key(taskContext.get(KEY).getAsString()).build(), RequestBody.fromFile(val));
          success = true;
        } catch (Exception e) {
          e1 = e;
          retries--;
        }
      }
    }
    if (!success) {
      return StatusUpdate.create(Status.FAILED, "Failed to upload file to S3 after 5 attempts", e1);
    }
    return StatusUpdate.create(Status.SUCCEEDED);
  }

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    validateBase(taskContext);
    if (!taskContext.has(FILE) && !taskContext.has(DATA)) {
      throw new Exception("Missing file & data (either specify file path or data)");
    }
  }

  public static void validateBase(JsonObject taskContext) throws Exception {
    if (taskContext == null) {
      throw new Exception("Task context is null");
    }
    if (!taskContext.has(REGION)) {
      throw new Exception("Missing region");
    }
    Region.of(taskContext.get(REGION).getAsString().toLowerCase());
    if (!taskContext.has(BUCKET)) {
      throw new Exception("Missing bucket");
    }
    if (!taskContext.has(KEY)) {
      throw new Exception("Missing key");
    }
  }

  @Override
  public StatusUpdate checkStatus(TaskRuntime runtime,
                                  String taskId,
                                  LifecycleProcess process,
                                  JsonObject processContext,
                                  JsonObject taskContext) throws Exception {
    return null;
  }

}
