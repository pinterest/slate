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

public abstract class BackgroundService implements Runnable {

  public static enum ExceptionPolicy {
                                      FAIL,
                                      RETRY
  }

  protected ResourceDefinition definition;

  public BackgroundService(ResourceDefinition definition) {
    this.definition = definition;
  }

  /**
   * Governs what happens when an exception is encountered by the run method of
   * the bgservice
   * 
   * @return {@link ExceptionPolicy}
   */
  public abstract ExceptionPolicy getExceptionPolicy();

  public abstract void performBackgroundWork(boolean runOnce);

  @Override
  public final void run() {
    boolean flg = true;
    while (flg) {
      try {
        performBackgroundWork(false);
      } catch (Exception e) {
        switch (getExceptionPolicy()) {
        case RETRY:
          flg = true;
          break;
        case FAIL:
          flg = false;
          break;
        }
      }
    }
  }

}
