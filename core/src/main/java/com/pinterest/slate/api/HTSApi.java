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
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.pinterest.slate.human.HumanTask;
import com.pinterest.slate.human.HumanTaskSystem;
import com.pinterest.slate.process.Status;

@Path("/v2/hts")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class HTSApi {

  private static final Logger logger = Logger.getLogger(HTSApi.class.getCanonicalName());
  private HumanTaskSystem hts;

  public HTSApi(HumanTaskSystem hts) {
    this.hts = hts;
  }

  @Path("/{processId}/{taskId}")
  @GET
  public HumanTask getTask(@PathParam("processId") String processId,
                           @PathParam("taskId") String taskId) {
    try {
      return hts.getTask(processId, taskId);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  @PUT
  public HumanTask createTask(HumanTask task) {
    try {
      HumanTask createdTask = hts.create(task);
      return createdTask;
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  @Path("/{processId}/{taskId}/status/{status}")
  @PUT
  public void updateStatus(@PathParam("processId") String processId,
                           @PathParam("taskId") String taskId,
                           @PathParam("status") String status,
                           String comment) {
    try {
      hts.updateStatus(processId, taskId, Status.valueOf(status), comment);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }

  @Path("/{processId}/{taskId}/assginee/{assignee}")
  @PUT
  public void updateAssignee(@PathParam("processId") String processId,
                             @PathParam("taskId") String taskId,
                             @PathParam("assignee") String assignee) {
    try {
      hts.updateAssignee(processId, taskId, assignee);
    } catch (IOException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
