package com.pinterest.slate.graph;

import java.io.IOException;

import org.junit.Test;

import com.pinterest.slate.graph.TestUtils.ExecutionObjectBundle;

public class TestDemoGraphExecution {

  @Test
  public void testDemoParentChildExecution() throws IOException, Exception {
    String graph = """
        {"tmp_101":
        {"id":"tmp_101",
        "project":"logging","region":"us-east-1","owner":"logging","environment":"prod",
        "resourceDefinitionClass":"com.pinterest.slate.resources.DemoResourceDef",
        "desiredState":{},
        "parentResource":"tmp_102"
        },
        "tmp_102":
        {"id":"tmp_102",
        "project":"logging","region":"us-east-1","owner":"logging","environment":"prod",
        "resourceDefinitionClass":"com.pinterest.slate.resources.DemoParentResourceDef",
        "desiredState":{},
        "childResources": ["tmp_101"]
        }}
        """;
    ExecutionObjectBundle bundle = TestUtils.primeAndRunGraph(graph, "target/testdemograph", "", 10,
        null);
    TestUtils.executeUntilComplete(bundle);
    TestUtils.printResourceDBState(bundle);

    graph = """
        {"tmp_102":
        {"id":"tmp_102",
        "project":"logging","region":"us-east-1","owner":"logging","environment":"prod",
        "resourceDefinitionClass":"com.pinterest.slate.resources.DemoParentResourceDef",
        "desiredState":{},
        "childResources": ["tmp_101","tmp_103"]
        },
        "tmp_103":
        {"id":"tmp_103",
        "project":"logging","region":"us-east-1","owner":"logging","environment":"prod",
        "resourceDefinitionClass":"com.pinterest.slate.resources.DemoResourceDef",
        "desiredState":{},
        "parentResource":"tmp_102"
        }
        }
        """;

    TestResourceDB resourceDB = (TestResourceDB) bundle.getGe().getRuntime().getResourceDB();
    bundle = TestUtils.primeAndRunGraphWithMap(graph, "target/testdemograph2",
        resourceDB.getResourceMap(), 10, null);
    TestUtils.executeUntilComplete(bundle);
    TestUtils.printResourceDBState(bundle);
  }

}
