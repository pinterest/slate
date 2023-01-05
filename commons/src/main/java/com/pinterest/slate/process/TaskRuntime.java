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
package com.pinterest.slate.process;

import org.apache.commons.configuration2.Configuration;

public interface TaskRuntime {

  void configure(Configuration configuration) throws Exception;

  StatusUpdate startExecution(String taskDefinitionId,
                                        String taskInstanceId,
                                        LifecycleProcess workflow);

  StatusUpdate checkStatus(String taskTemplateId,
                           String instanceId,
                           LifecycleProcess workflow);
  
}
