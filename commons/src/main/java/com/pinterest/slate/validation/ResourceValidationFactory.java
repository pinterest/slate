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

import java.io.FileReader;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;

public class ResourceValidationFactory {

  private static final ResourceValidationFactory INSTANCE = new ResourceValidationFactory();
  private Map<String, ResourceValidator> validatorMap = new ConcurrentHashMap<>();

  private ResourceValidationFactory() {
  }

  public static ResourceValidationFactory getInstance() {
    return INSTANCE;
  }

  public void init(String configPath) throws Exception {
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.read(new FileReader(configPath));
    String[] validators = config.getString("validators").split(",");
    for (String name : validators) {
      try {
        Configuration subset = config.subset(name);
        String className = subset.getString("class");
        ResourceValidator valInstance = Class.forName(className).asSubclass(ResourceValidator.class)
            .newInstance();
        valInstance.init(subset);
        validatorMap.put(name, valInstance);
      } catch (Exception e) {
        throw new Exception("Failed to configure validator:" + name, e);
      }
    }
  }

  public Collection<ResourceValidator> getValidators() {
    return validatorMap.values();
  }

}
