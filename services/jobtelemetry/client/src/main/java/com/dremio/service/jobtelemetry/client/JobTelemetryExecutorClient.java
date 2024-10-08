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
package com.dremio.service.jobtelemetry.client;

import com.dremio.exec.proto.CoordExecRPC;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Empty;

/**
 * JobTelemetry client interface for executors to interact with.
 *
 * <p>Will have different software (fabric based) and service implementations.
 */
public interface JobTelemetryExecutorClient {
  /**
   * Put executor profile for a query.
   *
   * @param request
   * @return listenable future.
   */
  public ListenableFuture<Empty> putExecutorProfile(CoordExecRPC.ExecutorQueryProfile profile);
}
