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

import com.pinterest.slate.process.CoreTaskRuntime;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.TaskRuntime;

import io.dropwizard.hibernate.UnitOfWork;

public class GraphExecutionRuntime {

  private AbstractResourceDB resourceDB;
  private AbstractExecutionDAO executionGraphDAO;
  private AbstractGraphExecutionQueue executionQueue;
  private TaskRuntime taskRuntime = new CoreTaskRuntime();
  private AbstractGraphAuditSink auditSink;

  public GraphExecutionRuntime(AbstractExecutionDAO executionGraphDAO) {
    this.executionGraphDAO = executionGraphDAO;
  }

  @UnitOfWork
  public void loadQueueIfSupported() throws IOException {
    List<String> graphs = executionGraphDAO.listAllIncompleteExecutionGraphIds();
    executionQueue.bootstrap(graphs);
  }

  @UnitOfWork
  public void update(ExecutionGraph executionGraph) throws IOException {
    executionGraphDAO.save(executionGraph);
  }
  
  protected void setTaskRuntime(TaskRuntime taskRuntime) {
    this.taskRuntime = taskRuntime;
  }

  public AbstractGraphAuditSink getAuditSink() {
    return auditSink;
  }

  public void setAuditSink(AbstractGraphAuditSink auditSink) {
    this.auditSink = auditSink;
  }

  public AbstractResourceDB getResourceDB() {
    return resourceDB;
  }

  public TaskRuntime getTaskRuntime() {
    return taskRuntime;
  }

  public void setResourceDB(AbstractResourceDB resourceDB) {
    this.resourceDB = resourceDB;
  }

  public AbstractGraphExecutionQueue getExecutionQueue() {
    return executionQueue;
  }

  public void setExecutionQueue(AbstractGraphExecutionQueue executionQueue) {
    this.executionQueue = executionQueue;
  }

  @UnitOfWork
  public ExecutionGraph get(String executionId) throws IOException {
    return executionGraphDAO.get(executionId);
  }

  @UnitOfWork
  public List<ExecutionGraph> listActiveExecutionsForRequester(String requester) throws IOException {
    return executionGraphDAO.listActiveExecutionsForRequester(requester);
  }

  @UnitOfWork
  public List<ExecutionGraph> listRecentExecutionsForRequester(String requester) throws IOException {
    return executionGraphDAO.listRecentExecutionsForRequester(requester);
  }

  @UnitOfWork
  public List<ExecutionGraph> listAllExecutionsBetween(Date from,
                                                       Date to,
                                                       List<Status> status,
                                                       int page) throws IOException {
    return executionGraphDAO.listAllExecutionsGraphsBetween(from, to, status, page);
  }
}
