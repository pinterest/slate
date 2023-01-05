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

import java.util.regex.Pattern;

import org.apache.commons.configuration2.Configuration;

import com.google.common.collect.ImmutableSet;
import com.pinterest.slate.resources.Resource;

public class RequiredFieldValidator implements ResourceValidator {

  private ImmutableSet<String> regions;
  private static final Pattern PROJECT_REGEX = Pattern.compile("[a-z0-9\\@\\_\\-]+");

  @Override
  public void init(Configuration config) throws Exception {
    regions = ImmutableSet
        .copyOf(config.getString("validregions", "us-east-1,eu-west-1").toLowerCase().split(","));
  }

  @Override
  public void validate(Resource resource) throws ValidationException {
    if (resource.getOwner() == null) {
      throw new ValidationException("Resource must have an owner");
    }
    if (resource.getRegion() == null) {
      throw new ValidationException("Resource must have a region");
    }
    if (!regions.contains(resource.getRegion().toLowerCase())) {
      throw new ValidationException("Resource must have a region from:" + regions);
    }
    if (resource.getProject() == null) {
      throw new ValidationException("Resource must have a project");
    }
    if (!PROJECT_REGEX.matcher(resource.getProject()).matches()) {
      throw new ValidationException(
          "Project name must be lower case and match " + PROJECT_REGEX.pattern());
    }
    if (resource.getEnvironment() == null) {
      throw new ValidationException("Resource must have an environment");
    }
    if (resource.getResourceDefinitionClass() == null) {
      throw new ValidationException("Resource must have a resource definition class");
    }
  }

}
