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

import java.io.Serializable;

public class Tool implements Serializable {
  
  private static final long serialVersionUID = 1L;
  private String label;
  private String url;
  private String description;
  private boolean embed;
  
  public Tool() {
  }

  public Tool(String label, String url, String description, boolean embed) {
    this.label = label;
    this.url = url;
    this.description = description;
    this.embed = embed;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isEmbed() {
    return embed;
  }

  public void setEmbed(boolean embed) {
    this.embed = embed;
  }

  @Override
  public String toString() {
    return "Tool [label=" + label + ", url=" + url + ", description=" + description + ", embed="
        + embed + "]";
  }

}
