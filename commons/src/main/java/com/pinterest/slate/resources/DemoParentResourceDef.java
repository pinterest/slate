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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Task;

@IgnoreRD
public class DemoParentResourceDef implements ResourceDefinition {

  private Map<String, EdgeDefinition> requiredInboundEdgeTypes = ImmutableMap.of();

  private Map<String, EdgeDefinition> requiredOutboundEdgeTypes = ImmutableMap.of();

  private Set<EdgeDefinition> requiredChildEdgeTypes = ImmutableSet
      .of(new EdgeDefinition(DemoResourceDef.class.getCanonicalName(), 1, 10));

  private String simpleName = "DemoParent";
  private String author = "Slate Team";
  private JsonObject configSchema;
  private JsonObject uiSchema;
  private String chatLink = "";

  public DemoParentResourceDef() {
    configSchema = ResourceDefinition.super.getConfigSchema();
    uiSchema = ResourceDefinition.super.getUiSchemaInternal();
  }

  @Override
  public Plan planChange(ResourceChange change) throws PlanException {
    LifecycleProcess process = new LifecycleProcess();
    process.setProcessContext(new JsonObject());
    process.setStartTaskId(Task.SUCCEED_PROCESS_TASK);
    Resource pR = change.getProposedResourceObject();
    if (pR.getId() == null) {
      pR.setId("demo_parent_resource_" + System.currentTimeMillis());
    }
    System.out.println("Parent resource planning");
    return Plan.of(pR, process, null);
  }

  @Override
  public String getChatLink() {
    return chatLink;
  }

  @Override
  public JsonObject getInternalSchema() {
    return GSON.fromJson("""
        {
          "type": "object",
          "title": "DemoParent",
          "properties": {}
        }
        """, JsonObject.class);
  }

  @Override
  public Map<String, EdgeDefinition> getRequiredOutboundEdgeTypes() {
    return requiredOutboundEdgeTypes;
  }

  public Map<String, EdgeDefinition> getRequiredInboundEdgeTypes() {
    return requiredInboundEdgeTypes;
  }

  @Override
  public Set<EdgeDefinition> getRequiredChildEdgeTypes() {
    return requiredChildEdgeTypes;
  }

  @Override
  public String getAuthor() {
    return author;
  }

  @Override
  public Set<String> getTags() {
    return ImmutableSet.of("Misc");
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  public JsonObject getConfigSchema() {
    return configSchema;
  }

  public void setConfigSchema(JsonObject configSchema) {
    this.configSchema = configSchema;
  }

  public JsonObject getUiSchema() {
    return uiSchema;
  }

  public void setUiSchema(JsonObject uiSchema) {
    this.uiSchema = uiSchema;
  }

  @Override
  public Resource newInstance(String id) {
    return new Resource(id, DemoParentResourceDef.class.getCanonicalName(), 5, 4);
  }

  @Override
  public String getShortDescription() {
    return "Simple do nothing demo Resource to test the Slate UI";
  }

  @Override
  public String getDocumentationLink() {
    return null;
  }

  @Override
  public String getDocumentation() {
    return null;
  }

}
