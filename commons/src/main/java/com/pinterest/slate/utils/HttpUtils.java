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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.ws.rs.ServiceUnavailableException;

import org.apache.http.ParseException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.pinterest.slate.resources.PlanException;

public class HttpUtils {

  private static final String MESH_PREFIX = "mesh://";
  private static final String JKS = "JKS";
  public static final String KS_PATH = System.getProperty("KS_PATH");
  public static final char[] PASSWORD = System.getProperty("KS_PASSWORD").toCharArray();
  private static final Logger logger = Logger.getLogger(HttpUtils.class.getCanonicalName());
  private static final Gson GSON = new Gson();
  public static final Pattern REQP = Pattern.compile("mesh\\:\\/\\/(?<host>[a-z\\-\\.]+)\\/.*");

  private HttpUtils() {
  }

  public static void doNothing() {
  }

  public static <E> E makeHttpGet(String url, Type t) throws IOException {
    String meshedUrl = makeMeshUrl(url);
    HttpRequestBase getResource = new HttpGet(meshedUrl);
    makeMeshRequest(url, getResource);
    try (CloseableHttpResponse req = HttpUtils.makeRequest(getResource)) {
      String responseEntity = EntityUtils.toString(req.getEntity());
      if (req.getStatusLine().getStatusCode() != 200) {
        logger.info("Status:" + meshedUrl + " " + url + " " + req.getStatusLine().getStatusCode()
            + " [" + req.getEntity() != null ? responseEntity : "noentity" + "] url:" + url);
      }
      if (req.getStatusLine().getStatusCode() == 503) {
        throw new ServiceUnavailableException();
      }
      if (req.getStatusLine().getStatusCode() == 200) {
        return GSON.fromJson(responseEntity, t);
      } else {
        throw new IOException("Bad response " + req.getStatusLine());
      }
    } catch (HttpHostConnectException e) {
      throw new ServiceUnavailableException();
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException
        | IOException e) {
      throw new IOException(e);
    }
  }

  public static <E> E makeHttpPost(String url, Object val, Type t) throws IOException,
                                                                   PlanException {
    return makeHttpPost(url, val, t, false);
  }

  private static String makeMeshUrl(String url) {
    if (url.startsWith(MESH_PREFIX)) {
      Matcher matcher = REQP.matcher(url);
      matcher.matches();
      String host = matcher.group("host");
      return url.replace("mesh", "http").replace(host, "localhost:19193");
    } else {
      return url;
    }
  }

  private static void makeMeshRequest(String url, HttpRequestBase req) {
    if (url.startsWith(MESH_PREFIX)) {
      Matcher matcher = REQP.matcher(url);
      matcher.matches();
      String host = matcher.group("host");
      req.addHeader("Host", host);
      logger.info("Mesh Host:" + host);
    }
  }

  public static <E> E makeHttpPost(String url,
                                   Object val,
                                   Type t,
                                   boolean ignore204) throws IOException, PlanException {
    String meshedUrl = makeMeshUrl(url);
    HttpPost postResource = new HttpPost(meshedUrl);
    if (val != null) {
      String json = GSON.toJson(val);
      postResource.setEntity(new StringEntity(json));
      logger.fine(url + json);
    }
    makeMeshRequest(url, postResource);
    postResource.addHeader("Origin", meshedUrl);
    postResource.addHeader("Content-Type", "application/json");
    try (CloseableHttpResponse req = HttpUtils.makeRequest(postResource)) {
      String entity = null;
      try {
        entity = EntityUtils.toString(req.getEntity());
      } catch (Exception e) {
      }
      if (req.getStatusLine().getStatusCode() != 200) {
        logger
            .fine("Status:" + req.getStatusLine().getStatusCode() + " [" + entity + "] url:" + url);
      }
      if (req.getStatusLine().getStatusCode() == 503) {
        throw new ServiceUnavailableException();
      }
      if (t == Void.class && req.getStatusLine().getStatusCode() == 204) {
        return null;
      }
      if (req.getStatusLine().getStatusCode() == 200) {
        if (t == Void.class) {
          return null;
        }
        return GSON.fromJson(entity, t);
      } else if (ignore204 && req.getStatusLine().getStatusCode() == 204) {
        return null;
      } else {
        throw new PlanException(
            "Code:" + req.getStatusLine().getStatusCode() + " " + entity != null ? entity
                : "No error recieved");
      }
    } catch (HttpHostConnectException e) {
      throw new ServiceUnavailableException();
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException
        | IOException e) {
      throw new IOException(e);
    }
  }

  public static <E> E makeHttpPut(String url, Object val, Type t) throws IOException,
                                                                  ParseException, PlanException {
    String meshedUrl = makeMeshUrl(url);
    HttpPut putResource = new HttpPut(meshedUrl);
    if (val != null) {
      String json = GSON.toJson(val);
      putResource.setEntity(new StringEntity(json));
    }
    makeMeshRequest(url, putResource);
    putResource.addHeader("Content-Type", "application/json");
    putResource.addHeader("Origin", meshedUrl);
    try (CloseableHttpResponse req = HttpUtils.makeRequest(putResource)) {
      String entity = null;
      try {
        entity = EntityUtils.toString(req.getEntity());
      } catch (Exception e) {
      }
      if (req.getStatusLine().getStatusCode() != 200) {
        logger.fine(req.getStatusLine().getStatusCode() + " ["
            + (req.getEntity() != null ? EntityUtils.toString(req.getEntity()) : "noentity")
            + "] url:" + url);
      }
      if (req.getStatusLine().getStatusCode() == 503) {
        throw new ServiceUnavailableException();
      }
      if (t == Void.class && req.getStatusLine().getStatusCode() == 204) {
        return null;
      }
      if (req.getStatusLine().getStatusCode() == 200) {
        if (t == Void.class) {
          return null;
        }
        return GSON.fromJson(entity, t);
      } else {
        logger.severe("Code:" + req.getStatusLine().getStatusCode() + " " + entity != null ? entity
            : "No error recieved");
        throw new PlanException(
            "Code:" + req.getStatusLine().getStatusCode() + " " + entity != null ? entity
                : "No error recieved");
      }
    } catch (HttpHostConnectException e) {
      throw new ServiceUnavailableException();
    } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException
        | IOException e) {
      throw new IOException(e);
    }
  }

  public static CloseableHttpResponse makeRequest(HttpRequestBase request) throws KeyManagementException,
                                                                           NoSuchAlgorithmException,
                                                                           KeyStoreException,
                                                                           IOException {
    return buildClient(request.getURI().toURL().toString(), 1000, 1000, null).execute(request);
  }

  public static KeyStore readStore() throws Exception {
    try (InputStream keyStoreStream = new FileInputStream(KS_PATH)) {
      KeyStore keyStore = KeyStore.getInstance(JKS);
      keyStore.load(keyStoreStream, PASSWORD);
      return keyStore;
    }
  }

  /**
   * @throws IOException
   */
  public static CloseableHttpClient buildClient(String baseURL,
                                                int connectTimeout,
                                                int requestTimeout,
                                                CredentialsProvider provider) throws NoSuchAlgorithmException,
                                                                              KeyStoreException,
                                                                              KeyManagementException,
                                                                              IOException {
    HttpClientBuilder clientBuilder = HttpClients.custom();
    if (provider != null) {
      clientBuilder.setDefaultCredentialsProvider(provider);
    }
    if (baseURL.startsWith("https://")) {
      SSLContext sslContext;
      try {
        sslContext = SSLContexts.custom().loadKeyMaterial(readStore(), PASSWORD).build();
        clientBuilder.setSSLContext(sslContext);
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
    RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout)
        .setConnectionRequestTimeout(requestTimeout).setAuthenticationEnabled(true).build();
    return clientBuilder.setDefaultRequestConfig(config).build();
  }

}
