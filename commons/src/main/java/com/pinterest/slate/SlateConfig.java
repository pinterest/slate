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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

/**
 * Config class for app from config.yaml
 */
public class SlateConfig extends Configuration {

  @JsonProperty
  public Map<String, Object> openTsdbClientConfig;

  @Valid
  @JsonProperty
  private int restartIntervalSeconds = 86400; // seconds in a day

  @NotNull
  @Valid
  private DataSourceFactory resourceDatabase = new DataSourceFactory();

  @Valid
  private Set<String> adminGroups = new HashSet<>();
  
  @Valid
  private Set<String> adminUsers = new HashSet<>();

  private String taskConfigurationDirectory;

  @NotNull
  private String validationConfigPath;

  private String stateStoreConfigPath;

  private String graphExecutionQueueConfigPath;

  private boolean enableDevelopment = false;

  private String taskTmpDirectory = "/tmp";
  
  private String auditSinkConfigPath;
  
  private boolean activateIgnoreRD = false;
  
  private String slateCoreurl = null;
  
  private List<String> satelliteServerUrls;
  
  private String recipeStoreConfigPath;
  
  private String baseMetricsUrl = "";

  public String getBaseMetricsUrl() {
    return baseMetricsUrl;
  }

  public void setBaseMetricsUrl(String baseMetricsUrl) {
    this.baseMetricsUrl = baseMetricsUrl;
  }

  public String getRecipeStoreConfigPath() {
    return recipeStoreConfigPath;
  }

  public void setRecipeStoreConfigPath(String recipeStoreConfigPath) {
    this.recipeStoreConfigPath = recipeStoreConfigPath;
  }

  public List<String> getSatelliteServerUrls() {
    return satelliteServerUrls;
  }

  public void setSatelliteServerUrls(List<String> satelliteServerUrls) {
    this.satelliteServerUrls = satelliteServerUrls;
  }

  public String getSlateCoreurl() {
    return slateCoreurl;
  }

  public void setSlateCoreurl(String slateCoreurl) {
    this.slateCoreurl = slateCoreurl;
  }

  public boolean isActivateIgnoreRD() {
    return activateIgnoreRD;
  }

  public void setActivateIgnoreRD(boolean activateIgnoreRD) {
    this.activateIgnoreRD = activateIgnoreRD;
  }

  public String getAuditSinkConfigPath() {
    return auditSinkConfigPath;
  }

  public void setAuditSinkConfigPath(String auditSinkConfigPath) {
    this.auditSinkConfigPath = auditSinkConfigPath;
  }

  public String getTaskTmpDirectory() {
    return taskTmpDirectory;
  }

  public void setTaskTmpDirectory(String taskTmpDirectory) {
    this.taskTmpDirectory = taskTmpDirectory;
  }

  public boolean isEnableDevelopment() {
    return enableDevelopment;
  }

  public void setEnableDevelopment(boolean enableDevelopment) {
    this.enableDevelopment = enableDevelopment;
  }

  public String getGraphExecutionQueueConfigPath() {
    return graphExecutionQueueConfigPath;
  }

  public void setGraphExecutionQueueConfigPath(String graphExecutionQueueConfigPath) {
    this.graphExecutionQueueConfigPath = graphExecutionQueueConfigPath;
  }

  public String getStateStoreConfigPath() {
    return stateStoreConfigPath;
  }

  public void setStateStoreConfigPath(String stateStoreConfigPath) {
    this.stateStoreConfigPath = stateStoreConfigPath;
  }

  public String getValidationConfigPath() {
    return validationConfigPath;
  }

  public void setValidationConfigPath(String validationConfigPath) {
    this.validationConfigPath = validationConfigPath;
  }

  public DataSourceFactory getResourceDatabase() {
    return resourceDatabase;
  }

  public void setResourceDatabase(DataSourceFactory resourceDatabase) {
    this.resourceDatabase = resourceDatabase;
  }

  public Set<String> getAdminGroups() {
    return adminGroups;
  }

  public void setAdminGroups(Set<String> adminGroups) {
    this.adminGroups = adminGroups;
  }

  public Set<String> getAdminUsers() {
    return adminUsers;
  }

  public void setAdminUsers(Set<String> adminUsers) {
    this.adminUsers = adminUsers;
  }

  /**
   * @return the openTsdbClientConfig
   */
  public Map<String, Object> getOpenTsdbClientConfig() {
    return openTsdbClientConfig;
  }

  /**
   * @param openTsdbClientConfig the openTsdbClientConfig to set
   */
  public void setOpenTsdbClientConfig(Map<String, Object> openTsdbClientConfig) {
    this.openTsdbClientConfig = openTsdbClientConfig;
  }

  /**
   * @return the s3DumpKey
   */

  /**
   * @return the restartIntervalSeconds
   */
  public int getRestartIntervalSeconds() {
    return restartIntervalSeconds;
  }

  /**
   * @param restartIntervalSeconds the restartIntervalSeconds to set
   */
  public void setRestartIntervalSeconds(int restartIntervalSeconds) {
    this.restartIntervalSeconds = restartIntervalSeconds;
  }

  public String getTaskConfigurationDirectory() {
    return taskConfigurationDirectory;
  }

  public void setTaskConfigurationDirectory(String taskConfigurationDirectory) {
    this.taskConfigurationDirectory = taskConfigurationDirectory;
  }

}
