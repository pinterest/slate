/**
 * Copyright 2023 Pinterest, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy ofapache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.slate.resources;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = "proposedresource")
@NamedQueries({})
public class ProposedResource implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  private String id;
  @Column(name = "resource_lock_owner")
  private String resourceLockOwner;
  
  public ProposedResource() {
  }

  public ProposedResource(String id, String resourceLockOwner) {
    this.id = id;
    this.resourceLockOwner = resourceLockOwner;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getResourceLockOwner() {
    return resourceLockOwner;
  }

  public void setResourceLockOwner(String resourceLockOwner) {
    this.resourceLockOwner = resourceLockOwner;
  }

}
