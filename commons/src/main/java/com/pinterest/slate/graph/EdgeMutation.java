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
package com.pinterest.slate.graph;

public class EdgeMutation {

  public static final String DELIMETER = "___";
  private String srcFieldName;
  private String dstFieldName;
  private String srcId;
  private String destId;
  private boolean add;
  private boolean parentChild;

  public EdgeMutation(String srcId, String destId, boolean add) {
    this.srcId = srcId;
    this.destId = destId;
    this.add = add;
  }

  public EdgeMutation(String srcId, String destId, boolean add, boolean parentChild) {
    super();
    this.srcId = srcId;
    this.destId = destId;
    this.add = add;
    this.parentChild = parentChild;
  }

  public boolean isAdd() {
    return add;
  }

  public void setAdd(boolean add) {
    this.add = add;
  }

  public String getSrcFieldName() {
    return srcFieldName;
  }

  public void setSrcFieldName(String srcFieldName) {
    this.srcFieldName = srcFieldName;
  }

  public String getDstFieldName() {
    return dstFieldName;
  }

  public void setDstFieldName(String dstFieldName) {
    this.dstFieldName = dstFieldName;
  }

  public String getSrcId() {
    return srcId;
  }

  public void setSrcId(String srcId) {
    this.srcId = srcId;
  }

  public String getDestId() {
    return destId;
  }

  public void setDestId(String destId) {
    this.destId = destId;
  }

  public String getId() {
    return srcId + DELIMETER + destId;
  }

  public boolean isParentChild() {
    return parentChild;
  }

  public void setParentChild(boolean parentChild) {
    this.parentChild = parentChild;
  }

  @Override
  public String toString() {
    return "EdgeMutation [srcFieldName=" + srcFieldName + ", dstFieldName=" + dstFieldName
        + ", srcId=" + srcId + ", destId=" + destId + ", add=" + add + ", parentChild="
        + parentChild + "]";
  }
}
