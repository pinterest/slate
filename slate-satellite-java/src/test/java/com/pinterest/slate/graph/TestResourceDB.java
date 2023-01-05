package com.pinterest.slate.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.google.gson.Gson;
import com.pinterest.slate.resources.Resource;

public class TestResourceDB extends AbstractResourceDB {

  private static final Gson GSON = new Gson();
  private SortedMap<String, String> resourceMap = new ConcurrentSkipListMap<>();

  @Override
  public List<ResourceSearchResultEntry> searchResourceIdPrefix(String idPrefix) throws IOException {
    List<ResourceSearchResultEntry> entries = new ArrayList<>();
    SortedMap<String, String> subMap = resourceMap.subMap(idPrefix, idPrefix + Character.MAX_VALUE);
    for (Entry<String, String> entry : subMap.entrySet()) {
      Resource r = GSON.fromJson(entry.getValue(), Resource.class);
      ResourceSearchResultEntry e = new ResourceSearchResultEntry(entry.getKey(),
          r.getResourceDefinitionClass());
      entries.add(e);
    }
    return entries;
  }

  @Override
  public void updateResources(Collection<Resource> resources) throws IOException {
    for (Resource resource : resources) {
      updateResource(resource);
    }
  }

  @Override
  public void updateResource(Resource resource) throws IOException {
    resourceMap.put(resource.getId(), GSON.toJson(resource));
  }

  @Override
  public Resource getResourceById(String id) throws IOException {
    String v = resourceMap.get(id);
    if (v == null) {
      return null;
    }
    return GSON.fromJson(v, Resource.class);
  }

  @Override
  public List<Resource> getResourcesById(List<String> ids,
                                         boolean errorOnMissing) throws IOException {
    List<Resource> list = new ArrayList<>();
    for (String id : ids) {
      String v = resourceMap.get(id);
      if (v == null && errorOnMissing) {
        throw new IOException("No such element");
      }
      list.add(GSON.fromJson(v, Resource.class));
    }
    return list;
  }

  @Override
  public Map<String, Resource> getResourceByIdAsMap(Collection<String> ids) throws IOException {
    Map<String, Resource> map = new HashMap<>();
    for (String id : ids) {
      String v = resourceMap.get(id);
      if (v != null) {
        map.put(id, GSON.fromJson(v, Resource.class));
      }
    }
    return map;
  }

  public SortedMap<String, String> getResourceMap() {
    return resourceMap;
  }

  @Override
  public Map<String, Long> getLastUpdateTimestamp(List<String> ids) throws IOException {
    Map<String, Long> map = new HashMap<>();
    for (String id : ids) {
      String v = resourceMap.get(id);
      if (v != null) {
        map.put(id, GSON.fromJson(v, Resource.class).getLastUpdateTimestamp());
      }
    }
    return map;
  }

  @Override
  public void deleteResource(Resource resource) throws IOException {
    // TODO Auto-generated method stub
    
  }

}
