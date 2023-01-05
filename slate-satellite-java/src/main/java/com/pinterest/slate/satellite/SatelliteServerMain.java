package com.pinterest.slate.satellite;

import com.pinterest.slate.GsonMessageBodyHandler;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.satellite.api.ResourceApi;
import com.pinterest.slate.satellite.api.TaskApi;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class SatelliteServerMain extends Application<SatelliteServerConfig> {

  private TaskSystem ts;

  @Override
  public void run(SatelliteServerConfig config, Environment environment) throws Exception {
    environment.jersey().register(GsonMessageBodyHandler.class);
    if (Boolean.parseBoolean(System.getProperty("loaddemo", "false"))) {
      System.out.println("Don't ignore RD");
      LocalResourceFactory.ACTIVATE_IGNORE_RD = false;
    }
    
    RPCResourceDB resourceDB = new RPCResourceDB();
    resourceDB.init(config);
    LocalResourceFactory.INSTANCE.init(config.getResourceConfigDirectory(), resourceDB);
    RPCHTS hts = new RPCHTS();
    hts.init(config);
    ts = new TaskSystem(config.isEnableDevelopment(), config.getTaskConfigurationDirectory(),
        config.getTaskTmpDirectory(), hts, config.getTaskDefinitionFQCN());
    LocalTaskFactory.INSTANCE.init(ts);
    environment.jersey().register(new ResourceApi(resourceDB));
    environment.jersey().register(new TaskApi(new LocalTaskRuntime()));
    environment.jersey().setUrlPattern("/api/*");
  }
  
  public static void main(String[] args) throws Exception {
    new SatelliteServerMain().run(args);
  }

}
