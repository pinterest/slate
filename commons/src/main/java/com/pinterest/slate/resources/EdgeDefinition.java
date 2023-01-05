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

import java.util.HashSet;
import java.util.Set;

public class EdgeDefinition {

  private Set<String> connectedResourceType;
  private int minCardinality;
  private int maxCardinality;

  public EdgeDefinition() {
  }

  public EdgeDefinition(String connectedResourceDefinition,
                         int minCardinality,
                         int maxCardinality) {
    this.connectedResourceType = new HashSet<>();
    this.connectedResourceType.add(connectedResourceDefinition);
    this.minCardinality = minCardinality;
    this.maxCardinality = maxCardinality;
  }
  
  public EdgeDefinition(Set<String> connectedResourceType,
                        int minCardinality,
                        int maxCardinality) {
   this.minCardinality = minCardinality;
   this.maxCardinality = maxCardinality;
   setConnectedResourceType(connectedResourceType);
 }

  public Set<String> getConnectedResourceType() {
    return connectedResourceType;
  }

  public void setConnectedResourceType(Set<String> connectedResources) {
    this.connectedResourceType = new HashSet<>();
    for (String resource : connectedResources) {
      this.connectedResourceType.add(resource);
    }
  }

  public int getMinCardinality() {
    return minCardinality;
  }

  public void setMinCardinality(int minCardinality) {
    this.minCardinality = minCardinality;
  }

  public int getMaxCardinality() {
    return maxCardinality;
  }

  public void setMaxCardinality(int maxCardinality) {
    this.maxCardinality = maxCardinality;
  }

  @Override
  public String toString() {
    return "EdgeDefinition [connectedResourceType=" + connectedResourceType + ", minCardinality="
        + minCardinality + ", maxCardinality=" + maxCardinality + "]";
  }

}