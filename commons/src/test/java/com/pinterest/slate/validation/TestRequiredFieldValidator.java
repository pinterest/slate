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

import static org.junit.Assert.fail;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.Test;

import com.pinterest.slate.resources.Resource;

public class TestRequiredFieldValidator {

  @Test
  public void testProjectName() throws Exception {
    RequiredFieldValidator val = new RequiredFieldValidator();
    val.init(new PropertiesConfiguration());
    Resource resource = new Resource();
    resource.setEnvironment("prod");
    resource.setOwner("test");
    resource.setProject("slate");
    resource.setRegion("us-east-1");
    resource.setResourceDefinitionClass("com.test.TestDef");
    val.validate(resource);

    resource.setProject("Slate");
    try {
      val.validate(resource);
      fail("Must have thrown exception");
    } catch (Exception e) {
    }
  }

}
