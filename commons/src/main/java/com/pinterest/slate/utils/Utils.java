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
package com.pinterest.slate.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

/**
 * Utils class for string parsing, etc.
 */
public class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class.getName());
  private static final String NIMBUS_API_ENDPOINT = "https://nimbus.pinadmin.com/api/v1";
  private static CloseableHttpClient httpClient = HttpClients.createDefault();

  public static final Gson GSON = new Gson();
  public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
  
  public static String executeGetRequest(String URL) {
    String responseBody = null;
    try {
      @SuppressWarnings("resource")
      HttpClient client = HttpClientBuilder.create().build(); // documentation says client does not
                                                              // need to be explicitly closed
      HttpGet getRequest = new HttpGet(URL);
      HttpResponse response = client.execute(getRequest);
      responseBody = EntityUtils.toString(response.getEntity());
      getRequest.releaseConnection();
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Failed to execute GET request " + URL + " due to exception: " + e.getMessage(), e);
    }
    return responseBody;
  }

  /**
   * JsonArray to String array
   * 
   * @param jsonArray
   * @return stringArray from JsonArray
   */
  public static String[] jsonToStringArray(JsonArray jsonArray) {
    int jaLength = jsonArray.size();
    String[] stringArray = new String[jaLength];
    for (int i = 0; i < jaLength; i++) {
      String entry = jsonArray.get(i).getAsString();
      stringArray[i] = entry;
    }
    return stringArray;
  }

  /**
   * @param stringArray
   * @return JsonArray
   */
  public static JsonArray stringArrayToJsonArray(String[] stringArray) {
    JsonArray ja = new JsonArray();
    for (String s : stringArray) {
      ja.add(s);
    }
    return ja;
  }
  
  public static boolean nimbusProjectIsValid(String projectName) throws Exception {
    if (projectName.equals("unknown")) {
      return false;
    }
    HttpGet httpGet = new HttpGet(NIMBUS_API_ENDPOINT + "/projects/" + projectName);
    try (CloseableHttpResponse resp = httpClient.execute(httpGet)) {
      if (resp.getStatusLine().getStatusCode() != 200) {
        return false;
      }
    }
    return true;
  }

}
