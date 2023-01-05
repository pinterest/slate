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

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.resources.Resource;

public class PlanVertex {

  private LifecycleProcess process;
  private List<String> upstreamVertices = new ArrayList<>();
  private Resource proposedResource;
  private Resource currentResource;
  private String oldId;
  private String newId;

  @Override
  public int hashCode() {
    return Objects.hashCode(process, upstreamVertices, proposedResource);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PlanVertex)) {
      return false;
    }
    PlanVertex pv = (PlanVertex) obj;
    if (!Objects.equal(pv.getProcess(), process) || !pv.getProposedResource().equals(proposedResource)
        || !pv.getUpstreamVertices().equals(upstreamVertices)) {
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

  public LifecycleProcess getProcess() {
    return process;
  }

  public void setProcess(LifecycleProcess process) {
    this.process = process;
  }

  public List<String> getUpstreamVertices() {
    return upstreamVertices;
  }

  public void setUpstreamVertices(List<String> upstreamVertices) {
    this.upstreamVertices = upstreamVertices;
  }

  public Resource getCurrentResource() {
    return currentResource;
  }

  public void setCurrentResource(Resource currentResource) {
    this.currentResource = currentResource;
  }

  public String getOldId() {
    return oldId;
  }

  public void setOldId(String oldId) {
    this.oldId = oldId;
  }

  public String getNewId() {
    return newId;
  }

  public void setNewId(String newId) {
    this.newId = newId;
  }

  @Override
  public String toString() {
    return "PlanVertex [process=" + process + ", upstreamVertices=" + upstreamVertices
        + ", proposedResource=" + proposedResource + ", currentResource=" + currentResource
        + ", oldId=" + oldId + ", newId=" + newId + "]";
  }
}