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
package com.pinterest.slate.process;

import com.tananaev.jsonpatch.JsonPatch;

public class StatusUpdate {

  private Status status;
  private String stdOut;
  private String stdErr;
  private JsonPatch processContextUpdate;

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getStdOut() {
    return stdOut;
  }

  public void setStdOut(String stdOut) {
    this.stdOut = stdOut;
  }

  public String getStdErr() {
    return stdErr;
  }

  public void setStdErr(String stdErr) {
    this.stdErr = stdErr;
  }

  public JsonPatch getProcessContextUpdate() {
    return processContextUpdate;
  }

  public void setProcessContextUpdate(JsonPatch processContextUpdate) {
    this.processContextUpdate = processContextUpdate;
  }

  public static StatusUpdate create(Status status) {
    StatusUpdate statusUpdate = new StatusUpdate();
    statusUpdate.setStatus(status);
    return statusUpdate;
  }

  public static StatusUpdate create(Status status, String msg) {
    StatusUpdate statusUpdate = new StatusUpdate();
    statusUpdate.setStatus(status);
    statusUpdate.setStdErr(msg);
    return statusUpdate;
  }

  public static StatusUpdate create(Status status, JsonPatch processContextUpdate) {
    StatusUpdate statusUpdate = new StatusUpdate();
    statusUpdate.setStatus(status);
    statusUpdate.setProcessContextUpdate(processContextUpdate);
    return statusUpdate;
  }

  public static StatusUpdate create(Status status, String msg, JsonPatch processContextUpdate) {
    StatusUpdate statusUpdate = new StatusUpdate();
    statusUpdate.setStatus(status);
    statusUpdate.setStdErr(msg);
    statusUpdate.setProcessContextUpdate(processContextUpdate);
    return statusUpdate;
  }

  public static StatusUpdate create(Status status, String msg, Exception e) {
    StatusUpdate statusUpdate = new StatusUpdate();
    statusUpdate.setStatus(status);
    if (e != null) {
      statusUpdate.setStdErr(msg + ". Reason:" + e.getMessage());
    }else {
      statusUpdate.setStdErr(msg+". Reason: missing exception");
    }
    return statusUpdate;
  }

  @Override
  public String toString() {
    return "StatusUpdate [status=" + status + ", stdOut=" + stdOut + ", stdErr=" + stdErr
        + ", processContextUpdate=" + processContextUpdate + "]";
  }
}