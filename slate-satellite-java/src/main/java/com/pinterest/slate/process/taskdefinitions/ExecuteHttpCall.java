package com.pinterest.slate.process.taskdefinitions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;
import com.pinterest.slate.utils.HttpUtils;

public abstract class ExecuteHttpCall extends TaskDefinition {

  protected static final String RESULT = "result";
  public static final String SUCCESS_CODES = "successCodes";
  private static final char[] PASSWORD = "pintastic".toCharArray();
  public static final String METHOD = "method";
  public static final String URL = "url";
  public static final String USE_SSL = "useSSL";
  private boolean dev;

  public ExecuteHttpCall(String taskDefinitionId) {
    super(taskDefinitionId);
  }

  @Override
  public void init(TaskSystem engine) throws Exception {
    super.init(engine);
    dev = engine.isDev();
  }

  public static KeyStore readStore() throws Exception {
    try (InputStream keyStoreStream = new FileInputStream(HttpUtils.KS_PATH)) {
      KeyStore keyStore = KeyStore.getInstance("JKS"); // or "PKCS12"
      keyStore.load(keyStoreStream, PASSWORD);
      return keyStore;
    }
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess process,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    if (dev) {
      return StatusUpdate.create(Status.SUCCEEDED);
    }
    String method = taskContext.get(METHOD).getAsString().toUpperCase();
    String url = taskContext.get(URL).getAsString();
    boolean useSSL = false;
    if (taskContext.has(USE_SSL)) {
      useSSL = taskContext.get(USE_SSL).getAsBoolean();
    }
    Set<Integer> successCodes = null;
    if (taskContext.has(SUCCESS_CODES)) {
      successCodes = new HashSet<Integer>();
      for (JsonElement jsonElement : taskContext.get(SUCCESS_CODES).getAsJsonArray()) {
        successCodes.add(jsonElement.getAsInt());
      }
    } else {
      successCodes = ImmutableSet.copyOf(new Integer[] { 200, 204, 201 });
    }
    HttpClientBuilder custom = HttpClients.custom();
    if (useSSL) {
      SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(readStore(), PASSWORD).build();
      custom = custom.setSSLContext(sslContext);
    }
    HttpClient httpClient = custom.build();
    HttpUriRequest request;
    switch (method) {
    case "GET":
      request = new HttpGet(url);
      break;
    case "POST":
      request = new HttpPost(url);
      break;
    case "PUT":
      request = new HttpPut(url);
      break;
    default:
      throw new Exception("Invalid request type");
    }
    HttpResponse response = httpClient.execute(request);
    if (successCodes.contains(response.getStatusLine().getStatusCode())) {
      HttpEntity entity = response.getEntity();
      taskContext.addProperty(RESULT, EntityUtils.toString(entity));
      return StatusUpdate.create(Status.SUCCEEDED);
    } else {
      return StatusUpdate.create(Status.FAILED, "Failed:" + response.getStatusLine().toString());
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

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    String method = taskContext.get(METHOD).getAsString().toUpperCase();
    if (!ImmutableSet.of("GET", "POST", "PUT").contains(method)) {
      throw new Exception("Unsupported method:" + method);
    }
    if (!taskContext.has(URL)) {
      throw new Exception("Missing url");
    }
    String url = taskContext.get(URL).getAsString();
    if (taskContext.has(USE_SSL)) {
      boolean useSSL = taskContext.get(USE_SSL).getAsBoolean();
      if (useSSL && !url.startsWith("https://")) {
        throw new Exception("URL to use SSL must be https://");
      }
    }
  }

}
