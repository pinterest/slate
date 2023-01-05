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

import java.util.Map;

import com.google.gson.JsonObject;

public class ResourceChange {

  private String requester;
  private Resource currentResourceObject;
  private Resource proposedResourceObject;
  private Map<String, Resource> deltaGraph;
  private JsonObject currentState;

  public ResourceChange(String requester,
                        Resource currentResourceObject,
                        JsonObject currentState,
                        Resource proposedResourceObject,
                        Map<String, Resource> deltaGraph) {
    this.requester = requester;
    this.currentResourceObject = currentResourceObject;
    this.currentState = currentState;
    this.proposedResourceObject = proposedResourceObject;
    this.deltaGraph = deltaGraph;
  }

  public String getRequester() {
    return requester;
  }

  public void setRequester(String requester) {
    this.requester = requester;
  }

  public Resource getCurrentResourceObject() {
    return currentResourceObject;
  }

  public void setCurrentResourceObject(Resource currentResourceObject) {
    this.currentResourceObject = currentResourceObject;
  }

  public Resource getProposedResourceObject() {
    return proposedResourceObject;
  }

  public void setProposedResourceObject(Resource proposedResourceObject) {
    this.proposedResourceObject = proposedResourceObject;
  }

  public Map<String, Resource> getDeltaGraph() {
    return deltaGraph;
  }

  public void setDeltaGraph(Map<String, Resource> deltaGraph) {
    this.deltaGraph = deltaGraph;
  }

  public JsonObject getCurrentState() {
    return currentState;
  }

  public void setCurrentState(JsonObject currentState) {
    this.currentState = currentState;
  }

  @Override
  public String toString() {
    return "ResourceChange [requester=" + requester + ", currentResourceObject="
        + currentResourceObject + ", proposedResourceObject=" + proposedResourceObject
        + ", deltaGraph=" + deltaGraph + ", currentState=" + currentState + "]";
  }

}
