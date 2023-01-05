package com.pinterest.slate.graph.storage;

import org.hibernate.SessionFactory;

import com.pinterest.slate.resources.ProposedResource;

import io.dropwizard.hibernate.AbstractDAO;

public class ProposedResourceDAO extends AbstractDAO<ProposedResource> {

  public ProposedResourceDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }
  
  public ProposedResource getResourceById(String id) {
    return get(id);
  }
  
  public void deleteResource(ProposedResource resource) {
    currentSession().delete(resource);
  }
  
  public String saveResource(ProposedResource resource) {
    return persist(resource).getId();
  }

}
