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
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.utils.DaemonThreadFactory;
import com.pinterest.slate.utils.HttpUtils;

public class ResourceFactory {

  private static final String API_RESOURCES = "/api/v1/resources";
  private static final Logger logger = Logger.getLogger(ResourceFactory.class.getCanonicalName());
  protected static final Gson gson = new Gson();

  public static ResourceFactory INSTANCE = new ResourceFactory();
  protected Map<String, ResourceDefinition> resourceMap = new HashMap<>();
  protected Map<String, Set<String>> resourceTagMap = new HashMap<>();
  // map the resource definition className to the URL of this resource definition
  private Map<String, String> resourceDefinitionLocalityMap = new HashMap<>();

  protected ResourceFactory() {
  }

  public void init(SlateConfig config, AbstractResourceDB graph) throws Exception {
    HttpUtils.doNothing();
    Executors.newScheduledThreadPool(1, new DaemonThreadFactory()).scheduleAtFixedRate(
        () -> loadResourceDefinitionsFromSatellites(config, graph), 0, 20, TimeUnit.SECONDS);
  }

  private void loadResourceDefinitionsFromSatellites(SlateConfig config, AbstractResourceDB graph) {
    Type resourceMapType = new TypeToken<Map<String, RPCBasedResourceDefinition>>() {
    }.getType();
    Type tagMapType = new TypeToken<Map<String, Set<String>>>() {
    }.getType();
    for (String url : config.getSatelliteServerUrls()) {
      Map<String, RPCBasedResourceDefinition> map = null;
      try {
        map = HttpUtils.makeHttpGet(url + API_RESOURCES + "/definitions", resourceMapType);
      } catch (Exception e) {
        continue;
      }
      Map<String, Set<String>> tagMap;
      try {
        tagMap = HttpUtils.makeHttpGet(url + API_RESOURCES + "/tags", tagMapType);
      } catch (IOException e) {
        continue;
      }
      for (Entry<String, RPCBasedResourceDefinition> entry : map.entrySet()) {
        String resourceClass = entry.getKey();
        RPCBasedResourceDefinition newInstance = entry.getValue();
        String baseUrl = url + API_RESOURCES + "/" + resourceClass;
        newInstance.setBaseUrl(baseUrl);
        resourceDefinitionLocalityMap.put(baseUrl, entry.getKey());
        try {
          String canonicalName = resourceClass;
          JsonObject fixConfigSchema = fixConfigSchema(newInstance);
          newInstance.setConfigSchema(fixConfigSchema);

          if (newInstance.getRequiredParentEdgeTypes() != null) {
            if (newInstance.getRequiredParentEdgeTypes().getMaxCardinality() != 1) {
              throw new Exception("Resources are not allowed to have more than 1 parents");
            }
          }

//          JsonArray requiredProps = newInstance.getConfigSchema().get("required").getAsJsonArray();
//          JsonObject propertiesObject = newInstance.getConfigSchema().get("properties")
//              .getAsJsonObject();
          List<String> requiredPropertyNames = Arrays.asList("region", "owner", "project",
              "environment");
//          for (String propertyName : requiredPropertyNames) {
//            if (!propertiesObject.has(propertyName)) {
//              throw new Exception("Invalid config schema missing required configs");
//            }
//          }
//          Set<String> requiredProperties = new HashSet<>();
//          for (JsonElement jsonElement : requiredProps) {
//            requiredProperties.add(jsonElement.getAsString());
//          }
//          if (!requiredProperties.containsAll(requiredPropertyNames)) {
//            throw new Exception("Required properties not marked correctly in configschema");
//          }

          // build internal schema
          JsonObject internalSchema = newInstance.getConfigSchema().deepCopy();
          JsonObject internalProps = internalSchema.get("properties").getAsJsonObject();
          JsonArray required = internalSchema.get("required").getAsJsonArray();
          for (String name : requiredPropertyNames) {
            required.remove(new JsonPrimitive(name));
            internalProps.remove(name);
          }
          newInstance.setInternalSchema(internalSchema);
          resourceMap.put(canonicalName, newInstance);
        } catch (Exception e) {
          logger.log(Level.SEVERE,
              "Failed to initialize ResourceDefinition for:" + newInstance.getSimpleName(), e);
        }
      }
      populateTagsForResource(tagMap);
    }
  }

  public JsonObject fixConfigSchema(RPCBasedResourceDefinition def) {
    try {
      JsonObject internalSchema = def.getConfigSchema();
      JsonObject schema = new JsonObject();
      JsonObject topLevelSchema = ResourceDefinition.getGlobalSchema();
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
          for (JsonElement i : e.getValue().getAsJsonArray()) {
            if (!required.contains(i)) {
              required.add(i);
            }
          }
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

  public static JsonObject readJsonFile(String filename) throws IOException {
    InputStream systemResourceAsStream = ClassLoader.getSystemResourceAsStream(filename);
    String schemaJson = new String(IOUtils.toCharArray(systemResourceAsStream, "utf-8"));
    JsonObject obj = gson.fromJson(schemaJson, JsonObject.class);
    return obj;
  }

  private void populateTagsForResource(Map<String, Set<String>> tagMap) {
    for (Entry<String, Set<String>> entry : tagMap.entrySet()) {
      resourceTagMap.putIfAbsent(entry.getKey(), new ConcurrentSkipListSet<String>());
      resourceTagMap.get(entry.getKey()).addAll(entry.getValue());
    }
  }

  public ResourceDefinition getResourceDefinition(Resource resource) {
    return getResourceDefinition(resource.getResourceDefinitionClass());
  }

  public ResourceDefinition getResourceDefinition(String resourceClass) {
    ResourceDefinition resourceDefinition = resourceMap.get(resourceClass);
    if (resourceDefinition == null) {
      throw new IllegalArgumentException("No such resource:" + resourceClass);
    }
    return resourceDefinition;
  }

  public Map<String, ResourceDefinition> getResourceMap() {
    return resourceMap;
  }

  public Map<String, Set<String>> getResourceTypeMap() {
    return resourceTagMap;
  }

  public void backfillResource(AbstractResourceDB resourceDB,
                               ResourceDefinition resourceDefinition,
                               boolean skipIngestion) {
    try {
      Iterator<List<Resource>> allBackfillResources = resourceDefinition
          .getAllBackfillResources(resourceDB);
      if (allBackfillResources != null) {
        while (allBackfillResources.hasNext()) {
          List<Resource> next = allBackfillResources.next();
          if (next != null && !next.isEmpty()) {
            if (!skipIngestion) {
//              System.out.println(next.stream().map(r->r.getId()).collect(Collectors.toList()));
              resourceDB.updateResources(next);
            } else {
              // System.out.println(next);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void updateReference(ResourceFactory instance2) {
    INSTANCE = instance2;
  }

}
