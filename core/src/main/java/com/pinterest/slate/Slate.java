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
package com.pinterest.slate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.servlet.FilterRegistration;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import com.pinterest.slate.api.GraphEngineApi;
import com.pinterest.slate.api.HTSApi;
import com.pinterest.slate.api.MetricsApi;
import com.pinterest.slate.api.RecipeApi;
import com.pinterest.slate.api.ResourceApi;
import com.pinterest.slate.api.SlateMgmtApi;
import com.pinterest.slate.api.TaskApi;
import com.pinterest.slate.graph.AbstractExecutionDAO;
import com.pinterest.slate.graph.AbstractGraphAuditSink;
import com.pinterest.slate.graph.AbstractGraphExecutionQueue;
import com.pinterest.slate.graph.AbstractStateStore;
import com.pinterest.slate.graph.ExecutionGraph;
import com.pinterest.slate.graph.ExecutionGraphDAO;
import com.pinterest.slate.graph.GraphEngine;
import com.pinterest.slate.graph.GraphExecutionRuntime;
import com.pinterest.slate.graph.GraphExecutor;
import com.pinterest.slate.graph.storage.ProposedResourceDAO;
import com.pinterest.slate.graph.storage.RDBMSResourceDAO;
import com.pinterest.slate.graph.storage.RDBMSResourceDB;
import com.pinterest.slate.human.AbstractHumanTaskDAO;
import com.pinterest.slate.human.HumanTask;
import com.pinterest.slate.human.HumanTaskDAO;
import com.pinterest.slate.human.HumanTaskSystem;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.TaskFactory;
import com.pinterest.slate.recipe.AbstractRecipeStore;
import com.pinterest.slate.resources.ProposedResource;
import com.pinterest.slate.resources.Resource;
import com.pinterest.slate.resources.ResourceFactory;
import com.pinterest.slate.security.AuthorizationFilter;
import com.pinterest.slate.utils.DaemonThreadFactory;
import com.pinterest.slate.validation.ResourceValidationFactory;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

/**
 * Entry point class for Dropwizard app
 */
public class Slate extends Application<SlateConfig> {

  private static final Logger logger = Logger.getLogger(Slate.class.getName());
  private static final HibernateBundle<SlateConfig> slateBundle = new HibernateBundle<SlateConfig>(
      HumanTask.class, Resource.class, ExecutionGraph.class, ProposedResource.class) {

    @Override
    public DataSourceFactory getDataSourceFactory(SlateConfig configuration) {
      return configuration.getResourceDatabase();
    }

  };

  private TaskSystem ts;

  @Override
  public void initialize(Bootstrap<SlateConfig> bootstrap) {
    bootstrap.addBundle(slateBundle);
    bootstrap.addBundle(new AssetsBundle("/webapp/build", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<SlateConfig>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(SlateConfig configuration) {
        SwaggerBundleConfiguration config = new SwaggerBundleConfiguration();
        config.setTitle("Schema Registry API");
        config.setResourcePackage("com.pinterest.slate.api");
        return config;
      }
    });
  }

  @Override
  public void run(SlateConfig configuration, Environment environment) throws Exception {
    environment.jersey().register(GsonMessageBodyHandler.class);

    FilterRegistration.Dynamic registration = environment.servlets().addFilter("UrlRewriteFilter",
        new UrlRewriteFilter());
    registration.addMappingForUrlPatterns(null, true, "/*");
    registration.setInitParameter("confPath", "urlrewrite.xml");
    environment.jersey().setUrlPattern("/api/*");

    initV2(configuration, environment);

    logger.info("Slate scheduled restart " + configuration.getRestartIntervalSeconds() + "s.");
    // add nightly restart to Slate
    Executors.newScheduledThreadPool(1, DaemonThreadFactory.INSTANCE).schedule(() -> {
      logger.info("Slate is doing regular scheduled restart "
          + configuration.getRestartIntervalSeconds() + "s, this is expected by design.");
      System.exit(0);
    }, configuration.getRestartIntervalSeconds(), TimeUnit.SECONDS);
  }

  private void initV2(SlateConfig config, Environment environment) throws Exception {
    configureAuthorizationFilter(config, environment);

    HumanTaskDAO hto = new HumanTaskDAO(slateBundle.getSessionFactory());
    HumanTaskSystem hts = new UnitOfWorkAwareProxyFactory(slateBundle).create(HumanTaskSystem.class,
        AbstractHumanTaskDAO.class, hto);
    ts = new TaskSystem(config.isEnableDevelopment(), config.getTaskConfigurationDirectory(),
        config.getTaskTmpDirectory(), hts, null);
    TaskFactory.INSTANCE.init(ts, config);

    AbstractStateStore stateStore = initializeStateStore(config);

    RDBMSResourceDAO resourceDao = new RDBMSResourceDAO(slateBundle.getSessionFactory(),
        stateStore);
    ProposedResourceDAO proposedResourceDAO = new ProposedResourceDAO(
        slateBundle.getSessionFactory());
    RDBMSResourceDB resourceDB = new UnitOfWorkAwareProxyFactory(slateBundle).create(
        RDBMSResourceDB.class, new Class[] { RDBMSResourceDAO.class, ProposedResourceDAO.class },
        new Object[] { resourceDao, proposedResourceDAO });
    resourceDB.init(config);

    ResourceFactory.INSTANCE.init(config, resourceDB);

    AbstractGraphExecutionQueue queue = initializeGraphExecutionQueue(config);
    AbstractGraphAuditSink auditSink = initializeAuditSink(config);

    ExecutionGraphDAO eto = new ExecutionGraphDAO(slateBundle.getSessionFactory(), stateStore);
    GraphExecutionRuntime grt = new UnitOfWorkAwareProxyFactory(slateBundle)
        .create(GraphExecutionRuntime.class, AbstractExecutionDAO.class, eto);
    grt.setResourceDB(resourceDB);
    grt.setAuditSink(auditSink);
    grt.setExecutionQueue(queue);
    GraphExecutor executor = new GraphExecutor(grt);
    Thread th = new Thread(executor);
    th.setName("GraphExecutor");
    th.setDaemon(true);
    th.start();

    ResourceValidationFactory.getInstance().init(config.getValidationConfigPath());

    GraphEngine engine = new GraphEngine(ResourceFactory.INSTANCE, resourceDB, executor);

    AbstractRecipeStore recipeStore = initializeRecipeStore(config);
    if (recipeStore != null) {
      environment.jersey().register(new RecipeApi(recipeStore));
    }

    environment.jersey().register(new ResourceApi(resourceDB, stateStore));
    environment.jersey().register(new GraphEngineApi(config, engine, grt));
    environment.jersey().register(new TaskApi(config, ts));
    environment.jersey().register(new SlateMgmtApi(config, grt));
    environment.jersey().register(new HTSApi(hts));
    environment.jersey().register(new MetricsApi(config));
  }

  private AbstractRecipeStore initializeRecipeStore(SlateConfig configuration) throws Exception {
    if (configuration.getRecipeStoreConfigPath() == null) {
      return null;
    }
    PropertiesConfiguration config = new PropertiesConfiguration();
    try {
      config.read(new FileReader(new File(configuration.getRecipeStoreConfigPath())));
    } catch (ConfigurationException | IOException e) {
      throw new IOException(e);
    }
    String stateStoreClass = config.getString("class");
    AbstractRecipeStore stateStore = Class.forName(stateStoreClass)
        .asSubclass(AbstractRecipeStore.class).newInstance();
    stateStore.init(config);
    return stateStore;
  }

  protected void configureAuthorizationFilter(SlateConfig config,
                                              Environment environment) throws Exception {
    /**
    AuthorizationFilter authorizer = new AuthorizationFilter();
    authorizer.configure(config);
    environment.jersey().register(authorizer);
    environment.jersey().register(RolesAllowedDynamicFeature.class);
    */
  }

  private AbstractGraphExecutionQueue initializeGraphExecutionQueue(SlateConfig configuration) throws IOException,
                                                                                               InstantiationException,
                                                                                               IllegalAccessException,
                                                                                               ClassNotFoundException {
    PropertiesConfiguration config = new PropertiesConfiguration();
    try {
      config.read(new FileReader(new File(configuration.getGraphExecutionQueueConfigPath())));
    } catch (ConfigurationException | IOException e) {
      throw new IOException(e);
    }
    String executionQueueClass = config.getString("class");
    AbstractGraphExecutionQueue executionQueue = Class.forName(executionQueueClass)
        .asSubclass(AbstractGraphExecutionQueue.class).newInstance();
    executionQueue.init(config);
    return executionQueue;
  }

  private AbstractGraphAuditSink initializeAuditSink(SlateConfig configuration) throws Exception {
    PropertiesConfiguration config = new PropertiesConfiguration();
    try {
      config.read(new FileReader(new File(configuration.getAuditSinkConfigPath())));
    } catch (ConfigurationException | IOException e) {
      throw new IOException(e);
    }
    String sinkClass = config.getString("class");
    AbstractGraphAuditSink sink = Class.forName(sinkClass).asSubclass(AbstractGraphAuditSink.class)
        .newInstance();
    sink.init(config);
    return sink;
  }

  private AbstractStateStore initializeStateStore(SlateConfig configuration) throws IOException,
                                                                             InstantiationException,
                                                                             IllegalAccessException,
                                                                             ClassNotFoundException {
    PropertiesConfiguration config = new PropertiesConfiguration();
    try {
      config.read(new FileReader(new File(configuration.getStateStoreConfigPath())));
    } catch (ConfigurationException | IOException e) {
      throw new IOException(e);
    }
    String stateStoreClass = config.getString("class");
    AbstractStateStore stateStore = Class.forName(stateStoreClass)
        .asSubclass(AbstractStateStore.class).newInstance();
    stateStore.init(config);
    return stateStore;
  }

  public static void main(String[] args) throws Exception {
    new Slate().run(args);
  }
}
