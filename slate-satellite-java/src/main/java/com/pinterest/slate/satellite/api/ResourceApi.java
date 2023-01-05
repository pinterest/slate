package com.pinterest.slate.satellite.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.JsonObject;
import com.pinterest.slate.graph.AbstractResourceDB;
import com.pinterest.slate.resources.MetricsDefinition;
import com.pinterest.slate.resources.Plan;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceChange;
import com.pinterest.slate.resources.ResourceDefinition;
import com.pinterest.slate.resources.Tool;
import com.pinterest.slate.satellite.LocalResourceFactory;

@Path("/v1/resources/")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class ResourceApi {

  private AbstractResourceDB resourceDB;
  private Map<String, Iterator<List<Resource>>> backfillIterators;

  public ResourceApi(AbstractResourceDB resourceDB) {
    this.resourceDB = resourceDB;
    backfillIterators = new ConcurrentHashMap<>();
  }

  @Path("/definitions")
  @GET
  public Map<String, ResourceDefinition> getResourceDefinitions() {
    return LocalResourceFactory.INSTANCE.getResourceMap();
  }

  @Path("/tags")
  @GET
  public Map<String, Set<String>> getResourceTags() {
    return LocalResourceFactory.INSTANCE.getResourceTagMap();
  }

  @Path("/{resourceDefinitionClass}")
  @POST
  public Plan planChange(@PathParam("resourceDefinitionClass") String resourceDefinitionClass,
                         ResourceChange change) {
    ResourceDefinition resourceDefinition = LocalResourceFactory.INSTANCE
        .getResourceDefinition(resourceDefinitionClass);
    try {
      return resourceDefinition.planChange(change);
    } catch (PlanException e) {
      throw new BadRequestException(
          Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
    }
  }

  @Path("/{resourceDefinitionClass}/currentstate")
  @POST
  public JsonObject getCurrentState(@PathParam("resourceDefinitionClass") String resourceDefinitionClass,
                                    Resource resource) {
    ResourceDefinition resourceDefinition = LocalResourceFactory.INSTANCE
        .getResourceDefinition(resourceDefinitionClass);
    try {
      return resourceDefinition.readExternalCurrentState(resource);
    } catch (Exception e) {
      throw new InternalServerErrorException(
          Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
    }
  }

  @Path("/{resourceDefinitionClass}/backfill")
  @POST
  public List<List<Resource>> getBackfill(@PathParam("resourceDefinitionClass") String resourceDefinitionClass,
                                       @QueryParam("batchSize") int batchSize) {
    batchSize = batchSize == 0 ? 10 : batchSize;
    ResourceDefinition resourceDefinition = LocalResourceFactory.INSTANCE
        .getResourceDefinition(resourceDefinitionClass);
    try {
      Iterator<List<Resource>> iterator = backfillIterators.get(resourceDefinitionClass);
      if (iterator == null) {
        iterator = resourceDefinition.getAllBackfillResources(resourceDB);
        backfillIterators.put(resourceDefinitionClass, iterator);
      }
      if (iterator == null || !iterator.hasNext()) {
        throw new NotFoundException("No backfill information available");
      }
      List<List<Resource>> result = new ArrayList<List<Resource>>();
      for (int i = 0; i < batchSize; i++) {
        if (iterator.hasNext()) {
          result.add(iterator.next());
        } else {
          break;
        }
      }
      return result;
    } catch (Exception e) {
      e.printStackTrace();
      throw new InternalServerErrorException(
          Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
    }
  }

  @Path("/{resourceDefinitionClass}/tools")
  @POST
  public Set<Tool> getTools(@PathParam("resourceDefinitionClass") String resourceDefinitionClass,
                            Resource resource) {
    ResourceDefinition resourceDefinition = LocalResourceFactory.INSTANCE
        .getResourceDefinition(resourceDefinitionClass);
    try {
      return resourceDefinition.getTools(resource);
    } catch (Exception e) {
      throw new InternalServerErrorException(
          Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
    }
  }

  @Path("/{resourceDefinitionClass}/metrics")
  @POST
  public List<MetricsDefinition> getMetrics(@PathParam("resourceDefinitionClass") String resourceDefinitionClass,
                                            Resource resource) {
    ResourceDefinition resourceDefinition = LocalResourceFactory.INSTANCE
        .getResourceDefinition(resourceDefinitionClass);
    try {
      return resourceDefinition.getMetrics(resource);
    } catch (Exception e) {
      throw new InternalServerErrorException(
          Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build());
    }
  }
}
