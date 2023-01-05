package com.pinterest.slate.graph;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.pinterest.slate.graph.TestUtils.ExecutionObjectBundle;
import com.pinterest.slate.graph.storage.LocalStateStore;
import com.pinterest.slate.human.HumanTaskSystem;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.TaskFactory;
import com.pinterest.slate.resources.Plan;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceChange;
import com.pinterest.slate.resources.ResourceDefinition;
import com.pinterest.slate.resources.ResourceFactory;
import com.pinterest.slate.satellite.LocalResourceFactory;
import com.pinterest.slate.satellite.LocalTaskFactory;
import com.pinterest.slate.validation.ResourceValidationFactory;

public class TestUtils {

  public static final Gson GSON = new Gson();
  public static final Type DELTAGRAPHTYPE = new TypeToken<Map<String, Resource>>() {
  }.getType();

  public static Plan generatePlan(ResourceDefinition def,
                                  String proposed,
                                  String currentStateStr) throws PlanException {
    Map<String, Resource> deltaGraph;
    Resource proposedResourceObject;
    JsonObject currentState = null;
    Resource currentResourceObject = null;
    ResourceChange change;
    deltaGraph = GSON.fromJson(proposed, TestUtils.DELTAGRAPHTYPE);
    proposedResourceObject = deltaGraph.get("newnode_0");

    if (currentStateStr != null) {
      currentState = TestUtils.GSON.fromJson(currentStateStr, JsonObject.class);
      currentResourceObject = def.newInstance("prn:aws:prod:");
      currentResourceObject.setRegion("us-east-1");
      currentResourceObject.setEnvironment("prod");
      currentResourceObject.setDesiredState(currentState);
    }
    change = new ResourceChange("testuser", currentResourceObject, currentState,
        proposedResourceObject, deltaGraph);
    Plan plan = def.planChange(change);
    return plan;
  }
  
  @SafeVarargs
  public static ExecutionObjectBundle primeAndRunGraph(String proposedDeltaGraph,
                                                       String stateStoreDir,
                                                       String existingGraph,
                                                       int maxiterations,
                                                       Function<GraphExecutor, Void>... preRunHooks) throws IOException,
                                                                                                     Exception {
    Map<String, Resource> resourceMap = null;
    if (existingGraph != null) {
      resourceMap = GSON.fromJson(existingGraph, DELTAGRAPHTYPE);
    }
    return primeAndRunGraph(proposedDeltaGraph, stateStoreDir, resourceMap, maxiterations,
        preRunHooks);
  }

  @SafeVarargs
  public static ExecutionObjectBundle primeAndRunGraphWithMap(String proposedDeltaGraph,
                                                              String stateStoreDir,
                                                              Map<String, String> existingGraph,
                                                              int maxiterations,
                                                              Function<GraphExecutor, Void>... preRunHooks) throws IOException,
                                                                                                            Exception {
    Map<String, Resource> resourceMap = new HashMap<>();
    if (existingGraph != null) {
      for (Entry<String, String> entry : existingGraph.entrySet()) {
        resourceMap.put(entry.getKey(), GSON.fromJson(entry.getValue(), Resource.class));
      }
    }
    return primeAndRunGraph(proposedDeltaGraph, stateStoreDir, resourceMap, maxiterations,
        preRunHooks);
  }

  @SafeVarargs
  public static ExecutionObjectBundle primeAndRunGraph(String proposedDeltaGraph,
                                                       String stateStoreDir,
                                                       Map<String, Resource> existingGraph,
                                                       int maxiterations,
                                                       Function<GraphExecutor, Void>... preRunHooks) throws IOException,
                                                                                                     Exception {
    TestResourceDB db = new TestResourceDB();
    if (existingGraph != null) {
      db.updateResources(existingGraph.values());
    }
    GraphEngine.MAX_ITERATIONS = maxiterations;
    LocalResourceFactory.INSTANCE.ACTIVATE_IGNORE_RD = false;
    LocalResourceFactory.INSTANCE.init("src/test/resources/resourceconfigs", db);
    ResourceFactory.INSTANCE.updateReference(LocalResourceFactory.INSTANCE);
    ResourceValidationFactory.getInstance().init("src/test/resources/dev-validation.properties");
    LocalStateStore stateStore = new LocalStateStore();
    Configuration conf = new PropertiesConfiguration();
    conf.addProperty("basegraphstoragepath", stateStoreDir+"/executions");
    conf.addProperty("baseresourceupdatestoragepath", stateStoreDir+"/updates");
    stateStore.init(conf);
    HumanTaskSystem hts = new HumanTaskSystem(new TestHumanTaskDAO());
    TaskSystem ts = new TaskSystem(true, "teletraan/config/taskconfigs", "target/tests/tmp", hts, null);
    LocalTaskFactory.getInstance().init(ts);
    TaskFactory.INSTANCE = LocalTaskFactory.getInstance();
    GraphExecutor ge = new GraphExecutor(new TestGraphExecutionRuntime(stateStore, db));
    GraphEngine e = new GraphEngine(LocalResourceFactory.INSTANCE, db, ge);

    if (preRunHooks != null) {
      for (Function<GraphExecutor, Void> function : preRunHooks) {
        function.apply(ge);
      }
    }
    return new ExecutionObjectBundle(
        e.executeGraphUpate("testuser", GSON.fromJson(proposedDeltaGraph, DELTAGRAPHTYPE)), ge,
        hts);
  }

  public static void executeUntilComplete(ExecutionObjectBundle b) {
    while (!b.getGe().getRuntime().getExecutionQueue().isEmpty()) {
      b.getGe().execute();
    }
  }
  
  public static void printResourceDBState(ExecutionObjectBundle bundle) {
    TestResourceDB resourceDB = (TestResourceDB) bundle.getGe().getRuntime().getResourceDB();
    for (String string : resourceDB.getResourceMap().values()) {
      System.out.println(string + "\n");
    }
  }

  public static class ExecutionObjectBundle {

    private ExecutionGraph eg;
    private GraphExecutor ge;
    private HumanTaskSystem hts;

    public ExecutionObjectBundle(ExecutionGraph eg, GraphExecutor ge, HumanTaskSystem hts) {
      this.eg = eg;
      this.ge = ge;
      this.hts = hts;
    }

    public ExecutionGraph getEg() {
      return eg;
    }

    public GraphExecutor getGe() {
      return ge;
    }

    public HumanTaskSystem getHts() {
      return hts;
    }
  }

}
