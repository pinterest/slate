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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pinterest.slate.graph.AbstractResourceDB;

public interface ResourceDefinition {

  public static final Gson GSON = new Gson();

  public default JsonObject getConfigSchema() {
    return getConfigSchema(getSimpleName());
  }
  
  public String getDocumentation();

  public String getChatLink();

  public JsonObject getInternalSchema();

  public default JsonObject getConfigSchema(String name) {
    try {
      JsonObject internalSchema = getInternalSchema();
      JsonObject schema = new JsonObject();
      JsonObject topLevelSchema = getGlobalSchema();
      JsonArray required = topLevelSchema.get("required").getAsJsonArray();
      JsonObject topLevelProperties = topLevelSchema.get("properties").getAsJsonObject();
      schema.add("properties", topLevelProperties);
      schema.add("required", required);

      for (Entry<String, JsonElement> e : internalSchema.entrySet()) {
        if (e.getKey().equals("properties")) {
          JsonObject propsObject = e.getValue().getAsJsonObject();
          for (Entry<String, JsonElement> pe : propsObject.entrySet()) {
            topLevelProperties.add(pe.getKey(), pe.getValue());
          }
        } else if (e.getKey().equals("required")) {
          required.addAll(e.getValue().getAsJsonArray());
        } else {
          schema.add(e.getKey(), e.getValue());
        }
      }
      return schema;
    } catch (Exception e) {
      e.printStackTrace();
      return new JsonObject();
    }
  }

  public static JsonObject getGlobalSchema() {
    return GSON.fromJson("""
        {
          "properties": {
            "owner": {
              "type": "string",
              "title": "Owner LDAP Group",
              "description": "LDAP Group that owns this Resource.",
              "minLength": 1,
              "maxLength": 255
            },
            "project": {
              "type": "string",
              "title": "Project",
              "description": "Project does this Resource belong / chargeback to.",
              "minLength": 2,
              "maxLength": 255
            },
            "region": {
              "type": "string",
              "title": "Region",
              "description": "The region this Resource should be provisioned in.",
              "enum": [
                "us-east-1",
                "eu-west-1"
              ]
            },
            "environment": {
              "type": "string",
              "title": "Environment",
              "description": "The environment this Resource should be provisioned in.",
              "enum": [
                "prod",
                "control",
                "canary",
                "integ",
                "dev"
              ]
            }
          },
          "required": [
            "owner",
            "project",
            "region",
            "environment"
          ]
        }
        """, JsonObject.class);
  }

  public default void init(String configDirectory, AbstractResourceDB resourceDB) throws Exception {
  }

  public default BackgroundService getBackgroundService() {
    return null;
  }

  public String getAuthor();

  public default JsonObject getUiSchemaInternal() {
    return getUiSchema(getSimpleName());
  }

  public JsonObject getUiSchema();

  public default JsonObject getUiSchema(String name) {
    try {
      return ResourceFactory.readJsonFile("resources/" + name + "/uischema.json");
    } catch (Exception e) {
      return new JsonObject();
    }
  }

  /**
   * The plan method is fired if the graph engine detects changes on this
   * resource. The currentResourceObject and the proposedResourceObject are made
   * visible along with the relevant subGraph, this information should be used to
   * generate a {@link Plan}
   * 
   * This method is also responsible to analyzing the upstream dependencies for
   * the proposed changes. The function is expected consider input and output
   * resource changes / connections
   * 
   * @param change contains current state and proposed state along with the delta
   *               graph
   * @return plan containing process to execute the change and dependencies
   * @throws PlanException
   */
  public default Plan planChange(ResourceChange change) throws PlanException {
    return null;
  }

  /**
   * Return metrics for this Resource, these metrics are used to render chart in
   * UI.<br/>
   * <br/>
   * <br/>
   * 
   * Note: This method is primarily used by the UI but could also be leveraged by
   * direct users of the API.
   * 
   * @param resource
   * @return
   * @throws Exception 
   */
  public default List<MetricsDefinition> getMetrics(Resource resource) throws Exception {
    return null;
  }

  /**
   * Returns URLs to external systems regarding this resource and also if
   * (boolean) they can be rendered as an embedded widget inside Slate. <br/>
   * <br/>
   * <br/>
   * 
   * Note: This method is primarily used by the UI but could also be leveraged by
   * direct users of the API.
   * 
   * @param resource
   * @return map of external urls and flagged with whether or not they can be
   *         embedded.
   * @throws Exception 
   */
  public default Set<Tool> getTools(Resource resource) throws Exception {
    return null;
  }

  /**
   * Return a short description about this resource, this will be rendered on the
   * tooltip for this Resource type.
   * 
   * @return
   */
  public String getShortDescription();

  /**
   * Return a link to wiki / docs for users to read more about this Resource type
   * and how it works.
   * 
   * @return
   */
  public String getDocumentationLink();

  public default Map<String, EdgeDefinition> getRequiredOutboundEdgeTypes() {
    return ImmutableMap.of();
  }

  public default Map<String, EdgeDefinition> getRequiredInboundEdgeTypes() {
    return ImmutableMap.of();
  }
  
  public default EdgeDefinition getRequiredParentEdgeTypes() {
    return null;
  }
  
  public default Set<EdgeDefinition> getRequiredChildEdgeTypes() {
    return ImmutableSet.of();
  }

  /**
   * @return the current state of this resource
   */
  public default JsonObject readExternalCurrentState(Resource resource) throws Exception {
    return resource.getDesiredState();
  }

  public Set<String> getTags();

  public String getSimpleName();

  public Resource newInstance(String id);

  public default String getLanguage() {
    return "java";
  }

  public default List<String> l(String... ids) {
    return Arrays.asList(ids);
  }

  public default Iterator<List<Resource>> getAllBackfillResources(AbstractResourceDB resourceDB) throws Exception {
    return null;
  }

}
