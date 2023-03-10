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
package com.pinterest.slate.process;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public enum Status {

                    RUNNING,
                    NOT_STARTED,
                    FAILED,
                    SUCCEEDED,
                    CANCELLED;

  public static final Set<Status> END_STATUS = ImmutableSet.of(Status.CANCELLED, Status.SUCCEEDED,
      Status.FAILED);

  public static boolean isComplete(Status status) {
    return END_STATUS.contains(status);
  }

  boolean isFailed(Status status) {
    return false;
  }

}