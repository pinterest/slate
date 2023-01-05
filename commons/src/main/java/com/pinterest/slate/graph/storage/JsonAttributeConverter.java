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
package com.pinterest.slate.graph.storage;

import javax.persistence.AttributeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class JsonAttributeConverter implements AttributeConverter<JsonObject, String> {

  private static final Gson gson = new Gson();

  @Override
  public String convertToDatabaseColumn(JsonObject attribute) {
    return gson.toJson(attribute);
  }

  @Override
  public JsonObject convertToEntityAttribute(String dbData) {
    return gson.fromJson(dbData, JsonObject.class);
  }

}
