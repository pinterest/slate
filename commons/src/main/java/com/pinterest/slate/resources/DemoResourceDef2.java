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
public class DemoResourceDef2 implements ResourceDefinition {

  private Map<String, EdgeDefinition> requiredInboundEdgeTypes = ImmutableMap.of("i1",
      new EdgeDefinition(DemoResourceDef.class.getCanonicalName(), 0, 1));

  private Map<String, EdgeDefinition> requiredOutboundEdgeTypes = ImmutableMap.of("o1",
      new EdgeDefinition(DemoResourceDef.class.getCanonicalName(), 0, 1), "o2",
      new EdgeDefinition(DemoResourceDef.class.getCanonicalName(), 0, 1));

  private EdgeDefinition requiredParentEdgeTypes = null;

  private String simpleName = "Demo2";
  private String author = "Slate Team";
  private JsonObject configSchema;
  private JsonObject uiSchema;
  private String chatLink = "";

  public DemoResourceDef2() {
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
      pR.setId("demo_resource_" + System.currentTimeMillis());
    }
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
          "title": "Demo2",
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
  public EdgeDefinition getRequiredParentEdgeTypes() {
    return requiredParentEdgeTypes;
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
    return new Resource(id, DemoResourceDef2.class.getCanonicalName(), 5, 4);
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
