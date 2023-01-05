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
package com.pinterest.slate.process.taskdefinitions;

import static com.pinterest.slate.resources.Utils.getStringOrDefault;
import static com.pinterest.slate.resources.Utils.getStringOrError;

import java.io.IOException;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.pinterest.slate.human.TaskSystem;
import com.pinterest.slate.process.LifecycleProcess;
import com.pinterest.slate.process.Status;
import com.pinterest.slate.process.StatusUpdate;
import com.pinterest.slate.process.TaskDefinition;
import com.pinterest.slate.process.TaskRuntime;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.users.UsersLookupByEmailRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.users.UsersLookupByEmailResponse;

public class SlackTask extends TaskDefinition {

  private static final String INVALID_VALUE = "invalid_value";
  private static final String EMAIL_DOMAIN = System.getenv("EMAIL_DOMAIN");
  public static final String USERNAME = "username";
  public static final String LINK = "link";
  public static final String MESSAGE = "message";
  public static final String TITLE = "title";
  public static final String TASK_DEFINITION_ID = "slackTask";
  private static final Logger logger = Logger.getLogger(SlackTask.class.getCanonicalName());
  private static String token;

  static {
    token = System.getenv("SLACK_TOKEN");
    if (token == null) {
      token = INVALID_VALUE;
    }
  }

  public SlackTask() {
    super(TASK_DEFINITION_ID);
  }

  @Override
  public void init(TaskSystem engine) throws Exception {
  }

  @Override
  public void validate(String taskInstanceId,
                       LifecycleProcess process,
                       JsonObject processContext,
                       JsonObject taskContext) throws Exception {
    if (!taskContext.has(USERNAME) || taskContext.get(USERNAME) == null) {
      throw new Exception("Missing username to send message to");
    }
    if (!taskContext.has(TITLE)) {
      throw new Exception("Missing title");
    }
    if (!taskContext.has(MESSAGE)) {
      throw new Exception("Missing message");
    }
  }

  @Override
  public StatusUpdate startExecution(TaskRuntime runtime,
                                     String taskId,
                                     LifecycleProcess workflow,
                                     JsonObject processContext,
                                     JsonObject taskContext) throws Exception {
    String title = getStringOrError(taskContext, TITLE);
    String message = getStringOrError(taskContext, MESSAGE);
    String link = getStringOrDefault(taskContext, LINK, "n/a");
    String username = getStringOrError(taskContext, USERNAME);

    sendSlackMessage(title, message, link, username);

    return StatusUpdate.create(Status.SUCCEEDED);
  }

  public static void sendSlackMessage(String title,
                                      String message,
                                      String link,
                                      String username) throws IOException, SlackApiException {
    if (token.equals(INVALID_VALUE)) {
      return;
    }
    Slack slack = Slack.getInstance();
    UsersLookupByEmailResponse resp = slack.methods(token).usersLookupByEmail(
        UsersLookupByEmailRequest.builder().email(username + "@" + EMAIL_DOMAIN).build());
    if (resp.isOk()) {
      username = resp.getUser().getId();
      ChatPostMessageResponse chatPostMessage = slack.methods(token)
          .chatPostMessage(ChatPostMessageRequest.builder().channel(username)
              .text("*" + title + "*\n" + message + "\n" + link).build());
      if (!chatPostMessage.isOk()) {
        logger.severe(
            "Failed to send message to user:" + username + " reason:" + chatPostMessage.getError());
      }
    } else {
      title = "Error detected while trying to find userid for:" + username + " reason:"
          + resp.getError();
    }
  }

  @Override
  public StatusUpdate checkStatus(TaskRuntime runtime,
                                  String taskId,
                                  LifecycleProcess process,
                                  JsonObject processContext,
                                  JsonObject taskContext) throws Exception {
    return null;
  }
}
