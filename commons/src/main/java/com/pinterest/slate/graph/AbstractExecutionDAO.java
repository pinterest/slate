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
import java.util.Date;
import java.util.List;

import com.pinterest.slate.process.Status;

public interface AbstractExecutionDAO {

  public void save(ExecutionGraph graph) throws IOException;

  public List<String> listAllIncompleteExecutionGraphIds() throws IOException;

  public List<ExecutionGraph> listActiveExecutionsForRequester(String requester) throws IOException;

  public List<ExecutionGraph> listAllExecutionsGraphsBetween(Date from,
                                                       Date to,
                                                       List<Status> status,
                                                       int page) throws IOException;

  public ExecutionGraph get(String executionId) throws IOException;

  public List<ExecutionGraph> listRecentExecutionsForRequester(String requester) throws IOException;

}
