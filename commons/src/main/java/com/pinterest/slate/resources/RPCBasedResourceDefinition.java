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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.utils.HttpUtils;

@IgnoreRD
public class RPCBasedResourceDefinition implements ResourceDefinition {

  private static final Logger logger = Logger
      .getLogger(RPCBasedResourceDefinition.class.getCanonicalName());
  private String baseUrl;
  private String chatLink;
  private String author;
  private JsonObject configSchema;
  private JsonObject internalSchema;
  private JsonObject uiSchema;
  private Set<String> tags;
  private Map<String, EdgeDefinition> requiredInboundEdgeTypes = null;
  private Map<String, EdgeDefinition> requiredOutboundEdgeTypes = null;
  private Set<EdgeDefinition> requiredChildEdgeTypes = null;
  private EdgeDefinition requiredParentEdgeTypes = null;
  private String documentationLink;
  private String shortDescription;
  private String resourceDefinitionClass;
  private String simpleName;
  private String documentation;

  public RPCBasedResourceDefinition(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  @Override
  public Plan planChange(ResourceChange change) throws PlanException {
    try {
      Plan plan = HttpUtils.makeHttpPost(baseUrl, change, new TypeToken<Plan>() {
      }.getType());
      return plan;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to plan resource(" + simpleName + ")", e);
      throw new PlanException(e);
    }
  }

  @Override
  public JsonObject readExternalCurrentState(Resource resource) throws Exception {
    try {
      return HttpUtils.makeHttpPost(baseUrl + "/currentstate", resource,
          new TypeToken<JsonObject>() {
          }.getType());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to get external current state resource(" + simpleName + ")", e);
      throw new PlanException(e);
    }
  }

  @Override
  public Iterator<List<Resource>> getAllBackfillResources(AbstractResourceDB resourceDB) throws Exception {
    return new Iterator<List<Resource>>() {

      int counter = 0;
      int index = 0;
      List<List<Resource>> result;

      @Override
      public List<Resource> next() {
        List<Resource> list = result.get(index++);
        if (list.size() > 0) {
          counter++;
          System.out.println(counter++);
        }
        return list;
      }

      @Override
      public boolean hasNext() {
        if (result == null || index == result.size()) {
          try {
            result = HttpUtils.makeHttpPost(baseUrl + "/backfill?pageSize=" + 20, null,
                new TypeToken<List<List<Resource>>>() {
                }.getType(), true);
            index = 0;// reset index after a new fetch
            if (result == null || result.size() == 0) {
              return false;
            } else {
              return true;
            }
          } catch (Exception e) {
            e.printStackTrace();
            return false;
          }
        } else {
          return true;
        }
      }
    };
  }

  @Override
  public Set<Tool> getTools(Resource resource) throws Exception {
    try {
      return HttpUtils.makeHttpPost(baseUrl + "/tools", resource, new TypeToken<Set<Tool>>() {
      }.getType(), true);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to get tools for resource(" + simpleName + ")", e);
      throw new PlanException(e);
    }
  }

  @Override
  public List<MetricsDefinition> getMetrics(Resource resource) throws Exception {
    try {
      return HttpUtils.makeHttpPost(baseUrl + "/metrics", resource,
          new TypeToken<List<MetricsDefinition>>() {
          }.getType());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to get metrics for resource(" + simpleName + ")", e);
      throw new PlanException(e);
    }
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void setInternalSchema(JsonObject internalSchema) {
    this.internalSchema = internalSchema;
  }

  @Override
  public String getChatLink() {
    return chatLink;
  }

  @Override
  public JsonObject getInternalSchema() {
    return internalSchema;
  }

  @Override
  public String getAuthor() {
    return author;
  }

  @Override
  public JsonObject getConfigSchema() {
    return configSchema;
  }

  public void setConfigSchema(JsonObject configSchema) {
    this.configSchema = configSchema;
  }

  @Override
  public JsonObject getUiSchema() {
    return uiSchema;
  }

  @Override
  public Map<String, EdgeDefinition> getRequiredInboundEdgeTypes() {
    return requiredInboundEdgeTypes;
  }

  @Override
  public Map<String, EdgeDefinition> getRequiredOutboundEdgeTypes() {
    return requiredOutboundEdgeTypes;
  }
  
  @Override
  public Set<EdgeDefinition> getRequiredChildEdgeTypes() {
    return requiredChildEdgeTypes;
  }

  public void setRequiredChildEdgeTypes(Set<EdgeDefinition> requiredChildEdgeTypes) {
    this.requiredChildEdgeTypes = requiredChildEdgeTypes;
  }

  public void setRequiredParentEdgeTypes(EdgeDefinition requiredParentEdgeTypes) {
    this.requiredParentEdgeTypes = requiredParentEdgeTypes;
  }

  @Override
  public String getDocumentationLink() {
    return documentationLink;
  }

  @Override
  public String getShortDescription() {
    return shortDescription;
  }

  @Override
  public Set<String> getTags() {
    return tags;
  }

  public EdgeDefinition getRequiredParentEdgeTypes() {
    return requiredParentEdgeTypes;
  }

  @Override
  public Resource newInstance(String id) {
    return new Resource(id, resourceDefinitionClass, 1, 1);
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  @Override
  public String getDocumentation() {
    return documentation;
  }

}
