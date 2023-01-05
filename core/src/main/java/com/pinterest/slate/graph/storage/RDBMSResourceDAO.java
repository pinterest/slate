package com.pinterest.slate.graph.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import com.pinterest.slate.graph.AbstractStateStore;
import com.pinterest.slate.graph.ResourceSearchResultEntry;
import com.pinterest.slate.resources.Resource;

import io.dropwizard.hibernate.AbstractDAO;

public class RDBMSResourceDAO extends AbstractDAO<Resource> {

  private static final Logger logger = Logger.getLogger(RDBMSResourceDAO.class.getCanonicalName());
  private AbstractStateStore stateStore;

  public RDBMSResourceDAO(SessionFactory sessionFactory, AbstractStateStore stateStore) {
    super(sessionFactory);
    this.stateStore = stateStore;
  }

  public Resource getResourceById(String id) {
    return get(id);
  }

  public String saveResource(Resource resource) {
    resource.setLastUpdateTimestamp(System.currentTimeMillis());
    // add support for versioning
    try {
      stateStore.saveResourceUpdate(resource);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to store version for resource:" + resource.getId()
          + " version:" + resource.getLastUpdateTimestamp(), e);
    }
    return persist(resource).getId();
  }

  public void deleteResource(Resource resource) {
    currentSession().delete(resource);
  }

  public List<ResourceSearchResultEntry> getResourceIdsLike(String idPrefix) {
    @SuppressWarnings("unchecked")
    List<Object[]> resultList = namedQuery("searchResourcesLike")
        .setParameter("idPrefix", "%" + idPrefix + "%").setMaxResults(10).getResultList();
    List<ResourceSearchResultEntry> results = new ArrayList<>(resultList.size());
    for (Object[] result : resultList) {
      results.add(new ResourceSearchResultEntry((String) result[0], (String) result[1]));
    }
    return results;
  }
  
  public int searchResourceListCount(String resourceDefinitionClass,
                                                            String project,
                                                            String owner,
                                                            String idContent) {
    Query countQuery = namedQuery("searchResourceCount").setParameter("rdc", resourceDefinitionClass)
        .setParameter("project", project).setParameter("owner", owner);
    if (idContent != null) {
      countQuery = countQuery.setParameter("idContents", "%" + idContent + "%");
    } else {
      countQuery = countQuery.setParameter("idContents", null);
    }
    
    long totalResult = (Long)countQuery.getSingleResult();
    return (int)totalResult;
  }

  public List<ResourceSearchResultEntry> searchResourceList(String resourceDefinitionClass,
                                                            String project,
                                                            String owner,
                                                            String idContent,
                                                            int pageNo,
                                                            int pageSize) {
    System.out.println(resourceDefinitionClass + " " + project + " " + owner + " " + idContent);
    Query query = namedQuery("searchResource").setParameter("rdc", resourceDefinitionClass)
        .setParameter("project", project).setParameter("owner", owner);
    Query countQuery = namedQuery("searchResourceCount").setParameter("rdc", resourceDefinitionClass)
        .setParameter("project", project).setParameter("owner", owner);
    if (idContent != null) {
      query = query.setParameter("idContents", "%" + idContent + "%");
      countQuery = countQuery.setParameter("idContents", "%" + idContent + "%");
    } else {
      query = query.setParameter("idContents", null);
      countQuery = countQuery.setParameter("idContents", null);
    }

    if (pageSize == 0) {
      pageSize = 100;
    }

    query = query.setMaxResults(pageSize);
    query.setFirstResult(pageNo * pageSize);
    List<Object[]> resultList = query.getResultList();
    List<ResourceSearchResultEntry> results = new ArrayList<>(resultList.size());
    for (Object[] result : resultList) {
      results.add(new ResourceSearchResultEntry((String) result[0], (String) result[1]));
    }
    return results;
  }

  public Map<String, Long> getLastUpdateTimestamp(List<String> resourceIds) {
    Map<String, Long> results = new HashMap<>();
    @SuppressWarnings("unchecked")
    List<Object[]> resultList = namedQuery("resourceTimestamp").setParameter("ids", resourceIds)
        .getResultList();
    for (Object[] name : resultList) {
      results.put((String) name[0], (Long) name[1]);
    }
    return results;
  }

  public List<String> getAllOwners() {
    return namedQuery("allOwners").getResultList();
  }
  
  public List<String> getAllProjects() {
    return namedQuery("allProjects").getResultList();
  }
}
