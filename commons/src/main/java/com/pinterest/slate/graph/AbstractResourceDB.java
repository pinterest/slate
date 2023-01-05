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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;

import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.resources.Resource;

public abstract class AbstractResourceDB {

  public void init(SlateConfig configuration) throws Exception {
  }

  public void lockResources(String owner, List<String> resourceIds) throws Exception {
    Map<String, Resource> resources = getResourceByIdAsMap(resourceIds);
    for (String id : resourceIds) {
      Resource r = resources.get(id);
      if (r == null) {
        String proposedResourceLockOwner = getProposedResourceLockOwner(id);
        if (proposedResourceLockOwner != null) {
          throw new PlanException(
              "ProposedResource(" + id + ") is already locked by " + proposedResourceLockOwner);
        }
        lockProposedResource(owner, id);
        continue;
      }
      if (r.getResourceLockOwner() != null) {
        throw new PlanException(
            "Resource(" + r.getId() + ") is already locked by " + r.getResourceLockOwner());
      }
      r.setResourceLockOwner(owner);
    }
    updateResources(resources.values());
  }

  public String getProposedResourceLockOwner(String id) {
    return null;
  }

  public void lockProposedResource(String owner, String resourceId) throws IOException {
  }

  public void unlockProposedResource(String resourceId) throws IOException {
  }

//  public void lockResource(String owner, String id) throws Exception {
//    Resource r = getResourceById(id);
//    if (r.getResourceLockOwner() != null) {
//      throw new PlanException(
//          "Resource(" + r.getId() + ") is already locked by " + r.getResourceLockOwner());
//    }
//    r.setResourceLockOwner(owner);
//    updateResource(r);
//  }

  public void unlockResource(String id) throws IOException {
    Resource c = getResourceById(id);
    if (c != null) {
      // unlock resource if resource exists
      c.setResourceLockOwner(null);
      updateResource(c);
    } else {
      try {
        unlockProposedResource(id);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void unlockResources(Collection<String> ids) throws IOException {
    Map<String, Resource> resources = getResourceByIdAsMap(ids);
    for (String id : ids) {
      Resource c = resources.get(id);
      if (c != null) {
        // unlock resource if resource exists
        c.setResourceLockOwner(null);
      } else {
        try {
          unlockProposedResource(id);
        } catch (Exception e) {
        }
      }
    }
    updateResources(resources.values());
  }

  public abstract List<ResourceSearchResultEntry> searchResourceIdPrefix(String idPrefix) throws IOException;

  public List<Resource> traverseGraph(String resourceId, int depth) throws IOException {
    if (depth == -1) {
      // short circuit if there is nothing more to traverse
      return Collections.emptyList();
    }
    List<Resource> graph = new ArrayList<>();
    Resource resource = getResourceById(resourceId);
    if (resource == null) {
      throw new NotFoundException();
    }
    List<String> connections = resource.getNonNullConnections();
    for (String id : connections) {
      graph.add(getResourceById(id));
      graph.addAll(traverseGraph(resource.getId(), depth - 1));
    }
    return graph;
  }

  public abstract void updateResources(Collection<Resource> resources) throws IOException;

  public abstract void updateResource(Resource resource) throws IOException;

  public abstract void deleteResource(Resource resource) throws IOException;

  /**
   * Get Resource object for the supplied id.
   * 
   * @param id resourceId
   * @return return null if doesn't exist
   * @throws IOException if there is a communication issue with the database
   */
  public abstract Resource getResourceById(String id) throws IOException;

  /**
   * Get Resource objects for all the ids supplied.
   * 
   * @param ids            of resources
   * @param errorOnMissing
   * @return list of resource objects
   * @throws IOException if there is a communication issue with the database, if
   *                     errorOnMissing is enabled then throw exception of missing
   *                     id as well
   */
  public abstract List<Resource> getResourcesById(List<String> ids,
                                                  boolean errorOnMissing) throws IOException;

  /**
   * Get Resource objects for all the ids supplied, for Ids not the map shouldn't
   * contain an entry.
   * 
   * @param ids
   * @return
   * @throws IOException
   */
  public abstract Map<String, Resource> getResourceByIdAsMap(Collection<String> ids) throws IOException;

  /**
   * Get Resource lastupdatetimestamp to check resource version
   * 
   * @param ids
   * @return
   * @throws IOException
   */
  public abstract Map<String, Long> getLastUpdateTimestamp(List<String> ids) throws IOException;

  public List<ResourceSearchResultEntry> searchResource(String resourceDefinitionClass,
                                                        String project,
                                                        String owner,
                                                        String idContent,
                                                        int pageNo,
                                                        int pageSize) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  public List<String> getAllProjects() {
    // TODO Auto-generated method stub
    return null;
  }

  public List<String> getAllOwners() {
    // TODO Auto-generated method stub
    return null;
  }

  public int searchResourceListCount(String resourceDefinitionClass,
                                     String project,
                                     String owner,
                                     String idContent) throws IOException {
    // TODO Auto-generated method stub
    return 0;
  }
}
