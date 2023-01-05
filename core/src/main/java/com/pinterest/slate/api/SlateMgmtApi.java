package com.pinterest.slate.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
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
import com.pinterest.slate.graph.GraphExecutionRuntime;
import com.pinterest.slate.graph.PlanVertex;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.resources.ResourceFactory;
import com.pinterest.slate.security.AuthorizationFilter;

import io.dropwizard.hibernate.UnitOfWork;
import jersey.repackaged.com.google.common.collect.Sets;

@Path("/v2/mgmt")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class SlateMgmtApi {

  private SlateConfig config;
  private GraphExecutionRuntime runtime;

  public SlateMgmtApi(SlateConfig config, GraphExecutionRuntime runtime) {
    this.config = config;
    this.runtime = runtime;
  }

  @Path("/isadmin")
  @GET
  public boolean isUserAdmin(@Context SecurityContext sc,
                             @HeaderParam("x-forwarded-groups") String groupsString) {
    String user = getUser(sc);
    Set<String> groups = TaskApi.getGroups(groupsString);
    boolean b = !Sets.intersection(groups, config.getAdminGroups()).isEmpty()
        || config.getAdminUsers().contains(user);
    return b;
  }

  public static String getUser(SecurityContext sc) {
    return sc.getUserPrincipal() != null ? sc.getUserPrincipal().getName() : "dev";
  }

  @RolesAllowed({ AuthorizationFilter.ADMIN_ROLE_NAME })
  @Path("/{resourcedefinitionclass}/backfill")
  @UnitOfWork
  @GET
  public void triggerResourceBackfil(@Context SecurityContext sc,
                                     @PathParam("resourcedefinitionclass") String resourceDefinitionClass) {
    // prevent multiple resource backfills from executing simultaneously
    try {
      synchronized (ResourceFactory.class) {
        ResourceFactory.INSTANCE.backfillResource(runtime.getResourceDB(),
            ResourceFactory.INSTANCE.getResourceDefinition(resourceDefinitionClass), false);
      }
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(
          "Resource definition:" + resourceDefinitionClass + " doesn't exist");
    }
  }

  @RolesAllowed({ AuthorizationFilter.ADMIN_ROLE_NAME })
  @Path("/toptimetakenprocesses")
  @UnitOfWork
  @GET
  public PriorityQueue<LifecycleProcess> getTopNTimeTakenProcesses(@Context SecurityContext sc,
                                                                   @QueryParam("from") String fromStr,
                                                                   @QueryParam("to") String toStr,
                                                                   @QueryParam("status") @DefaultValue("CANCELLED,SUCCEEDED,FAILED") String statusStr,
                                                                   @QueryParam("n") @DefaultValue("10") String topN) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      if (fromStr == null) {
        fromStr = sdf.format(DateUtils.addDays(new Date(), -30));
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
      int pageNum = 0;
      int queueSize = Integer.parseInt(topN);
      PriorityQueue<LifecycleProcess> topNProcessQueue = new PriorityQueue<>(queueSize,
          new Comparator<LifecycleProcess>() {
            @Override
            public int compare(LifecycleProcess p1, LifecycleProcess p2) {
              long p1TotalTime = p1.getEndTimeMs() - p1.getStartTimeMs();
              long p2TotalTime = p2.getEndTimeMs() - p2.getStartTimeMs();
              return Long.compare(p1TotalTime, p2TotalTime);
            }
          });
      try {
        while (true) {
          List<ExecutionGraph> executions = runtime.listAllExecutionsBetween(from, to, status,
              pageNum);
          if (executions.size() < 1) {
            // end of pagination
            break;
          }
          for (ExecutionGraph execution : executions) {
            ExecutionGraph graph = runtime.get(execution.getExecutionId());
            Map<String, PlanVertex> plan = graph.getExecutionPlan();
            for (PlanVertex pv : plan.values()) {
              LifecycleProcess process = pv.getProcess();
              if (topNProcessQueue.size() < queueSize) {
                topNProcessQueue.add(process);
              } else {
                // check with top element time.
                LifecycleProcess topProcess = topNProcessQueue.peek();
                long queueTopTime = topProcess.getEndTimeMs() - topProcess.getStartTimeMs();
                long currentTime = process.getEndTimeMs() - process.getStartTimeMs();
                if (currentTime > queueTopTime) {
                  topNProcessQueue.poll();
                  topNProcessQueue.add(process);
                }
              }
            }
          }
          pageNum++;
        }
        return topNProcessQueue;
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
