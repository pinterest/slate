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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceFactory;

@Entity
@Table(name = "executiongraph")
@NamedQueries({
    @NamedQuery(name = "allIncompleteExecutionIds", query = "select e.id from ExecutionGraph e where e.status not in ('FAILED', 'SUCCEEDED', 'CANCELLED')"),
    @NamedQuery(name = "allExecutionIdsForRequester", query = "select e from ExecutionGraph e where e.requester=:requester order by e.startTime desc"),
    @NamedQuery(name = "allExecutionsBetween", query = "select e from ExecutionGraph e where e.startTime>=:startTime and e.startTime<=:endTime and e.status in (:status) order by e.startTime desc"),
    @NamedQuery(name = "activeExecutionIdsForRequester", query = "select e from ExecutionGraph e where e.requester=:requester and e.status in ('RUNNING', 'NOT_STARTED') order by e.startTime desc"),
    @NamedQuery(name = "recentCompletedExecutionIdsForRequester", query = "select e from ExecutionGraph e where e.requester=:requester and e.status in ('FAILED', 'SUCCEEDED', 'CANCELLED') and e.endTime>=(current_date - 15)  order by e.startTime desc") })
public class ExecutionGraph implements Serializable {

  private static final long serialVersionUID = 1L;
  @Column(name = "execution_id")
  @Id
  private String executionId;
  private String requester;
  @Column(name = "start_time")
  private Date startTime;
  @Column(name = "end_time")
  private Date endTime;
  @Column(name = "state_path")
  private String stateStoragePath;
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private Status status = Status.NOT_STARTED;
  @Transient
  private Set<String> currentPlanSet = new ConcurrentSkipListSet<>();
  @Transient
  private Map<String, PlanVertex> executionPlan;
  @Transient
  private SortedMap<String, EdgeMutation> allEdgeMutations;

  public void continueExecution(GraphExecutionRuntime graphRuntime) throws Exception {
    if (status == Status.NOT_STARTED) {
      status = Status.RUNNING;
    }
    for (Iterator<String> iterator = currentPlanSet.iterator(); iterator.hasNext();) {
      String id = iterator.next();
      PlanVertex planVertex = executionPlan.get(id);
      LifecycleProcess process = planVertex.getProcess();
      if (process == null) {
        iterator.remove();
        updateResource(graphRuntime, id, planVertex);
        continue;
      }
      if (process.isComplete()) {
        process.setEndTimeMs(System.currentTimeMillis());
        // if this process is completed
        iterator.remove();
      }
      switch (process.getEndStatus()) {
      case NOT_STARTED:
        // submit for execution
        process.setExecutionId(executionId);
        process.setEndStatus(Status.RUNNING);
        process.setProcessId(executionId + "_" + planVertex.getProposedResource().getId());
        process.init();
        process.setStartTimeMs(System.currentTimeMillis());
        process.continueExecution(graphRuntime.getTaskRuntime());
        break;
      case SUCCEEDED:
        // push update
        // its automatically unlocked because the resource_local_owner of the proposed
        // resource should be null
        updateResource(graphRuntime, id, planVertex);
        if (planVertex.getCurrentResource() == null) {
          graphRuntime.getResourceDB().unlockProposedResource(id);
        }
        break;
      case FAILED:
      case CANCELLED:
        // unlock node
        graphRuntime.getResourceDB().unlockResource(planVertex.getProposedResource().getId());
        status = process.getEndStatus();
        break;
      case RUNNING:// update status
        process.continueExecution(graphRuntime.getTaskRuntime());
        break;
      }
    }
    if (currentPlanSet.isEmpty()) {
      // if any process fails mark the graph execution as failed
      if (executionPlan.values().stream().anyMatch(
          v -> v.getProcess() != null && v.getProcess().getEndStatus() == Status.FAILED)) {
        status = Status.FAILED;
      } else if (executionPlan.values().stream().allMatch(
          v -> v.getProcess() == null || v.getProcess().getEndStatus() == Status.SUCCEEDED)) {
        // if all processes succeed mark the graph execution as succeeded
        status = Status.SUCCEEDED;
      } else {
        throw new Exception(
            "Unexpected situation where there are no Vertices and the status is not set");
      }
    }
  }

  private void updateResource(GraphExecutionRuntime graphRuntime,
                              String id,
                              PlanVertex planVertex) throws IOException {
    graphRuntime.getResourceDB().updateResource(planVertex.getProposedResource());
    enqueuePlanVerticesWithSatissfiedDependencies();
    try {
      List<Resource> checkAndAddEdgeMutations = checkAndAddEdgeMutations(id);
      graphRuntime.getResourceDB().updateResources(checkAndAddEdgeMutations);
    } catch (RuntimeException e) {
      System.err.println(id + " & allEdgeMutations:" + allEdgeMutations);
    }
  }

  private List<Resource> checkAndAddEdgeMutations(String id) {
    List<Resource> updatedResources = new ArrayList<>();
    for (Entry<String, EdgeMutation> entry : allEdgeMutations.entrySet()) {
      if (entry.getKey().contains(id)) {
        EdgeMutation value = entry.getValue();
        String srcId = value.getSrcId();
        String dstId = value.getDestId();
        // other id is destination
        if ((executionPlan.get(srcId).getProcess() == null
            || executionPlan.get(srcId).getProcess().getEndStatus() == Status.SUCCEEDED)
            && (executionPlan.get(dstId).getProcess() == null
                || executionPlan.get(dstId).getProcess().getEndStatus() == Status.SUCCEEDED)) {
          // both vertices of this edge have completed execution therefore it's time to
          // update the edges
          Resource srcResource = executionPlan.get(srcId).getProposedResource();
          Resource dstResource = executionPlan.get(dstId).getProposedResource();

          // e.g.
          // EdgeMutation
          // [srcFieldName=consumer, dstFieldName=topics,
          // srcId=prn:kafka:dev:aws_us-east-1::datakafka08:test_topic,
          // destId=prn:merced:prod:aws_us-east-1::kafka:hour:test_topic, add=true]

          if (value.isParentChild()) {
            handleParentEdgeMutation(value, srcResource, dstResource);
          } else {
            handlePeerEdgeMutation(value, srcResource, dstResource);
          }
          updatedResources.add(srcResource);
          updatedResources.add(dstResource);
        }
      }
    }
    return updatedResources;
  }

  private void handleParentEdgeMutation(EdgeMutation value,
                                        Resource srcResource,
                                        Resource dstResource) {
    // src is always the parent of dst
    if (srcResource.getChildResources() == null) {
      srcResource.setChildResources(new HashSet<String>());
    }
    srcResource.getChildResources().add(dstResource.getId());
    dstResource.setParentResource(srcResource.getId());
  }

  private void handlePeerEdgeMutation(EdgeMutation value,
                                      Resource srcResource,
                                      Resource dstResource) {
    if (srcResource.getOutputResources() == null) {
      srcResource.setOutputResources(new HashMap<>(ResourceFactory.INSTANCE
          .getResourceDefinition(srcResource).getRequiredOutboundEdgeTypes().size()));
    }
    if (dstResource.getInputResources() == null) {
      dstResource.setInputResources(new HashMap<>(ResourceFactory.INSTANCE
          .getResourceDefinition(dstResource).getRequiredInboundEdgeTypes().size()));
    }
    Set<String> set1 = srcResource.getOutputResources().get(value.getSrcFieldName());
    if (set1 == null) {
      set1 = new HashSet<>();
      srcResource.getOutputResources().put(value.getSrcFieldName(), set1);
    }
    Set<String> set2 = dstResource.getInputResources().get(value.getDstFieldName());
    if (set2 == null) {
      set2 = new HashSet<>();
      dstResource.getInputResources().put(value.getDstFieldName(), set2);
    }
    if (value.isAdd()) {
      set1.add(dstResource.getId());
      set2.add(srcResource.getId());
    } else {
      System.out.println("Removing");
      set1.remove(dstResource.getId());
      set2.remove(srcResource.getId());
    }
  }

  public void enqueuePlanVerticesWithSatissfiedDependencies() {
    // get any vertices that can now be executed because it's upstream has beem
    // completed
    Set<String> completed = executionPlan.values().stream()
        .filter(v -> v.getProcess() == null || v.getProcess().getEndStatus() == Status.SUCCEEDED)
        .map(v -> v.getProposedResource().getId()).collect(Collectors.toSet());

    // find vertices whose dependencies have been satisfied so we can enqueue them
    // for execution
    List<PlanVertex> nonNullPlans = executionPlan.values().stream()
        .filter(v -> v.getProcess() != null && v.getProcess().getEndStatus() == Status.NOT_STARTED)
        .collect(Collectors.toList());
    for (PlanVertex v : nonNullPlans) {
      List<String> upstreamVertices = v.getUpstreamVertices();
      if (upstreamVertices == null || upstreamVertices.stream().filter(i -> i != null).count() == 0
          || completed.containsAll(upstreamVertices)) {
        currentPlanSet.add(v.getProposedResource().getId());
      }
    }
  }

  public Status getStatus() {
    return status;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public Map<String, PlanVertex> getExecutionPlan() {
    return executionPlan;
  }

  public void setExecutionPlan(Map<String, PlanVertex> executionPlan) {
    this.executionPlan = executionPlan;
  }

  public void setAllEdgeMutations(SortedMap<String, EdgeMutation> allEdgeMutations) {
    this.allEdgeMutations = allEdgeMutations;
  }

  public SortedMap<String, EdgeMutation> getAllEdgeMutations() {
    return allEdgeMutations;
  }

  public String getRequester() {
    return requester;
  }

  public void setRequester(String requester) {
    this.requester = requester;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  public String getStateStoragePath() {
    return stateStoragePath;
  }

  public void setStateStoragePath(String stateStoragePath) {
    this.stateStoragePath = stateStoragePath;
  }

  public Set<String> getCurrentPlanSet() {
    return currentPlanSet;
  }

  public void setCurrentPlanSet(Set<String> currentPlanSet) {
    this.currentPlanSet = currentPlanSet;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

}
