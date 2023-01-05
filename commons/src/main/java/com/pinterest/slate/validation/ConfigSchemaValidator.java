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
package com.pinterest.slate.validation;

import org.everit.json.schema.Schema;
import org.everit.json.schema.Validator;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceDefinition;
import com.pinterest.slate.resources.ResourceFactory;

public class ConfigSchemaValidator implements ResourceValidator {

  @Override
  public void validate(Resource resource) throws ValidationException {
    ResourceDefinition resourceDefinition = ResourceFactory.INSTANCE
        .getResourceDefinition(resource.getResourceDefinitionClass());
    if (resourceDefinition == null) {
      throw new ValidationException(
          "No such resource definition:" + resource.getResourceDefinitionClass());
    }
    JsonObject configSchema = resourceDefinition.getInternalSchema();
    JSONObject schemaJson = new JSONObject(configSchema.toString());
    Schema schema = SchemaLoader.load(schemaJson);
    Validator validator = Validator.builder().failEarly().build();
    String desiredStateJSON = resource.getDesiredState().toString();
    try {
      validator.performValidation(schema, new JSONObject(desiredStateJSON));
    } catch (Exception e) {
      throw new ValidationException("Schema validation failed for: " + resource.getId() + " class:"
          + resource.getResourceDefinitionClass() + " msg:" + e.getMessage(), e);
    }
  }

}