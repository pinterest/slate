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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.AttributeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ResourceSetAttributeCSVConverter implements AttributeConverter<Set<String>, String> {

  private static final Gson GSON = new Gson();

  @Override
  public String convertToDatabaseColumn(Set<String> attribute) {
    return GSON.toJson(attribute);
  }

  @Override
  public Set<String> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return new HashSet<>();
    }
    JsonArray ary = GSON.fromJson(dbData, JsonArray.class);
    Set<String> res = new HashSet<String>(ary.size());
    for (JsonElement e2 : ary) {
      res.add(e2.getAsString());
    }
    return res;
  }

}
