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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonMessageBodyHandler
    implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
  private static final String UTF_8 = "UTF-8";
  
  private static final Gson GSON = new GsonBuilder().serializeNulls().create();

  @Override
  public boolean isReadable(Class<?> type,
                            Type genericType,
                            java.lang.annotation.Annotation[] annotations,
                            MediaType mediaType) {
    return true;
  }

  @Override
  public Object readFrom(Class<Object> type,
                         Type genericType,
                         Annotation[] annotations,
                         MediaType mediaType,
                         MultivaluedMap<String, String> httpHeaders,
                         InputStream entityStream) {
    InputStreamReader streamReader = null;
    try {
      streamReader = new InputStreamReader(entityStream, UTF_8);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    try {
      Type jsonType;
      if (type.equals(genericType)) {
        jsonType = type;
      } else {
        jsonType = genericType;
      }
      return GSON.fromJson(streamReader, jsonType);
    } finally {
      try {
        streamReader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean isWriteable(Class<?> type,
                             Type genericType,
                             Annotation[] annotations,
                             MediaType mediaType) {
    return true;
  }

  @Override
  public long getSize(Object object,
                      Class<?> type,
                      Type genericType,
                      Annotation[] annotations,
                      MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(Object object,
                      Class<?> type,
                      Type genericType,
                      Annotation[] annotations,
                      MediaType mediaType,
                      MultivaluedMap<String, Object> httpHeaders,
                      OutputStream entityStream) throws IOException, WebApplicationException {
    OutputStreamWriter writer = new OutputStreamWriter(entityStream, UTF_8);
    try {
      Type jsonType;
      if (type.equals(genericType)) {
        jsonType = type;
      } else {
        jsonType = genericType;
      }
      GSON.toJson(object, jsonType, writer);
    } finally {
      writer.close();
    }
  }
}
