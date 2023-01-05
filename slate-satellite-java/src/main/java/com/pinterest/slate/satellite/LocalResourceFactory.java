package com.pinterest.slate.satellite;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.reflections.Reflections;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.resources.BackgroundService;
import com.pinterest.slate.resources.EdgeDefinition;
import com.pinterest.slate.resources.IgnoreRD;
import com.pinterest.slate.resources.RPCBasedResourceDefinition;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceDefinition;
import com.pinterest.slate.resources.ResourceFactory;
import com.pinterest.slate.resources.Utils;

public class LocalResourceFactory extends ResourceFactory {

  private static final Logger logger = Logger
      .getLogger(LocalResourceFactory.class.getCanonicalName());
  private static final Gson gson = new Gson();

  public static final LocalResourceFactory INSTANCE = new LocalResourceFactory();
  public static boolean ACTIVATE_IGNORE_RD = true;
  private transient ExecutorService bges = Executors.newCachedThreadPool(new ThreadFactory() {

    @Override
    public Thread newThread(Runnable r) {
      Thread th = new Thread(r);
      th.setDaemon(true);
      return th;
    }
  });

  private LocalResourceFactory() {
  }

  public void init(String configDirectory, AbstractResourceDB graph) throws Exception {
    Reflections reflection = new Reflections("com.pinterest");
    Set<Class<? extends ResourceDefinition>> resources = reflection
        .getSubTypesOf(ResourceDefinition.class);
    logger.info("Found:" + resources);
    for (Class<? extends ResourceDefinition> resource : resources) {
      if (Modifier.isAbstract(resource.getModifiers())) {
        // skip abstract classes
        continue;
      }
      if (resource == RPCBasedResourceDefinition.class) {
        continue;
      }

      if (LocalResourceFactory.ACTIVATE_IGNORE_RD) {
        IgnoreRD[] annotationsByType = resource.getAnnotationsByType(IgnoreRD.class);
        if (annotationsByType.length > 0) {
          System.out.println("Ignore:" + resource.getCanonicalName());
          // skip this resource definition
          continue;
        }
      }
      logger.info("Attempting to initialize:" + resource.getCanonicalName());
      try {
        ResourceDefinition newInstance = resource.newInstance();
        resolveEdgeRuleSubtypes(reflection, newInstance.getRequiredInboundEdgeTypes());
        resolveEdgeRuleSubtypes(reflection, newInstance.getRequiredOutboundEdgeTypes());
        String canonicalName = resource.getCanonicalName();
        JsonArray requiredProps = newInstance.getConfigSchema().get("required").getAsJsonArray();
        JsonObject propertiesObject = newInstance.getConfigSchema().get("properties")
            .getAsJsonObject();
        List<String> requiredPropertyNames = Arrays.asList("region", "owner", "project",
            "environment");
        for (String propertyName : requiredPropertyNames) {
          if (!propertiesObject.has(propertyName)) {
            throw new Exception("Invalid config schema missing required configs");
          }
        }
        Set<String> requiredProperties = new HashSet<>();
        for (JsonElement jsonElement : requiredProps) {
          requiredProperties.add(jsonElement.getAsString());
        }
        if (!requiredProperties.containsAll(requiredPropertyNames)) {
          throw new Exception("Required properties not marked correctly in configschema");
        }

        resourceMap.put(canonicalName, newInstance);
        populateTagsForResource(newInstance, canonicalName);
        try {
          newInstance.init(configDirectory, graph);
          logger.info("Initialized:" + resource.getName());
          BackgroundService srvc = newInstance.getBackgroundService();
          if (srvc != null && !Utils.UNIT_TEST) {
            bges.submit(srvc);
            logger.info("Started background service for " + resource.getName());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      } catch (Exception e) {
        logger.log(Level.SEVERE,
            "Failed to initialize ResourceDefinition for:" + resource.getName(), e);
      }
    }
  }

  private void resolveEdgeRuleSubtypes(Reflections reflection,
                                       Map<String, EdgeDefinition> edgeRules) throws ClassNotFoundException {
    if (edgeRules != null) {
      for (EdgeDefinition edgeDefinition : edgeRules.values()) {
        Set<String> addResolvedRules = new HashSet<>();
        for (String c : edgeDefinition.getConnectedResourceType()) {
          try {
            Set<?> cChild = reflection.getSubTypesOf(Class.forName(c));
            for (Object object : cChild) {
              @SuppressWarnings("unchecked")
              Class<? extends ResourceDefinition> rChild = (Class<? extends ResourceDefinition>) object;
              addResolvedRules.add(rChild.getCanonicalName());
            }
          } catch (Exception e) {
            logger.warning("Failed to resolve:" + c);
          }
        }
        edgeDefinition.getConnectedResourceType().addAll(addResolvedRules);
      }
    }
  }

  public Map<String, Set<String>> getResourceTagMap() {
    return resourceTagMap;
  }

  private void populateTagsForResource(ResourceDefinition newInstance, String canonicalName) {
    for (String tag : newInstance.getTags()) {
      Set<String> set = resourceTagMap.get(tag);
      if (set == null) {
        set = new HashSet<>();
        resourceTagMap.put(tag, set);
      }
      set.add(canonicalName);
    }
  }

  public static JsonObject deepMerge(JsonObject source, JsonObject target) {
    for (Map.Entry<String, JsonElement> sourceEntry : source.entrySet()) {
      String key = sourceEntry.getKey();
      JsonElement value = sourceEntry.getValue();
      if (!target.has(key)) {
        // target does not have the same key, so perhaps it should be added to target
        if (!value.isJsonNull()) // well, only add if the source value is not null
          target.add(key, value);
      } else {
        if (!value.isJsonNull()) {
          if (value.isJsonObject()) {
            // source value is json object, start deep merge
            deepMerge(value.getAsJsonObject(), target.get(key).getAsJsonObject());
          } else {
            target.add(key, value);
          }
        } else {
          target.remove(key);
        }
      }
    }
    return target;
  }

  public void backfillResources(AbstractResourceDB resourceDB) throws Exception {
    for (ResourceDefinition resourceDefinition : resourceMap.values()) {
      try {
        Iterator<List<Resource>> allBackfillResources = resourceDefinition
            .getAllBackfillResources(resourceDB);
        if (allBackfillResources != null) {
          while (allBackfillResources.hasNext()) {
            List<Resource> next = allBackfillResources.next();
            if (next != null) {
              resourceDB.updateResources(next);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

}
