package com.pinterest.slate.satellite;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.http.ParseException;

import com.pinterest.slate.human.HumanTask;
import com.pinterest.slate.human.HumanTaskSystem;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.utils.HttpUtils;

public class RPCHTS extends HumanTaskSystem {

  private static final String HTS_URL = "/api/v2/hts";
  private String slateCoreUrl;

  public RPCHTS() {
    super(null);
  }

  public void init(SatelliteServerConfig config) {
    slateCoreUrl = config.getSlateCoreUrl();
  }

  @Override
  public HumanTask create(HumanTask task) throws IOException {
    try {
      return HttpUtils.makeHttpPut(slateCoreUrl + HTS_URL, task, HumanTask.class);
    } catch (ParseException | IOException | PlanException e) {
      e.printStackTrace();
      throw new IOException(e);
    }
  }

  @Override
  public HumanTask getTask(String processId, String taskId) throws IOException {
    return HttpUtils.makeHttpGet(slateCoreUrl + HTS_URL + "/" + processId + "/" + taskId,
        HumanTask.class);
  }

  @Override
  public void updateAssignee(String processId,
                             String taskId,
                             String assigneeUser) throws IOException {
    try {
      HttpUtils.makeHttpPut(
          slateCoreUrl + HTS_URL + "/" + processId + "/" + taskId + "/assignee/" + assigneeUser,
          null, Void.class);
    } catch (ParseException | IOException | PlanException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void updateStatus(String processId,
                           String taskId,
                           Status status,
                           String comment) throws IOException {
    try {
      HttpUtils.makeHttpPut(
          slateCoreUrl + HTS_URL + "/" + processId + "/" + taskId + "/status/" + status, comment,
          Void.class);
    } catch (ParseException | IOException | PlanException e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<HumanTask> listPendingTasksForAssigneeGroup(Set<String> groups) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<HumanTask> listPendingTasksForAssigneeUser(String user) {
    throw new UnsupportedOperationException();
  }

}
