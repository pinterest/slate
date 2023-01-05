package com.pinterest.slate.graph.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.pinterest.slate.graph.AbstractStateStore;
import com.pinterest.slate.graph.ExecutionGraph;
import com.pinterest.slate.resources.Resource;

import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3StateStore extends AbstractStateStore {

  private static final Gson GSON = new Gson();
  private S3Client s3;
  private String bucket;
  private String executionGraphStoragePrefix;
  private String resourceUpdateStoragePrefix;

  @Override
  public void init(Configuration stateStoreConfig) throws IOException {
    s3 = S3Client.builder().region(Region.US_EAST_1)
        .credentialsProvider(InstanceProfileCredentialsProvider.create()).build();
    bucket = stateStoreConfig.getString("bucket");
    executionGraphStoragePrefix = stateStoreConfig.getString("executionGraphStoragePrefix");
    resourceUpdateStoragePrefix = stateStoreConfig.getString("resourceUpdateStoragePrefix");
  }

  @Override
  public void saveExecutionGraph(ExecutionGraph graph) throws IOException {
    String json = GSON.toJson(graph);
    PutObjectRequest req = PutObjectRequest.builder().bucket(bucket)
        .key(executionGraphStoragePrefix + "/" + graph.getExecutionId()).build();
    s3.putObject(req, RequestBody.fromString(json));
  }

  @Override
  public ExecutionGraph getExecutionGraph(String excutionId) throws IOException {
    ResponseInputStream<GetObjectResponse> object = s3.getObject(GetObjectRequest.builder()
        .bucket(bucket).key(executionGraphStoragePrefix + "/" + excutionId).build());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(object, out);
    return GSON.fromJson(new String(out.toByteArray()), ExecutionGraph.class);
  }

  @Override
  public String getExecutionGraphStoragePath(String executionId) {
    return "s3://" + bucket + "/" + executionGraphStoragePrefix + "/" + executionId;
  }

  @Override
  public void saveResourceUpdate(Resource resource) throws IOException {
    String json = GSON.toJson(resource);
    PutObjectRequest req = PutObjectRequest.builder().bucket(bucket)
        .key(getResourceVersionPath(resource.getId(), resource.getLastUpdateTimestamp())).build();
    s3.putObject(req, RequestBody.fromString(json));
  }

  @Override
  public List<Long> listResourceUpdateTimestamps(String resourceId) throws IOException {
    String prefix = resourceUpdateStoragePrefix + "/" + resourceId + "/";
    ListObjectsResponse listObjects = s3
        .listObjects(ListObjectsRequest.builder().bucket(bucket).prefix(prefix).build());
    return listObjects.contents().stream().map(o -> o.key().replace(prefix, ""))
        .map(v -> Long.parseLong(v)).collect(Collectors.toList());
  }

  @Override
  public Resource getResourceUpdate(String resourceId,
                                     long lastUpdateTimestamp) throws IOException {
    try {
      ResponseInputStream<GetObjectResponse> object = s3.getObject(GetObjectRequest.builder()
          .bucket(bucket).key(getResourceVersionPath(resourceId, lastUpdateTimestamp)).build());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      IOUtils.copy(object, out);
      return GSON.fromJson(new String(out.toByteArray()), Resource.class);
    } catch (NoSuchKeyException e) {
      throw new NotFoundException();
    }
  }

  private String getResourceVersionPath(String resourceId, long lastUpdateTimestamp) {
    return resourceUpdateStoragePrefix + "/" + resourceId + "/" + lastUpdateTimestamp;
  }

}
