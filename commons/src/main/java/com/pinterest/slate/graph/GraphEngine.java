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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.gson.JsonObject;
import com.pinterest.slate.resources.EdgeDefinition;
import com.pinterest.slate.resources.Plan;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceChange;
import com.pinterest.slate.resources.ResourceDefinition;
import com.pinterest.slate.resources.ResourceFactory;
import com.pinterest.slate.validation.ResourceValidationFactory;
import com.pinterest.slate.validation.ResourceValidator;

public class GraphEngine {

  private static final Logger logger = Logger.getLogger(GraphEngine.class.getCanonicalName());
  public static int MAX_ITERATIONS = 100;
  private AbstractResourceDB resourceDB;
  private GraphExecutor graphExecutor;
  private ResourceFactory resourceFactory;

  public GraphEngine(ResourceFactory resourceFactory,
                     AbstractResourceDB resourceDB,
                     GraphExecutor graphExecutor) throws Exception {
    this.resourceFactory = resourceFactory;
    this.resourceDB = resourceDB;
    this.graphExecutor = graphExecutor;
  }

  protected GraphEngine(ResourceFactory resourceFactory, AbstractResourceDB resourceDB) {
    this.resourceFactory = resourceFactory;
    this.resourceDB = resourceDB;
  }

  /**
   * 1. Check dependency satisfaction 2. Check and pull cascading updates 3.
   * 
   * @param deltaGraph
   * @return
   * @throws Exception
   */
  public Map<String, PlanVertex> planGraphUpdate(String requester,
                                                 Map<String, Resource> deltaGraph) throws Exception {
    Map<String, Resource> existingResourcesMap = new HashMap<>();
    for (Entry<String, Resource> entry : new HashSet<>(deltaGraph.entrySet())) {
      Resource value = entry.getValue();
      if (!entry.getKey().equals(value.getId())) {
        throw new PlanException(
            "The id of resource in the map and the id inside the resource must match for:"
                + value.getId());
      }
      normalizeResourceEdges(value);
      validateResource(value);
      Resource resourceById = resourceDB.getResourceById(value.getId());
      if (resourceById != null) {
        // check if resources are locked
        if (resourceById.getResourceLockOwner() != null) {
          throw new PlanException("Resource:" + value.getId() + " is already locked by:"
              + resourceById.getResourceLockOwner()
              + ", changes can't be performed until the lock is released");
        }
        existingResourcesMap.put(value.getId(), resourceById);
      }
      resolveResourceAndAddToGraph(value.getId(), deltaGraph, value.getInputResources(), true,
          existingResourcesMap);
      resolveResourceAndAddToGraph(value.getId(), deltaGraph, value.getOutputResources(), false,
          existingResourcesMap);
      resolveParentChildAndAddToGraph(value, resourceById, deltaGraph, existingResourcesMap);
      validateResourceInputsAndOutputs(value, deltaGraph);
    }
    validateMutualEdgeConnectivity(deltaGraph);
    Map<String, String> netSubstitutionMap = new HashMap<>();
    int i = 1;
    Map<String, PlanVertex> planGraph = null;
    while (i < MAX_ITERATIONS) {
      i++;
      try {
        Map<String, PlanVertex> tmpPlanGraph = new HashMap<>();
        netSubstitutionMap.putAll(runResourceLevelPlanning(requester, existingResourcesMap,
            deltaGraph, tmpPlanGraph, true));
        if (planGraph != null && tmpPlanGraph.equals(planGraph)) {
          break;
        } else {
          planGraph = tmpPlanGraph;
        }
      } catch (Exception e) {
      }
    }
    logger.info("Number of iterations:" + i);
    planGraph = new HashMap<>();
    runResourceLevelPlanning(requester, existingResourcesMap, deltaGraph, planGraph, false);
    for (Entry<String, String> entry : netSubstitutionMap.entrySet()) {
      PlanVertex planVertex = planGraph.get(entry.getKey());
      planVertex.setOldId(entry.getValue());
      planVertex.setNewId(entry.getKey());
    }
    for (Entry<String, PlanVertex> entry2 : planGraph.entrySet()) {
      PlanVertex v = entry2.getValue();
      if (v.getCurrentResource() != null && v.getCurrentResource().getResourceLockOwner() != null) {
        throw new PlanException("Resource:" + entry2.getKey() + " is already locked by "
            + v.getCurrentResource().getResourceLockOwner());
      }
      if (v.getCurrentResource() == null) {
        String id = v.getProposedResource().getId();
        String proposedResourceLockOwner = resourceDB.getProposedResourceLockOwner(id);
        if (proposedResourceLockOwner != null) {
          throw new PlanException("ProposedResource:" + id + " is already locked by "
              + proposedResourceLockOwner + ". This means there is already a running execution");
        }
      }
    }
    return planGraph;
  }

  /**
   * This method catches any partial graph connectivity problems. It's not going
   * to catch cases where there are multiple edges between the same vertices.
   * 
   * Consider a scenario where user forgets to type the graph correctly and only
   * connects one direction of the edge whereas Slate needs both direction of the
   * edges in order to establish the correct connectivity graph.
   * 
   * @param deltaGraph
   * @throws PlanException
   */
  private void validateMutualEdgeConnectivity(Map<String, Resource> deltaGraph) throws PlanException {
    for (Entry<String, Resource> entry : deltaGraph.entrySet()) {
      Resource r = entry.getValue();
      for (Entry<String, Set<String>> entry2 : r.getInputResources().entrySet()) {
        for (String i : entry2.getValue()) {
          try {
            if (!deltaGraph.get(i).getOutputResources().values().stream().flatMap(v -> v.stream())
                .anyMatch(p -> p.equals(entry.getKey()))) {
              throw new PlanException("Missign mutual edge connection, connect is specified in:"
                  + entry.getKey() + " but not in " + i);
            }
          } catch (Exception e) {
            // TODO figure out where to cut the graph for recursive planning
          }
        }
      }
      for (Entry<String, Set<String>> entry2 : r.getOutputResources().entrySet()) {
        for (String i : entry2.getValue()) {
          try {
            if (!deltaGraph.get(i).getInputResources().values().stream().flatMap(v -> v.stream())
                .anyMatch(p -> p.equals(entry.getKey()))) {
              throw new PlanException("Missign mutual edge connection, connect is specified in:"
                  + entry.getKey() + " but not in " + i);
            }
          } catch (Exception e) {
            // TODO figure out where to cut the graph for recursive planning
          }
        }
      }
      if (r.getChildResources() != null) {
        for (String i : r.getChildResources()) {
          try {
            if (!deltaGraph.get(i).getParentResource().equals(entry.getKey())) {
              throw new PlanException("Missign mutual edge connection, connect is specified in:"
                  + entry.getKey() + " but not in " + i);
            }
          } catch (Exception e) {
            // TODO figure out where to cut the graph for recursive planning
          }
        }
      }
      if (r.getParentResource() != null) {
        Resource parentResource = deltaGraph.get(r.getParentResource());
        if (parentResource.getChildResources() == null
            || !parentResource.getChildResources().contains(r.getId())) {
          throw new PlanException("Missign mutual edge connection, connect is specified in:"
              + r.getParentResource() + " but not in " + parentResource.getId());
        }
      }
    }
  }

  private Map<String, String> runResourceLevelPlanning(String requester,
                                                       Map<String, Resource> existingResourcesMap,
                                                       Map<String, Resource> deltaGraph,
                                                       Map<String, PlanVertex> planGraph,
                                                       boolean catchIndividualPlanExceptions) throws IOException,
                                                                                              PlanException,
                                                                                              Exception {
    // now iterate over the members of the deltaGraph to trigger the planning
    // process
    Map<String, String> idSubstitutionMap = new HashMap<>();
    for (Entry<String, Resource> entry : deltaGraph.entrySet()) {
      // check if this resource already exists
      Resource currentResource = existingResourcesMap.get(entry.getKey());
      Resource proposedResource = entry.getValue();
      JsonObject currentState = null;
      // validate the type is correct
      if (currentResource != null) {
        if (!currentResource.getResourceDefinitionClass()
            .equalsIgnoreCase(proposedResource.getResourceDefinitionClass())) {
          throw new Exception("Invalid change: Existing resource type"
              + currentResource.getResourceDefinitionClass() + " and proposed resource type("
              + proposedResource.getResourceDefinitionClass()
              + ") must be the same. Changing the type of an existing resource is now allowed at the moment.");
        }
        // fetch current state only if the resources exists
        currentState = getCurrentState(currentResource);
      }

      try {
        // lookup the class of the proposed resource
        ResourceDefinition def = resourceFactory.getResourceDefinition(proposedResource);
        Plan plan = def.planChange(new ResourceChange(requester, currentResource, currentState,
            proposedResource, deltaGraph));
        proposedResource.setDesiredState(plan.getProposedResource().getDesiredState());
        planGraph.putIfAbsent(proposedResource.getId(), new PlanVertex());
        PlanVertex planVertex = planGraph.get(proposedResource.getId());
        if (planVertex.getProcess() != null) {
          throw new Exception("Found conflicting vertices in the graph");
        }
        // perform validation of the process & it's tasks to confirm that this graph has
        // the pre-requisites to execute
        initializePlanVertex(currentResource, proposedResource, plan, planVertex);
        if (plan.getProcess() != null) {
          plan.getProcess().validateTasks();
        }
        String rectifiedId = plan.getUpdatedResourceId();
        if (rectifiedId != null && !rectifiedId.equals(plan.getProposedResource().getId())) {
          Resource resourceById = resourceDB.getResourceById(rectifiedId);
          if (resourceById != null) {
            existingResourcesMap.put(rectifiedId, resourceById);
            // let's be safer and fail the planning to force customers to query the latest
            // resource from the graph in the UI
            logger.info(
                "Id rectification needed as the resource is existing resource but invalid id provided existingid:"
                    + rectifiedId + " newid:" + plan.getProposedResource().getId());
            throw new PlanException(
                "Are you using the latest version of the existing Resource? Invalid id provided to modify exsiting resource existingid:"
                    + rectifiedId + " proposedid:" + plan.getProposedResource().getId());
          }
          performIdSubstitution(deltaGraph, idSubstitutionMap, proposedResource, rectifiedId);
        }
      } catch (Exception e) {
        if (!catchIndividualPlanExceptions) {
          throw e;
        }
      }
    }
    // update resource ids in the map
    if (!idSubstitutionMap.isEmpty()) {
      for (Entry<String, String> entry : idSubstitutionMap.entrySet()) {
        Resource tmp = deltaGraph.remove(entry.getValue());
        deltaGraph.put(entry.getKey(), tmp);
        tmp.setId(entry.getKey());
        logger.info("Id substitution before:" + entry.getValue() + " after:" + entry.getKey());
      }
    }
    return idSubstitutionMap;
  }

  /**
   * Perform Id substitution if the proposed Id is not the same as Id generated
   * from planner. This is a critical step to perform de-duplication of resources.
   * 
   * @param deltaGraph
   * @param idSubstitutionMap
   * @param proposedResource
   * @param newId
   * @param planVertex
   */
  private void performIdSubstitution(Map<String, Resource> deltaGraph,
                                     Map<String, String> idSubstitutionMap,
                                     Resource proposedResource,
                                     String newId) {
    String oldId = proposedResource.getId();
    for (Entry<String, Set<String>> inputs : proposedResource.getInputResources().entrySet()) {
      for (String ids : inputs.getValue()) {
        for (Entry<String, Set<String>> out : deltaGraph.get(ids).getOutputResources().entrySet()) {
          if (out.getValue().remove(oldId)) {
            out.getValue().add(newId);
          }
        }
      }
    }
    for (Entry<String, Set<String>> outputs : proposedResource.getOutputResources().entrySet()) {
      for (String ids : outputs.getValue()) {
        for (Entry<String, Set<String>> ins : deltaGraph.get(ids).getInputResources().entrySet()) {
          if (ins.getValue().remove(oldId)) {
            ins.getValue().add(newId);
          }
        }
      }
    }
    idSubstitutionMap.put(newId, oldId);
  }

  private void initializePlanVertex(Resource currentResource,
                                    Resource proposedResource,
                                    Plan plan,
                                    PlanVertex planVertex) {
    planVertex.setUpstreamVertices(plan.getUpstreamVertexDependencyIds());
    planVertex.setProposedResource(proposedResource);
    planVertex.setProcess(plan.getProcess());
    planVertex.setCurrentResource(currentResource);
  }

  private void validateResource(Resource resource) throws PlanException {
    for (ResourceValidator resourceValidator : ResourceValidationFactory.getInstance()
        .getValidators()) {
      resourceValidator.validate(resource);
    }
  }

  private void normalizeResourceEdges(Resource value) {
    // remove unwanted desired state duplication
    value.getDesiredState().remove("owner");
    value.getDesiredState().remove("project");
    value.getDesiredState().remove("region");
    value.getDesiredState().remove("environment");
    Map<String, Set<String>> inputs = new HashMap<>();
    if (value.getInputResources() != null) {
      // add edges or replace existing ones on the input / output
      inputs.putAll(value.getInputResources());
    }
    value.setInputResources(inputs);

    Map<String, Set<String>> outputs = new HashMap<>();
    if (value.getOutputResources() != null) {
      // add edges or replace existing ones on the input / output
      outputs.putAll(value.getOutputResources());
    }
    value.setOutputResources(outputs);
  }

  private JsonObject getCurrentState(Resource currentResource) throws PlanException {
    ResourceDefinition def = resourceFactory.getResourceDefinition(currentResource);
    try {
      JsonObject currentState = def.readExternalCurrentState(currentResource);
      return currentState;
    } catch (Exception e) {
      throw new PlanException(e);
    }
  }

  public ExecutionGraph executeGraphUpate(String requester,
                                          Map<String, Resource> deltaGraph) throws Exception {
    if (requester == null || requester.isEmpty()) {
      throw new PlanException("Invalid requester");
    }
    Map<String, PlanVertex> planGraphUpdate = planGraphUpdate(requester, deltaGraph);
    List<String> ids = planGraphUpdate.values().stream().map(p -> p.getProposedResource().getId())
        .collect(Collectors.toList());
    // lock all resources, if resources don't exist it will skip locking those
    resourceDB.lockResources(requester, ids);
    try {
      // locking needs to be done before confirming the execution of this graph will
      // be performed if the locks fail we will be able to safely reject the execution
      ExecutionGraph executionGraph = new ExecutionGraph();
      executionGraph.setExecutionId(requester + "_" + System.currentTimeMillis());
      executionGraph.setRequester(requester);
      executionGraph.setExecutionPlan(planGraphUpdate);
      // extract all edge mutations so edges are not created in desired state unless
      // the respective plan operations succeed
      SortedMap<String, EdgeMutation> allEdgeMutations = generateAllEdgeMutations(planGraphUpdate);
      executionGraph.setAllEdgeMutations(allEdgeMutations);
      logger.info("Edge mutations:" + allEdgeMutations);
      // if (true) {
      // return executionGraph;
      // }
      // since edge mutations have been calculated we don't need to maintain the edge
      // states in proposed resource, instead we should revert back to the current
      // edges so we can add one edge at a time
      planGraphUpdate.values().stream().forEach(u -> {
        u.getProposedResource().setInputResources(
            u.getCurrentResource() != null ? u.getCurrentResource().getInputResources()
                : new HashMap<>());
        u.getProposedResource().setOutputResources(
            u.getCurrentResource() != null ? u.getCurrentResource().getOutputResources()
                : new HashMap<>());
        u.getProposedResource().setChildResources(
            u.getCurrentResource() != null ? u.getCurrentResource().getChildResources()
                : new HashSet<String>());
        u.getProposedResource().setParentResource(
            u.getCurrentResource() != null ? u.getCurrentResource().getParentResource() : null);
      });
      // prime execution graph for run
      executionGraph.enqueuePlanVerticesWithSatissfiedDependencies();
      graphExecutor.executeGraph(executionGraph);
      // now submit graph for execution
      return executionGraph;
    } catch (Exception e) {
      // unlock resources since the execution has failed
      e.printStackTrace();
      resourceDB.unlockResources(ids);
      throw e;
    }
  }

  private SortedMap<String, EdgeMutation> generateAllEdgeMutations(Map<String, PlanVertex> planGraphUpdate) {
    SortedMap<String, EdgeMutation> mutationMap = new TreeMap<>();
    for (Entry<String, PlanVertex> entry : planGraphUpdate.entrySet()) {
      Resource currentResource = entry.getValue().getCurrentResource();
      Resource proposedResource = entry.getValue().getProposedResource();

      Map<String, Set<String>> currentInputs = currentResource != null
          && currentResource.getInputResources() != null ? currentResource.getInputResources()
              : new HashMap<>();
      Map<String, Set<String>> proposedInputs = proposedResource != null
          && proposedResource.getInputResources() != null ? proposedResource.getInputResources()
              : new HashMap<>();
      generateMutationsResourceEdges(proposedResource, true, mutationMap, currentInputs,
          proposedInputs);

      Map<String, Set<String>> currentOutputs = currentResource != null
          && currentResource.getOutputResources() != null ? currentResource.getOutputResources()
              : new HashMap<>();
      Map<String, Set<String>> proposedOutputs = proposedResource != null
          && proposedResource.getOutputResources() != null ? proposedResource.getOutputResources()
              : new HashMap<>();
      generateMutationsResourceEdges(proposedResource, false, mutationMap, currentOutputs,
          proposedOutputs);

      Set<String> currentChildren = currentResource != null
          && currentResource.getChildResources() != null ? currentResource.getChildResources()
              : new HashSet<String>();
      Set<String> proposedChildren = proposedResource != null
          && proposedResource.getChildResources() != null ? proposedResource.getChildResources()
              : new HashSet<String>();
      generateParentChildMutationsResourceEdges(currentResource, proposedResource, currentChildren,
          proposedChildren, mutationMap);
    }
    return mutationMap;
  }

  private void generateParentChildMutationsResourceEdges(Resource currentResource,
                                                         Resource proposedResource,
                                                         Set<String> currentChildren,
                                                         Set<String> proposedChildren,
                                                         SortedMap<String, EdgeMutation> mutationMap) {
    SetView<String> removed = Sets.difference(currentChildren, proposedChildren);
    for (String r : removed) {
      mutationMap.put("pc_" + proposedResource.getId() + "_" + r,
          new EdgeMutation(proposedResource.getId(), r, false, true));
    }

    SetView<String> added = Sets.difference(proposedChildren, currentChildren);
    for (String r : added) {
      mutationMap.put("pc_" + proposedResource.getId() + "_" + r,
          new EdgeMutation(proposedResource.getId(), r, true, true));
    }

  }

  private void generateMutationsResourceEdges(Resource proposedResource,
                                              boolean isInput,
                                              Map<String, EdgeMutation> mutationMap,
                                              Map<String, Set<String>> currAry,
                                              Map<String, Set<String>> propAry) {
    Set<String> keys = new HashSet<>();
    keys.addAll(currAry.keySet());
    keys.addAll(propAry.keySet());
    for (String key : keys) {
      Set<String> curr = currAry.get(key);
      Set<String> prop = propAry.get(key);
      if ((curr == null || curr.isEmpty()) && (prop == null || prop.isEmpty())) {
        // skip both are null no-op
        curr = ImmutableSet.of();
        prop = ImmutableSet.of();
      } else if ((curr == null || curr.isEmpty()) && (prop != null && !prop.isEmpty())) {
        // new edges added
        curr = ImmutableSet.of();
      } else if ((curr != null && !curr.isEmpty()) && (prop == null || prop.isEmpty())) {
        // old edge removed
        prop = ImmutableSet.of();
      }
      // neither is null so perform deep compare
      SetView<String> difference = Sets.difference(curr, prop);
      // remove deleted resources
      generateMutationOperation(proposedResource, isInput, mutationMap, key, difference, false);
      // add new resources
      difference = Sets.difference(prop, curr);
      generateMutationOperation(proposedResource, isInput, mutationMap, key, difference, true);
    }
  }

  private void generateMutationOperation(Resource proposedResource,
                                         boolean isInput,
                                         Map<String, EdgeMutation> mutationMap,
                                         String key,
                                         SetView<String> difference,
                                         boolean add) {
    for (String id : difference) {
      EdgeMutation mut = null;
      if (isInput) {
        mut = new EdgeMutation(id, proposedResource.getId(), add);
        EdgeMutation edgeMutation = mutationMap.get(mut.getId());
        if (edgeMutation != null) {
          mut = edgeMutation;
        }
        mut.setDstFieldName(key);
      } else {
        mut = new EdgeMutation(proposedResource.getId(), id, add);
        EdgeMutation edgeMutation = mutationMap.get(mut.getId());
        if (edgeMutation != null) {
          mut = edgeMutation;
        }
        mut.setSrcFieldName(key);
      }
      mutationMap.put(mut.getId(), mut);
    }
  }

  private void resolveResourceAndAddToGraph(String rid,
                                            Map<String, Resource> deltaGraph,
                                            Map<String, Set<String>> dependencies,
                                            boolean isInput,
                                            Map<String, Resource> existingResourcesMap) throws PlanException {
    if (dependencies == null) {
      return;
    }
    for (Set<String> ids : dependencies.values()) {
      if (ids == null) {
        continue;
      }
      for (String id : ids) {
        if (!deltaGraph.containsKey(id)) {
          boolean anyMatch = false;
          try {
            Resource r = resourceDB.getResourceById(id);
            if (r == null) {
              throw new PlanException(
                  "Error finding Resource:" + id + ", please check your input/outputs");
            }

            if (isInput && !anyMatch) {
              // then scan for the output of these dependent resources to confirm whether or
              // not the cross dependency has been added
              anyMatch = r.getOutputResources().values().stream().flatMap(v -> v.stream())
                  .anyMatch(v -> {
                    return v.equals(rid);
                  });
            } else if (!isInput && !anyMatch) {
              anyMatch = r.getInputResources().values().stream().flatMap(v -> v.stream())
                  .anyMatch(v -> {
                    return v.equals(rid);
                  });
            }

            deltaGraph.put(id, r);
            existingResourcesMap.put(id, r);
          } catch (IOException e) {
            throw new PlanException("Error finding Resource:" + id
                + ", this resource must exist either in deltagraph or in resourcedb");
          }
          if (!anyMatch) {
            throw new PlanException("Resource(" + id + ") is missing edge from " + rid);
          }
        }
      }
    }
  }

  private void resolveParentChildAndAddToGraph(Resource proposed,
                                               Resource current,
                                               Map<String, Resource> deltaGraph,
                                               Map<String, Resource> existingResourcesMap) throws PlanException {
    Set<String> set = new HashSet<String>();
    if (proposed.getChildResources() != null) {
      set.addAll(proposed.getChildResources());
    }
    if (current != null && current.getChildResources() != null) {
      set.addAll(current.getChildResources());
    }
    for (String id : set) {
      if (!deltaGraph.containsKey(id)) {
        try {
          Resource r = resourceDB.getResourceById(id);
          if (r == null) {
            throw new IOException();
          }
          deltaGraph.put(id, r);
          existingResourcesMap.put(id, r);
        } catch (IOException e) {
          throw new PlanException("Error finding Resource:" + id
              + ", this resource must exist either in deltagraph or in resourcedb");
        }
      }
    }
  }

  private void validateResourceInputsAndOutputs(Resource proposedResource,
                                                Map<String, Resource> deltaSubGraph) throws PlanException {
    ResourceDefinition def = resourceFactory.getResourceDefinition(proposedResource);
    Map<String, Set<String>> inputResources = proposedResource.getInputResources();
    Map<String, Set<String>> outputResources = proposedResource.getOutputResources();
    Map<String, EdgeDefinition> requiredInboundEdgeTypes = def.getRequiredInboundEdgeTypes();
    Map<String, EdgeDefinition> requiredOutboundEdgeTypes = def.getRequiredOutboundEdgeTypes();

    if ((inputResources == null || inputResources.size() == 0)
        && requiredInboundEdgeTypes.size() > 0
        && requiredInboundEdgeTypes.values().stream().anyMatch(r -> r.getMinCardinality() > 0)) {
      throw new PlanException("Missing input connection for " + proposedResource.getId()
          + ", needed:" + requiredInboundEdgeTypes);
    }
    if ((outputResources == null || outputResources.size() == 0)
        && requiredOutboundEdgeTypes.size() > 0
        && requiredOutboundEdgeTypes.values().stream().anyMatch(r -> r.getMinCardinality() > 0)) {
      throw new PlanException("Missing output connection for " + proposedResource.getId()
          + ", needed:" + requiredOutboundEdgeTypes);
    }
    validateResourceEdgeType(proposedResource, deltaSubGraph, inputResources,
        requiredInboundEdgeTypes);
    validateResourceEdgeType(proposedResource, deltaSubGraph, outputResources,
        requiredOutboundEdgeTypes);

    // check parent / child relationships
    String parentResource = proposedResource.getParentResource();
    if (def.getRequiredParentEdgeTypes() != null) {
      if (parentResource == null || deltaSubGraph.get(parentResource) == null) {
        throw new PlanException(
            "Missing required parent resource " + proposedResource.getResourceDefinitionClass()
                + " from the delta graph " + def.getRequiredParentEdgeTypes());
      }
    }
    Set<String> childResources = proposedResource.getChildResources();
    if ((childResources == null || childResources.isEmpty())
        && (def.getRequiredChildEdgeTypes() != null)
        && def.getRequiredChildEdgeTypes().stream().anyMatch(r -> r.getMinCardinality() > 0)) {
      throw new PlanException("Missing child connection for " + proposedResource.getId()
          + ", needed:" + def.getRequiredChildEdgeTypes());
    }
    validateChildEdgeType(proposedResource, deltaSubGraph, def.getRequiredChildEdgeTypes());
    validateParentEdgeType(proposedResource, deltaSubGraph, def.getRequiredParentEdgeTypes());
  }

  private void validateParentEdgeType(Resource proposedResource,
                                      Map<String, Resource> resolvedSubGraph,
                                      EdgeDefinition parentEdgeType) throws PlanException {
    String parentResource = proposedResource.getParentResource();
    if (parentResource == null) {
      return;
    }
    Resource resource = resolvedSubGraph.get(parentResource);
    if (!parentEdgeType.getConnectedResourceType()
        .contains(resource.getResourceDefinitionClass())) {
      throw new PlanException("Invalid parent edge type for child:" + proposedResource.getId() + " "
          + parentEdgeType.getConnectedResourceType() + " vs "
          + resource.getResourceDefinitionClass());
    }
  }

  private void validateChildEdgeType(Resource proposedResource,
                                     Map<String, Resource> resolvedSubGraph,
                                     Set<EdgeDefinition> set) throws PlanException {
    Set<String> childResources = proposedResource.getChildResources();
    if (childResources == null) {
      return;
    }
    if (set == null) {
      throw new PlanException("Unexpected child edges for resourcedefinition:"
          + proposedResource.getResourceDefinitionClass());
    }
    Set<String> flatSet = set.stream()
        .filter(c -> c != null && c.getConnectedResourceType() != null)
        .flatMap(c -> c.getConnectedResourceType().stream()).collect(Collectors.toSet());
    Map<String, Set<String>> rByType = new HashMap<String, Set<String>>();
    for (String r : childResources) {
      Resource c = resolvedSubGraph.get(r);
      if (c == null) {
        throw new PlanException("Incomplete edge parent(" + proposedResource.getId() + ") to child("
            + r + "), please add edge both sides");
      }
      if (!proposedResource.getId().equals(c.getParentResource())) {
        throw new PlanException("Incomplete edge child(" + r + ") to parent("
            + proposedResource.getId() + "), please add edge both sides");
      }
      if (!flatSet.contains(c.getResourceDefinitionClass())) {
        throw new PlanException("Invalid child resource:" + r + " only " + set + " are allowed");
      }
      Set<String> set2 = rByType.get(c.getResourceDefinitionClass());
      if (set2 == null) {
        set2 = new HashSet<String>();
        rByType.put(c.getResourceDefinitionClass(), set2);
      }
      set2.add(r);
    }
    for (EdgeDefinition edgeDefinition : set) {
      Set<String> connectedResourceType = edgeDefinition.getConnectedResourceType();
      int c = 0;
      for (String s : connectedResourceType) {
        Set<String> set2 = rByType.get(s);
        if (set2 != null) {
          c += set2.size();
        }
      }
      if (edgeDefinition.getMaxCardinality() < c || edgeDefinition.getMinCardinality() > c) {
        throw new PlanException("Cardinality mismatch for parent(" + proposedResource.getId()
            + ") for edge:" + edgeDefinition);
      }
    }
  }

  private void validateResourceEdgeType(Resource proposedResource,
                                        Map<String, Resource> resolvedSubGraph,
                                        Map<String, Set<String>> resources,
                                        Map<String, EdgeDefinition> requiredEdgeTypes) throws PlanException {
    if (resources == null) {
      return;
    }
    for (Entry<String, Set<String>> entry : resources.entrySet()) {
      String i = entry.getKey();
      Set<String> ids = entry.getValue();
      if (ids == null) {
        continue;
      }
      EdgeDefinition edgeDefinition = requiredEdgeTypes.get(i);
      if (edgeDefinition == null) {
        throw new PlanException("Invalid edge definition for:" + i);
      }
      Set<String> connectedResourceType = edgeDefinition.getConnectedResourceType();
      if (!proposedResource.isDeleted()) {
        if (ids.size() < edgeDefinition.getMinCardinality()) {
          throw new PlanException("Min cardinality constraint violated:" + proposedResource.getId()
              + " at index:" + i + " of type:" + proposedResource.getResourceDefinitionClass());
        }
        if (ids.size() > edgeDefinition.getMaxCardinality()) {
          throw new PlanException("Max cardinality constraint violated:" + proposedResource.getId()
              + " at index:" + i + " of type:" + proposedResource.getResourceDefinitionClass());
        }
      }
      for (String id : ids) {
        Resource resource = resolvedSubGraph.get(id);
        if (resource == null) {
          throw new PlanException(
              "Resource(" + id + ") doesn't exist, its proposed to be connected to:"
                  + proposedResource.getId() + " at position:" + i);
        }
        if (!connectedResourceType.contains(resource.getResourceDefinitionClass())) {
          throw new PlanException(
              "Invalid connection, expected" + connectedResourceType.toString() + " got:" + id);
        }
      }
    }
  }

  public GraphExecutor getGraphExecutor() {
    return graphExecutor;
  }

}
