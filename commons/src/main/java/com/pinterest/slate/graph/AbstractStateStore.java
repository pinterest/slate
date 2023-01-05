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
package com.pinterest.slate.graph;

import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration2.Configuration;

import com.pinterest.slate.resources.Resource;

public abstract class AbstractStateStore {

  public abstract void init(Configuration stateStoreConfig) throws IOException;

  public abstract void saveExecutionGraph(ExecutionGraph graph) throws IOException;

  public abstract ExecutionGraph getExecutionGraph(String excutionId) throws IOException;

  public abstract String getExecutionGraphStoragePath(String excutionId);
  
  public abstract void saveResourceUpdate(Resource resource) throws IOException;
  
  public abstract List<Long> listResourceUpdateTimestamps(String resourceId) throws IOException;
  
  public abstract Resource getResourceUpdate(String resourceId, long version) throws IOException;

}