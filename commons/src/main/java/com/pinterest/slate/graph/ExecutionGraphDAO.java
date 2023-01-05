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

import org.hibernate.SessionFactory;

import com.pinterest.slate.process.Status;

import io.dropwizard.hibernate.AbstractDAO;
import io.dropwizard.hibernate.UnitOfWork;

@SuppressWarnings("unchecked")
public class ExecutionGraphDAO extends AbstractDAO<ExecutionGraph> implements AbstractExecutionDAO {

  protected AbstractStateStore store;

  public ExecutionGraphDAO(SessionFactory sessionFactory, AbstractStateStore store) {
    super(sessionFactory);
    this.store = store;
  }

  @UnitOfWork
  public void save(ExecutionGraph graph) throws IOException {
    if (graph.getStateStoragePath() == null) {
      graph.setStateStoragePath(store.getExecutionGraphStoragePath(graph.getExecutionId()));
    }
    store.saveExecutionGraph(graph);
    persist(graph);
  }

  public ExecutionGraph get(String executionId) throws IOException {
    return store.getExecutionGraph(executionId);
  }

  public List<String> listAllIncompleteExecutionGraphIds() throws IOException {
    return list(namedQuery("allIncompleteExecutionIds"));
  }

  public List<ExecutionGraph> listAllExecutionsGraphsBetween(Date from,
                                                       Date to,
                                                       List<Status> status,
                                                       int page) throws IOException {
    List<ExecutionGraph> executions = list(namedQuery("allExecutionsBetween").setParameter("startTime", from)
        .setParameter("endTime", to).setParameter("status", status).setMaxResults(10)
        .setFirstResult(page * 10 + 1));
    return executions;
  }

  public List<ExecutionGraph> listActiveExecutionsForRequester(String requester) throws IOException {
    List<ExecutionGraph> graphs = list(
        namedQuery("activeExecutionIdsForRequester").setParameter("requester", requester));
    return graphs;
  }

//  private List<ExecutionGraph> idToGraph(List<String> ids) {
//    List<ExecutionGraph> graphs = new ArrayList<>();
//    for (String id : ids) {
//      try {
//        graphs.add(store.get(id));
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//    }
//    return graphs;
//  }

  public List<ExecutionGraph> listRecentExecutionsForRequester(String requester) throws IOException {
    List<ExecutionGraph> graphs = list(
        namedQuery("recentCompletedExecutionIdsForRequester").setParameter("requester", requester));
    return graphs;
  }

}
