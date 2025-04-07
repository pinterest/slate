package com.pinterest.slate.api;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.human.HumanTask;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.Status;

import io.dropwizard.hibernate.UnitOfWork;

@Path("/v2/tasks")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class TaskApi {

  private static final Logger logger = Logger.getLogger(TaskApi.class.getCanonicalName());
  private TaskSystem ts;

  public TaskApi(SlateConfig config, TaskSystem ts) {
    this.ts = ts;
  }

  @GET
  @Path("/mytasks")
  @UnitOfWork
  public List<HumanTask> listMyAssignedTasks(@Context SecurityContext sc) {
    String user = SlateMgmtApi.getUser(sc);
    return ts.getHumanTaskSystem().listPendingTasksForAssigneeUser(user);
  }

  @GET
  @Path("/mygrouptasks")
  @UnitOfWork
  public List<HumanTask> listMyGroupsAssignedTasks(@Context SecurityContext sc,
                                                   @HeaderParam("x-forwarded-groups") String groupsString) {
    String user = SlateMgmtApi.getUser(sc);
    Set<String> groups = getGroups(groupsString);
    if (!groups.isEmpty()) {
      List<HumanTask> listPendingTasksForAssigneeGroup = ts.getHumanTaskSystem().listPendingTasksForAssigneeGroup(groups);
      return listPendingTasksForAssigneeGroup;
    } else {
      return ImmutableList.of();
    }
  }

  @PUT
  @Path("/assign/{processid}/{taskid}")
  @UnitOfWork
  public void assignGroupTaskToMe(@Context SecurityContext sc,
                                  @HeaderParam("x-forwarded-groups") String groupsString,
                                  @PathParam("processid") String processid,
                                  @PathParam("taskid") String taskId) {
    String user = SlateMgmtApi.getUser(sc);
    Set<String> groups = getGroups(groupsString);
    HumanTask task;
    try {
      task = ts.getHumanTaskSystem().getTask(processid, taskId);
      if (containsIgnoreCase(groups, task.getAssigneeGroupName())) {
        // check if this user is allowed to take on this task
        ts.getHumanTaskSystem().updateAssignee(processid, taskId, user);
      }
    } catch (IOException e) {
      throw new InternalServerErrorException();
    }
  }

  @PUT
  @Path("/{processid}/{taskid}/{status}")
  @UnitOfWork
  public void updateTaskStatus(@Context SecurityContext sc,
                               @HeaderParam("x-forwarded-groups") String groupsString,
                               @PathParam("processid") String processId,
                               @PathParam("taskid") String taskId,
                               @PathParam("status") Status status,
                               String comment) {
    String user = SlateMgmtApi.getUser(sc);
    Set<String> groups = getGroups(groupsString);
    try {
      HumanTask task = ts.getHumanTaskSystem().getTask(processId, taskId);
      if (task == null) {
        throw new NotFoundException();
      }
      if (task.getTaskStatus() == Status.SUCCEEDED) {
        // if the request has already been approved short circuit
        return;
      }
      if (containsIgnoreCase(groups, task.getAssigneeGroupName())) {
        // user is allowed to change task status
        switch (task.getTaskType()) {
        case APPROVAL:
        case NON_VERIFIABLE:
          ts.getHumanTaskSystem().updateStatus(processId, taskId, status, comment);
          logger.info("HTS task updated processId:" + processId + " taskId:" + taskId + " status:"
              + status);
          break;
        default:
          // no one is allowed to manually update the task status
          throw new NotAuthorizedException(
              "Task is neither an approval task nor a non-verifyable task, no one is allowed to manually alter the status of this task");
        }
      } else {
        throw new NotAuthorizedException("You are not authorized to approve this request. Groups:"
            + groupsString + " task:" + task.getAssigneeGroupName());
      }
    } catch (IOException e) {
      throw new InternalServerErrorException();
    }
  }

  public static Set<String> getGroups(String groupsString) {
    if (groupsString == null || groupsString.isBlank()) {
      return ImmutableSet.of();
    }
    Set<String> groups = Sets.newHashSet(groupsString.split(","));
    return groups;
  }

  /**
   * Check if a set contains a string, ignoring case.
   * @param set the string set to check
   * @param searchStr the string to search for
   * @return true if the set contains the string, false otherwise
   */
  public static boolean containsIgnoreCase(Set<String> set, String searchStr) {
    if (searchStr == null) {
      return false;
    }
    for (String setStr : set) {
      if (setStr.equalsIgnoreCase(searchStr)) {
        return true;
      }
    }
    return false;
  }
}
