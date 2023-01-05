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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.persistence.AttributeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ResourceEdgeAttributeCSVConverter implements AttributeConverter<Map<String, Set<String>>, String> {

  private static final Gson GSON = new Gson();

  @Override
  public String convertToDatabaseColumn(Map<String, Set<String>> attribute) {
    return GSON.toJson(attribute);
  }

  @Override
  public Map<String, Set<String>> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return new HashMap<>();
    }
    JsonObject fromJson = GSON.fromJson(dbData, JsonObject.class);
    Map<String, Set<String>> res = new HashMap<>();
    for (Entry<String, JsonElement> e : fromJson.entrySet()) {
      JsonArray ary = e.getValue().getAsJsonArray();
      Set<String> s = new HashSet<>(ary.size());
      for (JsonElement e2 : ary) {
        s.add(e2.getAsString());
      }
      res.put(e.getKey(), s);
    }
    return res;
  }

}
