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
package com.pinterest.slate.graph.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.Configuration;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.pinterest.slate.graph.AbstractStateStore;
import com.pinterest.slate.graph.ExecutionGraph;
import com.pinterest.slate.resources.Resource;

public class LocalStateStore extends AbstractStateStore {

  private static final Gson GSON = new Gson();
  private String baseGraphStoragePath = "target/tmp/graphs";
  private String baseResourceUpdateStoragePath = "target/tmp/resources";

  @Override
  public void init(Configuration stateStoreConfig) throws IOException {
    baseGraphStoragePath = stateStoreConfig.getString("basegraphstoragepath");
    new File(baseGraphStoragePath).mkdirs();
    baseResourceUpdateStoragePath = stateStoreConfig.getString("baseresourceupdatestoragepath");
    new File(baseResourceUpdateStoragePath).mkdirs();
  }

  @Override
  public void saveExecutionGraph(ExecutionGraph graph) throws IOException {
    String json = GSON.toJson(graph);
    Files.write(new File(getExecutionGraphStoragePath(graph.getExecutionId())).toPath(),
        json.getBytes());
  }

  @Override
  public ExecutionGraph getExecutionGraph(String excutionId) throws IOException {
    File file = new File(getExecutionGraphStoragePath(excutionId));
    if (!file.exists()) {
      return null;
    }
    String json = new String(
        Files.readAllBytes(file.toPath()));
    return GSON.fromJson(json, ExecutionGraph.class);
  }

  @Override
  public String getExecutionGraphStoragePath(String excutionId) {
    return baseGraphStoragePath + "/" + excutionId;
  }

  public Path getResourceVersionPath(String resourceId, long version) {
    File file = new File(baseResourceUpdateStoragePath + "/" + resourceId);
    if (!file.exists()) {
      if (!file.mkdir()) {
        throw new RuntimeException("Couldn't create directory " + file.getAbsolutePath());
      }
    }
    return new File(baseResourceUpdateStoragePath + "/" + resourceId + "/" + version).toPath();
  }

  public File getResourcePath(String resourceId) {
    return new File(baseResourceUpdateStoragePath + "/" + resourceId);
  }

  @Override
  public void saveResourceUpdate(Resource resource) throws IOException {
    long version = resource.getLastUpdateTimestamp();
    Files.writeString(getResourceVersionPath(resource.getId(), version), GSON.toJson(resource),
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  @Override
  public List<Long> listResourceUpdateTimestamps(String resourceId) throws IOException {
    File resourcePath = getResourcePath(resourceId);
    if (!resourcePath.exists()) {
      return ImmutableList.of();
    }
    File[] listFiles = resourcePath.listFiles();
    if (listFiles == null) {
      return ImmutableList.of();
    }
    return Arrays.asList(listFiles).stream().map(v -> Long.parseLong(v.getName()))
        .collect(Collectors.toList());
  }

  @Override
  public Resource getResourceUpdate(String resourceId,
                                    long lastUpdateTimestamp) throws IOException {
    return GSON.fromJson(Files.readString(getResourceVersionPath(resourceId, lastUpdateTimestamp)),
        Resource.class);
  }

}
