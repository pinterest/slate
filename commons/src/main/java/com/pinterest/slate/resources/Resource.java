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
package com.pinterest.slate.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.pinterest.slate.graph.ResourceEdgeAttributeCSVConverter;
import com.pinterest.slate.graph.ResourceSetAttributeCSVConverter;
import com.pinterest.slate.graph.storage.JsonAttributeConverter;
import com.pinterest.slate.graph.storage.SetAttributeCSVConverter;

@Entity
@Table(name = "resource")
@NamedQueries({
    @NamedQuery(name = "searchResourcesLike", query = "select r.id, r.resourceDefinitionClass from Resource r where r.id like :idPrefix"),
    @NamedQuery(name = "resourceTimestamp", query = "select r.id, r.lastUpdateTimestamp from Resource r where r.id in :ids"),
    @NamedQuery(name = "resourceStats", query = "select r.resourceDefinitionClass, count(r.id) from Resource r where r.resourceDefinitionClass in :defs group by r.resourceDefinitionClass"),
    @NamedQuery(name = "searchResource", query = "select r.id, r.resourceDefinitionClass from Resource r where (:rdc is NULL or r.resourceDefinitionClass = :rdc) and (:idContents is NULL or r.id like :idContents) and (:project is NULL or r.project = :project) and (:owner is NULL or r.owner = :owner)"),
    @NamedQuery(name = "searchResourceCount", query = "select count(r.id) from Resource r where (:rdc is NULL or r.resourceDefinitionClass = :rdc) and (:idContents is NULL or r.id like :idContents) and (:project is NULL or r.project = :project) and (:owner is NULL or r.owner = :owner)"),
    @NamedQuery(name = "allProjects", query = "select distinct(r.project) from Resource r "),
    @NamedQuery(name = "allOwners", query = "select distinct(r.owner) from Resource r ") })
public class Resource implements Serializable {

  private static final long serialVersionUID = 1L;
  // if resource is locked then this value should not be null
  @Id
  protected String id;
  @Column(name = "resource_definition_class")
  @NotNull
  protected String resourceDefinitionClass;
  @Column(name = "resource_lock_owner")
  protected String resourceLockOwner;
  @Column(name = "desired_state")
  @Convert(converter = JsonAttributeConverter.class)
  protected JsonObject desiredState = new JsonObject();
  @Column(name = "environment")
  @NotNull
  protected String environment;
  @Column(name = "project")
  @NotNull
  protected String project;
  @Column(name = "owner")
  @NotNull
  protected String owner;
  @Column(name = "region")
  @NotNull
  protected String region;
  @Column(name = "resource_watch_list")
  @Convert(converter = SetAttributeCSVConverter.class)
  protected Set<String> resourceWatchList = ImmutableSet.of();
  @Column(name = "input_resource_ids")
  @Convert(converter = ResourceEdgeAttributeCSVConverter.class)
  private Map<String, Set<String>> inputResources = null;
  @Column(name = "output_resource_ids")
  @Convert(converter = ResourceEdgeAttributeCSVConverter.class)
  private Map<String, Set<String>> outputResources = null;
  @Column(name = "parent_resource_id")
  private String parentResource = null;
  @Column(name = "child_resource_ids")
  @Convert(converter = ResourceSetAttributeCSVConverter.class)
  private Set<String> childResources = null;
  @Column(name = "deleted")
  @NotNull
  protected boolean deleted = false;
  @Column(name = "last_update_timestamp")
  // this can be used as the version number
  private long lastUpdateTimestamp;
  
  public Resource() {
  }

  public Resource(String id,
                  String resourceDefinitionClass,
                  int numberOfInputResources,
                  int numberOfOutputResources) {
    this.id = id;
    this.resourceDefinitionClass = resourceDefinitionClass;
    this.inputResources = new HashMap<>(numberOfInputResources);
    this.outputResources = new HashMap<>(numberOfOutputResources);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, desiredState, project, region, resourceWatchList, inputResources,
        outputResources);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Resource)) {
      return false;
    }
    Resource r = (Resource) obj;
    if (!r.getDesiredState().equals(desiredState) || !r.getId().equals(id)
        || !r.getProject().equals(project) || !r.getOwner().equals(owner)
        || !r.getRegion().equals(region) || !r.getInputResources().equals(inputResources)
        || !r.getOutputResources().equals(outputResources)
        || !r.getResourceWatchList().equals(resourceWatchList)) {
      return false;
    }
    return true;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getResourceDefinitionClass() {
    return resourceDefinitionClass;
  }

  public void setResourceDefinitionClass(String resourceDefinitionClass) {
    this.resourceDefinitionClass = resourceDefinitionClass;
  }

  public String getResourceLockOwner() {
    return resourceLockOwner;
  }

  public void setResourceLockOwner(String resourceLockOwner) {
    this.resourceLockOwner = resourceLockOwner;
  }

  public JsonObject getDesiredState() {
    return desiredState;
  }

  public void setDesiredState(JsonObject desiredState) {
    this.desiredState = desiredState;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public Set<String> getResourceWatchList() {
    return resourceWatchList;
  }

  public void setResourceWatchList(Set<String> resourceWatchList) {
    this.resourceWatchList = resourceWatchList;
  }

  public Map<String, Set<String>> getInputResources() {
    return inputResources;
  }

  public void setInputResources(Map<String, Set<String>> inputResources) {
    this.inputResources = inputResources;
  }

  public Map<String, Set<String>> getOutputResources() {
    return outputResources;
  }

  public void setOutputResources(Map<String, Set<String>> outputResources) {
    this.outputResources = outputResources;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public List<String> getNonNullConnections() {
    List<String> connections = new ArrayList<>();
    if (inputResources != null) {
      for (Set<String> id : inputResources.values()) {
        if (id != null) {
          connections.addAll(id);
        }
      }
    }
    if (outputResources != null) {
      for (Set<String> id : outputResources.values()) {
        if (id != null) {
          connections.addAll(id);
        }
      }
    }
    return connections;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public long getLastUpdateTimestamp() {
    return lastUpdateTimestamp;
  }

  public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
    this.lastUpdateTimestamp = lastUpdateTimestamp;
  }

  public String getParentResource() {
    return parentResource;
  }

  public void setParentResource(String parentResource) {
    this.parentResource = parentResource;
  }

  public Set<String> getChildResources() {
    return childResources;
  }

  public void setChildResources(Set<String> childResources) {
    this.childResources = childResources;
  }

  @Override
  public String toString() {
    return "Resource [id=" + id + ", resourceDefinitionClass=" + resourceDefinitionClass
        + ", resourceLockOwner=" + resourceLockOwner + ", desiredState=" + desiredState
        + ", environment=" + environment + ", project=" + project + ", owner=" + owner + ", region="
        + region + ", resourceWatchList=" + resourceWatchList + ", inputResources=" + inputResources
        + ", outputResources=" + outputResources + ", parentResource=" + parentResource
        + ", childResources=" + childResources + ", deleted=" + deleted + ", lastUpdateTimestamp="
        + lastUpdateTimestamp + "]";
  }

}
