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
package com.pinterest.slate.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * Describes an optional action rendered by the Slate application shell.
 *
 * Behavior can be attached by an external integration through CSS classes and
 * data attributes. The webapp allowlists data-* and aria-* attributes before
 * adding them to the DOM.
 */
public class UiAction {

  @NotNull
  private String id;

  @NotNull
  private String label;

  private String icon;

  private String title;

  private List<String> classNames = Collections.emptyList();

  private Map<String, String> attributes = Collections.emptyMap();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public List<String> getClassNames() {
    return classNames;
  }

  public void setClassNames(List<String> classNames) {
    this.classNames = classNames == null ? Collections.emptyList() : classNames;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes == null ? Collections.emptyMap() : attributes;
  }
}
