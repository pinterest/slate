package com.pinterest.slate.graph.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.graph.ResourceSearchResultEntry;
import com.pinterest.slate.resources.ProposedResource;
import com.pinterest.slate.resources.Resource;

import io.dropwizard.hibernate.UnitOfWork;

public class RDBMSResourceDB extends AbstractResourceDB {

  private RDBMSResourceDAO dao;
  private ProposedResourceDAO proposedResourceDAO;

  public RDBMSResourceDB(RDBMSResourceDAO dao, ProposedResourceDAO proposedResourceDAO) {
    this.dao = dao;
    this.proposedResourceDAO = proposedResourceDAO;
  }

  @UnitOfWork
  @Override
  public void updateResources(Collection<Resource> resources) throws IOException {
    for (Resource c : resources) {
      if (c != null) {
        updateResource(c);
      }
    }
  }

  @UnitOfWork
  @Override
  public String getProposedResourceLockOwner(String resourceId) {
    ProposedResource resourceById = null;
    try {
      resourceById = proposedResourceDAO.getResourceById(resourceId);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (resourceById == null) {
      return null;
    }
    return resourceById.getResourceLockOwner();
  }

  @UnitOfWork
  @Override
  public void lockProposedResource(String owner, String resourceId) throws IOException {
    proposedResourceDAO.saveResource(new ProposedResource(resourceId, owner));
  }

  @UnitOfWork
  @Override
  public void unlockProposedResource(String resourceId) throws IOException {
    ProposedResource resourceById = proposedResourceDAO.getResourceById(resourceId);
    proposedResourceDAO.deleteResource(resourceById);
  }

  @UnitOfWork
  @Override
  public void updateResource(Resource resource) throws IOException {
    dao.saveResource(resource);
  }

  @UnitOfWork
  @Override
  public Resource getResourceById(String id) throws IOException {
    return dao.getResourceById(id);
  }

  @UnitOfWork
  @Override
  public List<Resource> getResourcesById(List<String> ids,
                                         boolean errorOnMissing) throws IOException {
    List<Resource> result = new ArrayList<>(ids.size());
    for (String id : ids) {
      try {
        Resource c = getResourceById(id);
        if (c != null) {
          result.add(c);
        }
      } catch (Exception e) {
        if (errorOnMissing) {
          throw new IOException("Missing resource:" + id);
        }
      }
    }
    return result;
  }

  @UnitOfWork
  @Override
  public List<ResourceSearchResultEntry> searchResourceIdPrefix(String idPrefix) throws IOException {
    return dao.getResourceIdsLike(idPrefix);
  }
  

  @UnitOfWork
  @Override
  public int searchResourceListCount(String resourceDefinitionClass,
                                                        String project,
                                                        String owner,
                                                        String idContent) throws IOException {
    return dao.searchResourceListCount(resourceDefinitionClass, project, owner, idContent);
  }
  
  @UnitOfWork
  @Override
  public List<ResourceSearchResultEntry> searchResource(String resourceDefinitionClass,
                                                        String project,
                                                        String owner,
                                                        String idContent,
                                                        int pageNo,
                                                        int pageSize) throws IOException {
    return dao.searchResourceList(resourceDefinitionClass, project, owner, idContent, pageNo,
        pageSize);
  }

  @UnitOfWork
  @Override
  public List<String> getAllProjects() {
    return dao.getAllProjects();
  }

  @UnitOfWork
  @Override
  public List<String> getAllOwners() {
    return dao.getAllOwners();
  }

  public List<String> getAllEnvironments() {
    return ImmutableList.of("prod", "control", "canary", "integ", "dev");
  }

  @UnitOfWork
  @Override
  public Map<String, Resource> getResourceByIdAsMap(Collection<String> ids) throws IOException {
    Map<String, Resource> map = new HashMap<>(ids.size());
    for (String id : ids) {
      try {
        Resource r = getResourceById(id);
        if (r != null) {
          map.put(id, r);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return map;
  }

  @UnitOfWork
  @Override
  public Map<String, Long> getLastUpdateTimestamp(List<String> ids) throws IOException {
    return dao.getLastUpdateTimestamp(ids);
  }

  @Override
  public void deleteResource(Resource resource) throws IOException {
    dao.deleteResource(resource);
  }
}
