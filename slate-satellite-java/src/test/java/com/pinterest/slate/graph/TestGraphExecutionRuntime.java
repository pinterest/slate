package com.pinterest.slate.graph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import com.google.gson.Gson;
import com.pinterest.slate.graph.storage.LocalExecutionQueue;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.satellite.LocalTaskRuntime;

public class TestGraphExecutionRuntime extends GraphExecutionRuntime {

  private static final class TestExecutionDAO implements AbstractExecutionDAO {
    private final Gson GSON = new Gson();
    private Map<String, String> db = new ConcurrentHashMap<>();

    @Override
    public void save(ExecutionGraph graph) throws IOException {
      db.put(graph.getExecutionId(), GSON.toJson(graph));
    }

    @Override
    public List<ExecutionGraph> listRecentExecutionsForRequester(String requester) throws IOException {
      return null;
    }

    @Override
    public List<String> listAllIncompleteExecutionGraphIds() throws IOException {
      List<String> l = new ArrayList<>();
      for (Entry<String, String> entry : db.entrySet()) {
        ExecutionGraph eg = GSON.fromJson(entry.getValue(), ExecutionGraph.class);
        if (!Status.isComplete(eg.getStatus())) {
          l.add(eg.getExecutionId());
        }
      }
      return l;
    }

    @Override
    public List<ExecutionGraph> listAllExecutionsGraphsBetween(Date from,
                                                         Date to,
                                                         List<Status> status,
                                                         int page) throws IOException {
      List<ExecutionGraph> l = new ArrayList<>();
      for (Entry<String, String> entry : db.entrySet()) {
        ExecutionGraph eg = GSON.fromJson(entry.getValue(), ExecutionGraph.class);
        if (eg.getStartTime().after(from) && eg.getStartTime().before(to)
            && status.contains(eg.getStatus())) {
          l.add(eg);
        }
      }
      return l;
    }

    @Override
    public List<ExecutionGraph> listActiveExecutionsForRequester(String requester) throws IOException {
      List<ExecutionGraph> l = new ArrayList<>();
      for (Entry<String, String> entry : db.entrySet()) {
        ExecutionGraph eg = GSON.fromJson(entry.getValue(), ExecutionGraph.class);
        if (eg.getRequester().equals(requester)) {
          l.add(eg);
        }
      }
      return l;
    }

    @Override
    public ExecutionGraph get(String executionId) throws IOException {
      return GSON.fromJson(db.get(executionId), ExecutionGraph.class);
    }
  }

  public TestGraphExecutionRuntime(AbstractStateStore store, AbstractResourceDB resourceDB) {
    super(new TestExecutionDAO());
    setAuditSink(new AbstractGraphAuditSink() {

      @Override
      public void audit(ExecutionGraph executionGraph) throws Exception {
      }
    });
    setResourceDB(resourceDB);
    setExecutionQueue(new LocalExecutionQueue());
    setTaskRuntime(new LocalTaskRuntime());
  }

  public static SessionFactory buildSessionFactory() {
    Configuration configuration = new Configuration();
    configuration.addAnnotatedClass(ExecutionGraph.class);
    configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
    configuration.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
    configuration.setProperty("hibernate.connection.url", "jdbc:h2:mem");
    configuration.setProperty("hibernate.hbm2ddl.auto", "create");
    SessionFactory sessionFactory = configuration.buildSessionFactory();
    return sessionFactory;
  }

}
