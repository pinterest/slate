package com.pinterest.slate.satellite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.graph.ResourceSearchResultEntry;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.utils.HttpUtils;

/**
 * Supports getting resources from core. 
 * 
 * NOTE: updates and deletes are NOT supported
 * 
 * @author ambudsharma
 */
public class RPCResourceDB extends AbstractResourceDB {

  private static final String BASE_API = "/api/v2/";
  private String slateCoreUrl;

  public RPCResourceDB() {
  }

  @Override
  public void init(SlateConfig configuration) throws Exception {
  }

  public void init(SatelliteServerConfig configuration) throws Exception {
    slateCoreUrl = configuration.getSlateCoreUrl();
  }

  @Override
  public List<ResourceSearchResultEntry> searchResourceIdPrefix(String idPrefix) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateResources(Collection<Resource> resources) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateResource(Resource resource) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Resource getResourceById(String id) throws IOException {
    return HttpUtils.makeHttpGet(slateCoreUrl + BASE_API + "resources/" + id, Resource.class);
  }

  @Override
  public List<Resource> getResourcesById(List<String> ids,
                                         boolean errorOnMissing) throws IOException {
    List<Resource> response = new ArrayList<Resource>();
    for (String id : ids) {
      Resource resource = getResourceById(id);
      if (resource == null && errorOnMissing) {
        throw new IOException("Missing resource:" + id);
      }
      response.add(resource);
    }
    return response;
  }

  @Override
  public Map<String, Resource> getResourceByIdAsMap(Collection<String> ids) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Long> getLastUpdateTimestamp(List<String> ids) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteResource(Resource resource) throws IOException {
    // NOT IMPLEMENTED / ALLOWED
  }

}
