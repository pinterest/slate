package com.pinterest.slate.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.pinterest.slate.human.AbstractHumanTaskDAO;
import com.pinterest.slate.human.HumanTask;
import com.pinterest.slate.human.HumanTaskId;
import com.pinterest.slate.process.Status;

public final class TestHumanTaskDAO implements AbstractHumanTaskDAO {

  private Map<String, String> db = new ConcurrentHashMap<>();

  private HumanTask getHumanTask(String key) {
    return TestUtils.GSON.fromJson(db.get(key), HumanTask.class);
  }
  
  @Override
  public List<HumanTask> listPendingTasksForAssigneeUser(final String user) {
    return query(ht -> ht.getAssigneeUser().equals(user));
  }

  private List<HumanTask> query(Predicate<HumanTask> filter) {
    List<HumanTask> l = new ArrayList<>();
    for (String k : db.keySet()) {
      HumanTask ht = getHumanTask(k);
      if (filter.test(ht)) {
        l.add(ht);
      }
    }
    return l;
  }

  @Override
  public List<HumanTask> listPendingTasksForAssigneeGroup(Set<String> groups) {
    return query(ht -> groups.contains(ht.getAssigneeGroupName()));
  }

  @Override
  public HumanTask get(HumanTaskId htid) {
    return getHumanTask(htid.getProcessId() + "_" + htid.getTaskId());
  }

  @Override
  public HumanTask persist(HumanTask task) {
    task.setTaskStatus(Status.SUCCEEDED);
    db.put(task.getProcessId() + "_" + task.getTaskId(), TestUtils.GSON.toJson(task));
    return task;
  }
}