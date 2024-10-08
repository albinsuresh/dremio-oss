/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.common.exceptions;

import com.dremio.exec.proto.CoordinationProtos;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Holds context information about a UserException. We can add structured context information that
 * will added to the error message displayed to the client.
 */
class UserExceptionContext {

  private final String errorId;
  private final List<String> contextList;

  private CoordinationProtos.NodeEndpoint endpoint;
  private String errorOrigin;

  UserExceptionContext() {
    errorId = UUID.randomUUID().toString();
    contextList = new ArrayList<>();
  }

  /**
   * adds a string to the bottom of the context list
   *
   * @param context context string
   */
  void add(String context) {
    contextList.add(context);
  }

  /**
   * add NodeEndpoint identity to the context.
   *
   * <p>if the context already has a NodeEndpoint identity, the new identity will be ignored
   *
   * @param endpoint node endpoint identity
   */
  void add(CoordinationProtos.NodeEndpoint endpoint) {
    if (this.endpoint == null) {
      this.endpoint = endpoint;
    }
  }

  void addErrorOrigin(String role) {
    this.errorOrigin = role;
  }

  /**
   * adds a sring value to the bottom of the context list
   *
   * @param context context name
   * @param value context value
   */
  void add(String context, String value) {
    add(context + " " + value);
  }

  /**
   * adds a long value to the bottom of the context list
   *
   * @param context context name
   * @param value context value
   */
  void add(String context, long value) {
    add(context + " " + value);
  }

  /**
   * adds a double to the bottom of the context list
   *
   * @param context context name
   * @param value context value
   */
  void add(String context, double value) {
    add(context + " " + value);
  }

  /**
   * pushes a string to the top of the context list
   *
   * @param context context string
   */
  void push(String context) {
    contextList.add(0, context);
  }

  /**
   * pushes a string value to the top of the context list
   *
   * @param context context name
   * @param value context value
   */
  void push(String context, String value) {
    push(context + " " + value);
  }

  /**
   * pushes a long value to the top of the context list
   *
   * @param context context name
   * @param value context value
   */
  void push(String context, long value) {
    push(context + " " + value);
  }

  /**
   * adds a double at the top of the context list
   *
   * @param context context name
   * @param value context value
   */
  void push(String context, double value) {
    push(context + " " + value);
  }

  String getErrorId() {
    return errorId;
  }

  CoordinationProtos.NodeEndpoint getEndpoint() {
    return endpoint;
  }

  String getErrorOrigin() {
    return errorOrigin;
  }

  /**
   * generate a context message
   *
   * @return string containing all context information concatenated
   */
  String generateContextMessage(boolean includeErrorIdAndIdentity) {
    StringBuilder sb = new StringBuilder();

    for (String context : contextList) {
      sb.append(context).append("\n");
    }

    if (getErrorOrigin() != null) {
      sb.append("ErrorOrigin: ");
      sb.append(getErrorOrigin());
      sb.append("\n");
    }

    if (includeErrorIdAndIdentity) {
      // add identification infos
      sb.append("[Error Id: ");
      sb.append(errorId).append(" ");
      if (endpoint != null) {
        sb.append("on ").append(endpoint.getAddress()).append(":").append(endpoint.getUserPort());
      }
      sb.append("]");
    }
    return sb.toString();
  }

  List<String> getContextAsStrings() {
    return ImmutableList.copyOf(contextList);
  }
}
