package com.pinterest.slate.satellite.api;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;
import com.pinterest.slate.satellite.LocalTaskFactory;

@Path("/v1/tasks/")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class TaskApi {

  private TaskRuntime runtime;

  public TaskApi(TaskRuntime runtime) {
    this.runtime = runtime;
  }

  @Path("/definitions")
  @GET
  public Set<String> getTaskDefinitions() {
    Set<String> taskDefinitionSet = new HashSet<String>();
    for (Entry<String, TaskDefinition> entry : LocalTaskFactory.INSTANCE.getTaskRegistry()
        .entrySet()) {
      taskDefinitionSet.add(entry.getValue().getTaskDefinitionId());
    }
    return taskDefinitionSet;
  }

  @Path("/{taskDefinitionId}/{taskInstanceId}/execution")
  @POST
  public StatusUpdate startExecution(@PathParam("taskDefinitionId") String taskDefinitionId,
                                     @PathParam("taskInstanceId") String taskInstanceId,
                                     LifecycleProcess process) {
    return runtime.startExecution(taskDefinitionId, taskInstanceId, process);
  }

  @Path("/{taskDefinitionId}/{taskInstanceId}/status")
  @POST
  public StatusUpdate checkStatus(@PathParam("taskDefinitionId") String taskDefinitionId,
                                  @PathParam("taskInstanceId") String taskInstanceId,
                                  LifecycleProcess process) {
    return runtime.checkStatus(taskDefinitionId, taskInstanceId, process);
  }

  @Path("/{taskDefinitionId}/{taskInstanceId}/validation")
  @POST
  public void validation(@PathParam("taskDefinitionId") String taskDefinitionId,
                         @PathParam("taskInstanceId") String taskInstanceId,
                         LifecycleProcess process) {
    try {
      LocalTaskFactory.INSTANCE.getTask(taskDefinitionId).validate(taskInstanceId, process,
          process.getProcessContext(),
          process.getProcessContext().has(taskInstanceId)
              ? process.getProcessContext().get(taskInstanceId).getAsJsonObject()
              : null);
    } catch (Exception e) {
      e.printStackTrace();
      throw new BadRequestException(e.getMessage());
    }
  }

}
