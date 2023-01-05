package com.pinterest.slate.satellite;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;

import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskFactory;

public class LocalTaskFactory extends TaskFactory {

  private static final Logger logger = Logger.getLogger(LocalTaskFactory.class.getName());
  public static final LocalTaskFactory INSTANCE = new LocalTaskFactory();

  private LocalTaskFactory() {
  }

  public void init(TaskSystem ts) throws Exception {
    this.ts = ts;
    Set<Class<? extends TaskDefinition>> definitions = null;
    if (ts.getTaskDefinitionFQCN() != null) {
      definitions = new HashSet<Class<? extends TaskDefinition>>();
      for (String string : ts.getTaskDefinitionFQCN()) {
        definitions.add(Class.forName(string).asSubclass(TaskDefinition.class));
      }
    } else {
      Reflections reflection = new Reflections("com.pinterest");
      definitions = reflection.getSubTypesOf(TaskDefinition.class);
      System.out.println(definitions);
    }
    for (Class<? extends TaskDefinition> def : definitions) {
      if (Modifier.isAbstract(def.getModifiers())) {
        // skip abstract classes
        continue;
      }
      if (def == com.pinterest.slate.process.RPCTaskDefinition.class) {
        continue;
      }
      try {
        TaskDefinition instance = def.newInstance();
        instance.init(ts);
        taskRegistry.put(instance.getTaskDefinitionId(), instance);
      } catch (Exception e) {
        logger.log(Level.SEVERE,
            "Failed to initialize task definition for:" + def.getCanonicalName(), e);
      }
    }
  }

  public static LocalTaskFactory getInstance() {
    return INSTANCE;
  }

  public void registerTask(TaskDefinition taskDefinition) {
    taskRegistry.put(taskDefinition.getTaskDefinitionId(), taskDefinition);
  }

  public TaskDefinition getTask(String taskId) {
    return taskRegistry.get(taskId);
  }

  public Map<String, TaskDefinition> getTaskRegistry() {
    return taskRegistry;
  }

}