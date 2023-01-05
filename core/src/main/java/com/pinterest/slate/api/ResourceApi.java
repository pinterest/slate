package com.pinterest.slate.api;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.graph.AbstractStateStore;
import com.pinterest.slate.graph.ResourceSearchResultEntry;
import com.pinterest.slate.resources.MetricsDefinition;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceDefinition;
import com.pinterest.slate.resources.ResourceFactory;
import com.pinterest.slate.resources.Tool;
import com.pinterest.slate.security.AuthorizationFilter;

import io.dropwizard.hibernate.UnitOfWork;
import jersey.repackaged.com.google.common.collect.ImmutableList;

@Path("/v2/resources")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class ResourceApi {

  private AbstractResourceDB resourceDB;
  private AbstractStateStore stateStore;

  public ResourceApi(AbstractResourceDB resourceDB, AbstractStateStore stateStore) {
    this.resourceDB = resourceDB;
    this.stateStore = stateStore;
  }

  @Path("/definitions")
  @GET
  public ResourceFactory getResourceFactory() throws InstantiationException, IllegalAccessException,
                                              ClassNotFoundException {
    return ResourceFactory.INSTANCE;
  }

  public Collection<ResourceDefinitionStats> getResourceDefinitionStatsForAuthor(@Context SecurityContext sc,
                                                                                 @HeaderParam("x-forwarded-groups") String groupsString) {
    return ImmutableList.of();
  }

  @Path("/mydefinitions")
  @GET
  public Collection<ResourceDefinition> getResourceDefinitionsForAuthor(@Context SecurityContext sc,
                                                                        @HeaderParam("x-forwarded-groups") String groupsString) {
    Set<String> groups = new HashSet<String>(TaskApi.getGroups(groupsString));
    if (groups.isEmpty()) {
      return ResourceFactory.INSTANCE.getResourceMap().values();
    }
    return ResourceFactory.INSTANCE.getResourceMap().values().stream()
        .filter(r -> groups.contains(r.getAuthor())).collect(Collectors.toList());
  }

  @Path("/search")
  @GET
  @UnitOfWork
  public List<ResourceSearchResultEntry> searchResourceId(@NotNull @QueryParam("idPrefix") String idPrefix) {
    try {
      return resourceDB.searchResourceIdPrefix(idPrefix);
    } catch (IOException e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/fullsearch")
  @GET
  @UnitOfWork
  public List<ResourceSearchResultEntry> searchResourceId(@QueryParam("resourceDefinitionClass") String resourceDefinitionClass,
                                                          @QueryParam("project") String project,
                                                          @QueryParam("owner") String owner,
                                                          @QueryParam("idContent") String idContent,
                                                          @QueryParam("pageNo") @DefaultValue("0") String pageNo,
                                                          @QueryParam("pageSize") @DefaultValue("100") String pageSize) {
    try {
      return resourceDB.searchResource(resourceDefinitionClass, project, owner, idContent,
          Integer.parseInt(pageNo), Integer.parseInt(pageSize));
    } catch (IOException e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/fullsearchCount")
  @GET
  @UnitOfWork
  public int searchResourceCount(@QueryParam("resourceDefinitionClass") String resourceDefinitionClass,
                                                          @QueryParam("project") String project,
                                                          @QueryParam("owner") String owner,
                                                          @QueryParam("idContent") String idContent) {
    try {
      return resourceDB.searchResourceListCount(resourceDefinitionClass, project, owner, idContent);
    } catch (IOException e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }
  
  @Path("/allprojects")
  @GET
  @UnitOfWork
  public List<String> getAllProjects() {
    try {
      return resourceDB.getAllProjects();
    } catch (Exception e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/allowners")
  @GET
  @UnitOfWork
  public List<String> getAllOwners() {
    try {
      return resourceDB.getAllOwners();
    } catch (Exception e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/tools/{id}")
  @GET
  public Collection<Tool> getToolsForResource(@Context SecurityContext sc,
                                              @HeaderParam("x-forwarded-groups") String groupsString,
                                              @PathParam("id") String id) {
    if (!id.startsWith("prn:")) {
      return Collections.emptyList();
    }
    try {
      Resource resource = resourceDB.getResourceById(id);
      if (resource == null) {
        throw new NotFoundException("Resource not found");
      } else {
        return ResourceFactory.INSTANCE.getResourceDefinition(resource).getTools(resource);
      }
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  @Path("/metrics/{id}")
  @GET
  public Collection<MetricsDefinition> getMetricsForResource(@Context SecurityContext sc,
                                                             @HeaderParam("x-forwarded-groups") String groupsString,
                                                             @PathParam("id") String id) {
    if (!id.startsWith("prn:")) {
      return Collections.emptyList();
    }
    try {
      Resource resource = resourceDB.getResourceById(id);
      if (resource == null) {
        throw new NotFoundException("Resource not found");
      } else {
        List<MetricsDefinition> metrics = ResourceFactory.INSTANCE.getResourceDefinition(resource)
            .getMetrics(resource);
        return metrics;
      }
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  @POST
  public void testStorage(Resource resource) throws IOException {
    resourceDB.updateResource(resource);
  }

  @Path("/{id}")
  @GET
  @UnitOfWork
  public Resource getResource(@PathParam("id") String id) {
    try {
      return resourceDB.getResourceById(id);
    } catch (Exception e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/{id}/traverse")
  @GET
  @UnitOfWork
  public List<Resource> traverseResourceGraph(@PathParam("id") String id,
                                              @QueryParam("depth") @DefaultValue("0") int depth) {
    try {
      return resourceDB.traverseGraph(id, depth);
    } catch (IOException e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @RolesAllowed({ AuthorizationFilter.ADMIN_ROLE_NAME })
  @Path("/{id}/unlock")
  @PUT
  @UnitOfWork
  public void unlockResource(@PathParam("id") String id) {
    try {
      resourceDB.unlockResource(id);
    } catch (IOException e) {
      // TODO log exception
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/{id}/updatetimestamps")
  @GET
  @UnitOfWork
  public List<Long> getVersionsForResource(@PathParam("id") String resourceId) {
    try {
      return stateStore.listResourceUpdateTimestamps(resourceId);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/{id}/updatetimestamps/{updatetimestamp}")
  @GET
  @UnitOfWork
  public Resource getResourceVersion(@PathParam("id") String resourceId,
                                     @PathParam("updatetimestamp") Long updateTimestamp) {
    try {
      return stateStore.getResourceUpdate(resourceId, updateTimestamp);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new InternalServerErrorException();
    }
  }

  @Path("/lastupdatetimestamps")
  @POST
  @UnitOfWork
  public Map<String, Long> getLastUpdateTimestamp(List<String> resourceIds) {
    try {
      return resourceDB.getLastUpdateTimestamp(resourceIds);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
