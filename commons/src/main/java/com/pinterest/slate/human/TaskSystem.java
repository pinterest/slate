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
package com.pinterest.slate.human;

import java.util.List;

public class TaskSystem {
  
  private HumanTaskSystem hts;
  private String configDirectory;
  private String tmpDirectory;
  private boolean dev;
  private List<String> taskDefinitionFQCN;
  
  public TaskSystem(boolean dev, String configDirectory, String tmpDirectory, HumanTaskSystem hts, List<String> taskDefinitionFQCN) {
    this.dev = dev;
    this.configDirectory = configDirectory;
    this.tmpDirectory = tmpDirectory;
    this.hts = hts;
    this.taskDefinitionFQCN = taskDefinitionFQCN;
  }
  
  public List<String> getTaskDefinitionFQCN() {
    return taskDefinitionFQCN;
  }
  
  public HumanTaskSystem getHumanTaskSystem() {
    return hts;
  }

  public String getConfigDirectory() {
    return configDirectory;
  }
  
  public String getTmpDirectory() {
    return tmpDirectory;
  }

  public boolean isDev() {
    return dev;
  }

  public void setDev(boolean dev) {
    this.dev = dev;
  }

}
