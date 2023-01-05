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
package com.pinterest.slate.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang3.time.DateUtils;

import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.graph.ExecutionGraph;
import com.pinterest.slate.graph.GraphEngine;
import com.pinterest.slate.graph.GraphExecutionRuntime;
import com.pinterest.slate.graph.PlanVertex;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.security.AuthorizationFilter;

import io.dropwizard.hibernate.UnitOfWork;

@Path("/v2/graphs")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class GraphEngineApi {

  private GraphEngine engine;
  private GraphExecutionRuntime runtime;
  private SlateConfig config;

  public GraphEngineApi(SlateConfig config, GraphEngine engine, GraphExecutionRuntime runtime) {
    this.config = config;
    this.engine = engine;
    this.runtime = runtime;
  }

  @Path("/{executionId}")
  @GET
  public ExecutionGraph getExecutionGraph(@PathParam("executionId") String executionId) {
    try {
      return runtime.get(executionId);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new InternalServerErrorException(e);
    }
  }

  @Path("/plan")
  @POST
  public Map<String, PlanVertex> planChange(@Context SecurityContext sc,
                                            Map<String, Resource> deltaGraph) throws Exception {
    if (deltaGraph == null || deltaGraph.isEmpty()) {
      throw new NotAcceptableException("Empty graph updates cannot be accepted");
    }
    String requester = SlateMgmtApi.getUser(sc);
    try {
      return engine.planGraphUpdate(requester, deltaGraph);
    } catch (PlanException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @Path("/execute")
  @POST
  public ExecutionGraph executeChange(@Context SecurityContext sc,
                                      Map<String, Resource> deltaGraph) throws Exception {
    if (deltaGraph == null || deltaGraph.isEmpty()) {
      throw new NotAcceptableException("Empty graph updates cannot be accepted");
    }
    String requester = SlateMgmtApi.getUser(sc);
    try {
      return engine.executeGraphUpate(requester, deltaGraph);
    } catch (PlanException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  @Path("/my/active")
  @UnitOfWork
  @GET
  public List<ExecutionGraph> getMyActiveGraphs(@Context SecurityContext sc) {
    String requester = SlateMgmtApi.getUser(sc);
    try {
      return runtime.listActiveExecutionsForRequester(requester);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new InternalServerErrorException(e);
    }
  }

  @Path("/my/recent")
  @UnitOfWork
  @GET
  public List<ExecutionGraph> getMyRecentGraphs(@Context SecurityContext sc) {
    String requester = SlateMgmtApi.getUser(sc);
    try {
      return runtime.listRecentExecutionsForRequester(requester);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw new InternalServerErrorException(e);
    }
  }

  @RolesAllowed({ AuthorizationFilter.ADMIN_ROLE_NAME })
  @Path("/all")
  @UnitOfWork
  @GET
  public List<ExecutionGraph> getAllGraphs(@Context SecurityContext sc,
                                           @QueryParam("from") String fromStr,
                                           @QueryParam("to") String toStr,
                                           @QueryParam("status") @DefaultValue("RUNNING,NOT_STARTED") String statusStr,
                                           @QueryParam("page") @DefaultValue("0") String page) {
    String requester = SlateMgmtApi.getUser(sc);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      if (fromStr == null) {
        fromStr = sdf.format(DateUtils.addDays(new Date(), -7));
      }
      if (toStr == null) {
        toStr = sdf.format(new Date());
      }
      Date from = sdf.parse(fromStr);
      from.setHours(0);
      from.setMinutes(0);
      Date to = sdf.parse(toStr);
      to.setHours(23);
      to.setMinutes(59);
      to.setSeconds(59);

      List<Status> status = new ArrayList<>();
      for (String s : statusStr.split(",")) {
        status.add(Status.valueOf(s));
      }
      try {
        return runtime.listAllExecutionsBetween(from, to, status, Integer.parseInt(page));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        throw new InternalServerErrorException(e);
      }
    } catch (ParseException | NumberFormatException e1) {
      throw new BadRequestException("Invalid query filter from / to");
    }
  }

}
