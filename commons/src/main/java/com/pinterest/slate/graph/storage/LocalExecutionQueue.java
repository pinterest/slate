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
package com.pinterest.slate.graph.storage;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.configuration2.Configuration;

import com.pinterest.slate.graph.AbstractGraphExecutionQueue;

public class LocalExecutionQueue extends AbstractGraphExecutionQueue {

  private Queue<String> queue = new ArrayBlockingQueue<>(100);

  @Override
  public void init(Configuration config) throws IOException {
  }
  
  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  @Override
  public String take() {
    return !queue.isEmpty() ? queue.remove() : null;
  }

  @Override
  public void add(String executionId) {
    queue.add(executionId);
  }

  @Override
  public void delete(String executionId) {
  }

  @Override
  public void bootstrap(List<String> graphs) {
    queue.addAll(graphs);
  }

}
