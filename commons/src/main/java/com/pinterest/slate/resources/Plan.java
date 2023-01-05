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

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Objects;
import com.pinterest.slate.process.LifecycleProcess;

public class Plan {

  private String updatedResourceId;
  private LifecycleProcess process;
  private List<String> upstreamVertexDependencyIds;
  private Resource proposedResource;

  public Plan() {
  }

  public Plan(Resource proposedResource,
              LifecycleProcess process,
              List<String> upstreamVertexDependencyIds) {
    this.proposedResource = proposedResource;
    this.upstreamVertexDependencyIds = upstreamVertexDependencyIds;
    this.process = process;
  }

  public Plan(Resource proposedResource,
              LifecycleProcess process,
              String... upstreamVertexDependencyIds) {
    this.proposedResource = proposedResource;
    if (upstreamVertexDependencyIds == null) {
      upstreamVertexDependencyIds = new String[0];
    }
    this.upstreamVertexDependencyIds = Arrays.asList(upstreamVertexDependencyIds);
    this.process = process;
  }

  public static Plan of(Resource proposedResource,
                        LifecycleProcess process,
                        String... upstreamVertexDependencyIds) {
    return new Plan(proposedResource, process, upstreamVertexDependencyIds);
  }

  public static Plan of(String updatedResourceId,
                        Resource proposedResource,
                        LifecycleProcess process,
                        String... upstreamVertexDependencyIds) {
    Plan p = new Plan(proposedResource, process, upstreamVertexDependencyIds);
    p.setUpdatedResourceId(updatedResourceId);
    return p;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(process, updatedResourceId, proposedResource, updatedResourceId);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Plan)) {
      return false;
    }
    Plan p = (Plan) obj;
    if (!p.getProcess().equals(process) || !p.getProposedResource().equals(proposedResource)
        || !p.getUpstreamVertexDependencyIds().equals(upstreamVertexDependencyIds)) {
      return false;
    }
    return true;
  }

  public Resource getProposedResource() {
    return proposedResource;
  }

  public void setProposedResource(Resource proposedResource) {
    this.proposedResource = proposedResource;
  }

  public List<String> getUpstreamVertexDependencyIds() {
    return upstreamVertexDependencyIds;
  }

  public void setUpstreamVertexDependencyIds(List<String> upstreamVertexDependencyIds) {
    this.upstreamVertexDependencyIds = upstreamVertexDependencyIds;
  }

  public LifecycleProcess getProcess() {
    return process;
  }

  public void setProcess(LifecycleProcess process) {
    this.process = process;
  }

  public String getUpdatedResourceId() {
    return updatedResourceId;
  }

  public void setUpdatedResourceId(String updatedResourceId) {
    this.updatedResourceId = updatedResourceId;
  }

  @Override
  public String toString() {
    return "Plan [" + hashCode() + " updatedResourceId=" + updatedResourceId + ", process=" + process
        + ", upstreamVertexDependencyIds=" + upstreamVertexDependencyIds + ", proposedResource="
        + proposedResource + "]";
  }
}