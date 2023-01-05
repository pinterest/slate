package com.pinterest.slate.satellite;

import java.util.List;

import javax.validation.constraints.NotNull;

import io.dropwizard.Configuration;

public class SatelliteServerConfig extends Configuration {
  
  private String resourceConfigDirectory = "teletraan/resourceconfigs";
  @NotNull
  private String slateCoreUrl = "http://localhost:8090";
  private boolean enableDevelopment = false;
  private String taskTmpDirectory = "/tmp";
  private String taskConfigurationDirectory;
  private List<String> taskDefinitionFQCN;
  
  public List<String> getTaskDefinitionFQCN() {
    return taskDefinitionFQCN;
  }

  public void setTaskDefinitionFQCN(List<String> taskDefinitionFQCN) {
    this.taskDefinitionFQCN = taskDefinitionFQCN;
  }

  public String getTaskTmpDirectory() {
    return taskTmpDirectory;
  }

  public void setTaskTmpDirectory(String taskTmpDirectory) {
    this.taskTmpDirectory = taskTmpDirectory;
  }

  public String getTaskConfigurationDirectory() {
    return taskConfigurationDirectory;
  }

  public void setTaskConfigurationDirectory(String taskConfigurationDirectory) {
    this.taskConfigurationDirectory = taskConfigurationDirectory;
  }

  public boolean isEnableDevelopment() {
    return enableDevelopment;
  }

  public void setEnableDevelopment(boolean enableDevelopment) {
    this.enableDevelopment = enableDevelopment;
  }

  public String getSlateCoreUrl() {
    return slateCoreUrl;
  }

  public void setSlateCoreUrl(String slateCoreUrl) {
    this.slateCoreUrl = slateCoreUrl;
  }

  public String getResourceConfigDirectory() {
    return resourceConfigDirectory;
  }

  public void setResourceConfigDirectory(String resourceConfigDirectory) {
    this.resourceConfigDirectory = resourceConfigDirectory;
  }

}
