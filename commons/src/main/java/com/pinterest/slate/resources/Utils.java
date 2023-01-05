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

import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.pinterest.slate.graph.PlanVertex;
import com.pinterest.slate.process.LifecycleProcess;

public class Utils {

  public static final String PROP_UNIT_TEST = "unit_test";
  public static final Gson GSON = new Gson();
  public static boolean UNIT_TEST = Boolean
      .parseBoolean(System.getProperties().getProperty(PROP_UNIT_TEST, "false"));

  private Utils() {
  }

  public static String getStringOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsString();
  }

  public static String getStringOrDefault(JsonObject object, String field, String defaultValue) {
    if (!object.has(field)) {
      return defaultValue;
    }
    return object.get(field).getAsString();
  }

  public static boolean getBooleanOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsBoolean();
  }

  public static boolean getBooleanOrDefault(JsonObject object, String field, boolean defaultValue) {
    if (!object.has(field)) {
      return defaultValue;
    }
    return object.get(field).getAsBoolean();
  }

  public static double getDoubleOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsDouble();
  }

  public static double getDoubleOrDefault(JsonObject object, String field, double defaultValue) {
    if (!object.has(field)) {
      return defaultValue;
    }
    return object.get(field).getAsDouble();
  }

  public static int getIntOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsInt();
  }

  public static int getIntOrDefault(JsonObject object, String field, int defaultValue) {
    if (!object.has(field)) {
      return defaultValue;
    }
    return object.get(field).getAsInt();
  }

  public static long getLongOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsLong();
  }

  public static long getLongOrDefault(JsonObject object, String field, long defaultValue) {
    if (!object.has(field)) {
      return defaultValue;
    }
    return object.get(field).getAsLong();
  }

  public static JsonArray getArrayOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsJsonArray();
  }

  public static JsonObject getObject(JsonObject object, String field) {
    if (!object.has(field))
      return null;
    return object.get(field).getAsJsonObject();
  }

  public static JsonObject getObjectOrError(JsonObject object, String field) {
    if (!object.has(field)) {
      throw new NullPointerException("Missing required field:" + field);
    }
    return object.get(field).getAsJsonObject();
  }
  
  public static JsonObject getObjectOrDefault(JsonObject object, String field, JsonObject defaultValue) {
    if (!object.has(field)) {
      return defaultValue;
    }
    return object.get(field).getAsJsonObject();
  }

  public static void diffPlanVertex(PlanVertex v1, PlanVertex v2) {
    if (!Objects.equal(v1.getProcess(), v2.getProcess())) {
      System.out.println("Process is different");
      diffProcess(v1.getProcess(), v2.getProcess());
    }
    if (!v1.getProposedResource().equals(v2.getProposedResource())) {
      System.out.println("Proposed resources are different");
    }
  }

  public static void diffProcess(LifecycleProcess p1, LifecycleProcess p2) {
    if (!p1.getAllTasks().equals(p2.getAllTasks())) {
      System.out.println("Tasks are different:"
          + Sets.difference(p1.getAllTasks().keySet(), p2.getAllTasks().keySet()));
    }
  }

  /**
   * Returns a JSON sub-element from the given JsonElement and the given path
   *
   * @param json - a Gson JsonElement
   * @param path - a JSON path, e.g. a.b.c[2].d
   * @return - a sub-element of json according to the given path
   */
  public static JsonElement getJsonElement(JsonElement json, String path) {
    String[] parts = path.split("\\/|\\[|\\]");
    JsonElement result = json;

    for (String key : parts) {
      key = key.trim();
      if (key.isEmpty()) {
        continue;
      }
      if (result == null) {
        result = JsonNull.INSTANCE;
        break;
      }
      if (result.isJsonObject()) {
        result = ((JsonObject) result).get(key);
      } else if (result.isJsonArray()) {
        int ix = Integer.valueOf(key) - 1;
        result = ((JsonArray) result).get(ix);
      } else {
        break;
      }
    }
    return result;
  }

  public static void setJsonElement(JsonElement json, String path, JsonElement value) {
    String[] parts = path.split("\\/|\\[|\\]");
    JsonElement result = json;
    JsonElement parent = null;
    for (String key : parts) {
      key = key.trim();
      if (key.isEmpty()) {
        continue;
      }
      if (result == null) {
        if (result != null) {
          parent = result;
        }
        result = JsonNull.INSTANCE;
        break;
      }
      if (result.isJsonObject()) {
        if (result != null) {
          parent = result;
        }
        result = ((JsonObject) result).get(key);
      } else if (result.isJsonArray()) {
        int ix = Integer.valueOf(key) - 1;
        if (result != null) {
          parent = result;
        }
        result = ((JsonArray) result).get(ix);
      } else {
        break;
      }
    }
    if (parent != null) {
      if (parent.isJsonArray()) {
        parent.getAsJsonArray().add(value);
      } else if (parent.isJsonObject()) {
        parent.getAsJsonObject().add(parts[parts.length - 1], value);
      }
    }
  }

}
