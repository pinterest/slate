package com.pinterest.slate.graph;

import java.lang.reflect.Type;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pinterest.slate.resources.PlanException;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.satellite.LocalResourceFactory;
import com.pinterest.slate.validation.ResourceValidationFactory;

public class TestGraphEngine {

  private static final Gson GSON = new Gson();
  private static final Type deltaGraphType = new TypeToken<Map<String, Resource>>() {
  }.getType();

  @BeforeClass
  public static void beforeClass() throws Exception {
    ResourceValidationFactory.getInstance().init("src/test/resources/validation.properties");
  }

  @Test
  public void testBasicPlanAndValidation() throws Exception {
    AbstractResourceDB testDB = new TestResourceDB();
    LocalResourceFactory.INSTANCE.init("teletraan/resourceconfigs", testDB);
    GraphEngine g = new GraphEngine(LocalResourceFactory.INSTANCE, testDB);
    // graph id and resource id must match
    testGraph("""
        {"node1":{"id":"prn:aws:","owner":"logging","environment":"test","region":"us-east-1"}}
        """, "The id of resource in the map and the id inside the ", g);

    // resource must have a project
    testGraph("""
        {"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners":
        {"id":"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners",
        "owner":"logging","environment":"test","region":"us-east-1"}}
        """, "Resource must have a project", g);

    // resource must have a environment
    testGraph("""
        {"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners":
        {"id":"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners",
        "project":"logging","owner":"logging","region":"us-east-1"}}
        """, "Resource must have an environment", g);

    // resource must have a region
    testGraph("""
        {"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners":
        {"id":"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners",
        "owner":"logging","environment":"test"}}
        """, "Resource must have a region", g);

    // resource must have a definition class
    testGraph("""
        {"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners":
        {"id":"prn:aws:prod:kafka:aws_us-east-1:test:pinterest_pinners",
        "project":"logging","region":"eu-west-1","owner":"logging","environment":"test"}}
        """, "Resource must have a resource definition class", g);
  }

  @Test
  public void testPlanGraphUpdate() throws Exception {
    AbstractResourceDB testDB = new TestResourceDB();

    LocalResourceFactory.INSTANCE.init("teletraan/resourceconfigs", testDB);
    GraphEngine g = new GraphEngine(LocalResourceFactory.INSTANCE, testDB);

    testGraph("""
        {"tmp_101":
        {"id":"tmp_101",
        "project":"logging","region":"us-east-1","owner":"logging","environment":"prod",
        "resourceDefinitionClass":"com.pinterest.slate.resources.DemoResourceDef",
        "desiredState":{}
        }}
        """, "Missing required parent", g);
    
    
    testGraph("""
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
        """, "Missing required parent", g);
    
  }

  public Map<String, PlanVertex> testGraph(String graphJson,
                                           String expectedErrorString,
                                           GraphEngine g) throws Exception {
    try {
      // resource must have a environment
      return g.planGraphUpdate("testuser", GSON.fromJson(graphJson, deltaGraphType));
    } catch (PlanException e) {
      if (e.getMessage() != null && !e.getMessage().startsWith(expectedErrorString)) {
        throw e;
      } else {
        return null;
      }
    }
  }

}
