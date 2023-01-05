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

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.reflect.TypeToken;
import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.utils.DaemonThreadFactory;
import com.pinterest.slate.utils.HttpUtils;

public class TaskFactory {

  private static final Logger logger = Logger.getLogger(TaskFactory.class.getName());
  public static TaskFactory INSTANCE = new TaskFactory();
  protected Map<String, TaskDefinition> taskRegistry = new ConcurrentHashMap<>();
  private static final String API_TASKS = "/api/v1/tasks";
  protected TaskSystem ts;
  private SlateConfig config;

  protected TaskFactory() {
  }

  public void init(TaskSystem ts, SlateConfig config) throws Exception {
    this.ts = ts;
    this.config = config;
    Reflections reflection = new Reflections("com.pinterest");
    Set<Class<? extends TaskDefinition>> definitions = reflection
        .getSubTypesOf(TaskDefinition.class);
    for (Class<? extends TaskDefinition> def : definitions) {
      if (Modifier.isAbstract(def.getModifiers())) {
        // skip abstract classes
        continue;
      }
      if (def == RPCTaskDefinition.class) {
        continue;
      }
      TaskDefinition instance = def.newInstance();
      try {
        instance.init(ts);
        taskRegistry.put(instance.getTaskDefinitionId(), instance);
        logger.info("Loading local task definition:" + instance.getTaskDefinitionId());
      } catch (Exception e) {
        logger.log(Level.SEVERE,
            "Failed to initialize task definition for:" + def.getCanonicalName(), e);
      }
    }
    Executors.newScheduledThreadPool(1, new DaemonThreadFactory())
        .scheduleAtFixedRate(() -> loadTaskDefinitionsFromSatellites(), 0, 20, TimeUnit.SECONDS);
  }

  public void loadTaskDefinitionsFromSatellites() {
    if (config == null || config.getSatelliteServerUrls() == null) {
      return;
    }
    Type taskMapType = new TypeToken<Set<String>>() {
    }.getType();
    for (String taskServerUrl : config.getSatelliteServerUrls()) {
      try {
        Set<String> set = HttpUtils.makeHttpGet(taskServerUrl + API_TASKS + "/definitions",
            taskMapType);
        for (String entry : set) {
          if (taskRegistry.containsKey(entry)) {
            // ignore existing definitions
            continue;
          }
          RPCTaskDefinition value = new RPCTaskDefinition(taskServerUrl + API_TASKS + "/" + entry, entry);
          taskRegistry.put(entry, value);
          logger.info("Loading remote task definition:" + entry);
        }
      } catch (Exception e) {
        continue;
      }

    }
  }

  public static TaskFactory getInstance() {
    return INSTANCE;
  }

  @VisibleForTesting
  public void registerTask(TaskDefinition taskDefinition) {
    taskRegistry.put(taskDefinition.getTaskDefinitionId(), taskDefinition);
  }

  public TaskDefinition getTask(String taskId) {
    return taskRegistry.get(taskId);
  }

  public Map<String, TaskDefinition> getTaskregistry() {
    return taskRegistry;
  }

}