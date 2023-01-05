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
package com.pinterest.slate.graph;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.taskdefinitions.SlackTask;

import io.dropwizard.hibernate.UnitOfWork;

public class GraphExecutor implements Runnable {
  
  public static final String BASE_URL = System.getProperty("CORE_URL");
  private static Logger logger = Logger.getLogger(GraphExecutor.class.getCanonicalName());
  private GraphExecutionRuntime runtime;
  private boolean enableExecution = true;

  public GraphExecutor(GraphExecutionRuntime runtime) throws IOException {
    this.runtime = runtime;
    String getenv = System.getenv("ENABLE_EXECUTION");
    if (getenv != null) {
      enableExecution = Boolean.parseBoolean(getenv);
      logger.info("Graph execution enabled is :" + enableExecution);
    }
    runtime.loadQueueIfSupported();
  }

  @UnitOfWork
  @Override
  public void run() {
    while (enableExecution) {
      execute();
    }
  }

  @VisibleForTesting
  public void execute() {
    try {
      String executionId = runtime.getExecutionQueue().take();
      if (executionId != null) {
        ExecutionGraph executionGraph = runtime.get(executionId);
        if (executionGraph == null) {
          logger.severe("Graph:" + executionId + " is missing");
          SlackTask.sendSlackMessage("Error: Graph execution id is missing",
              "Error: Graph execution id is missing:" + executionId, "", "ambudsharma");
          return;
        }
        try {
          executionGraph.continueExecution(runtime);
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (!Status.isComplete(executionGraph.getStatus())) {
          runtime.getExecutionQueue().add(executionId);
        } else {
          // unlock all resources
          runtime.getExecutionQueue().delete(executionId);
          runtime.getResourceDB().unlockResources(executionGraph.getExecutionPlan().keySet());
          // set end time
          executionGraph.setEndTime(new Date());
          logger.info("Graph execution completed:" + executionGraph.getExecutionId()
              + " with status:" + executionGraph.getStatus());
          if (runtime.getAuditSink() != null) {
            // send an audit event
            runtime.getAuditSink().audit(executionGraph);
          }
          // notify requester that graph execution is now complete
          SlackTask.sendSlackMessage(
              "Task execution has completed with status:" + executionGraph.getStatus(), "",
              BASE_URL + "/executions/" + executionId,
              executionGraph.getRequester());
        }
        // update state
        runtime.update(executionGraph);
      } else {
        // add wait if the execution id was null indicating there is no data in the
        // queue
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      // sleep for 1s if there are no graphs to execute to reduce CPU usage
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @UnitOfWork
  public void executeGraph(ExecutionGraph executionGraph) throws IOException {
    // set start time to the current time
    executionGraph.setStartTime(new Date());
    runtime.update(executionGraph);
    runtime.getExecutionQueue().add(executionGraph.getExecutionId());
  }

  public GraphExecutionRuntime getRuntime() {
    return runtime;
  }

}
