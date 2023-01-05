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
package com.pinterest.slate.api;

import java.net.URLEncoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import com.pinterest.slate.SlateConfig;
import com.pinterest.slate.utils.Utils;

@Path("/v2/metrics")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes({ MediaType.APPLICATION_JSON })
public class MetricsApi {

  private SlateConfig config;

  public MetricsApi(SlateConfig config) {
    this.config = config;
  }

  @GET
  public String getStatsboardData(@Context UriInfo uriInfo) throws Exception {
    String url = config.getBaseMetricsUrl();
    String query = uriInfo.getQueryParameters().getFirst("query");
    if (uriInfo.getQueryParameters().containsKey("alias")) {
      query += "&alias=" + uriInfo.getQueryParameters().getFirst("alias");
    }
    String metricFormatted = query.replace("{", URLEncoder.encode("{", "UTF-8"))
        .replace("}", URLEncoder.encode("}", "UTF-8"))
        .replace("|", URLEncoder.encode("|", "UTF-8"));
    String urlFormatted = String.format(url, metricFormatted);
    String response = Utils.executeGetRequest(urlFormatted);
    return response;
  }

}
