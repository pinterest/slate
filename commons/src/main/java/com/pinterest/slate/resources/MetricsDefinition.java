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
package com.pinterest.slate.resources;

public class MetricsDefinition {
  
  private String metricLabel;
  private String query;
  private double warnThreshold;
  private double severeThreshold;
  
  public MetricsDefinition() {
  }
  
  public MetricsDefinition(String metricLabel,
                           String query,
                           double warnThreshold,
                           double severeThreshold) {
    this.metricLabel = metricLabel;
    this.query = query;
    this.warnThreshold = warnThreshold;
    this.severeThreshold = severeThreshold;
  }

  public String getMetricLabel() {
    return metricLabel;
  }
  public void setMetricLabel(String metricLabel) {
    this.metricLabel = metricLabel;
  }
  public String getQuery() {
    return query;
  }
  public void setQuery(String query) {
    this.query = query;
  }
  public double getWarnThreshold() {
    return warnThreshold;
  }
  public void setWarnThreshold(double warnThreshold) {
    this.warnThreshold = warnThreshold;
  }
  public double getSevereThreshold() {
    return severeThreshold;
  }
  public void setSevereThreshold(double severeThreshold) {
    this.severeThreshold = severeThreshold;
  }

  @Override
  public String toString() {
    return "MetricsDefinition [metricLabel=" + metricLabel + ", query=" + query + ", warnThreshold="
        + warnThreshold + ", severeThreshold=" + severeThreshold + "]";
  }

}