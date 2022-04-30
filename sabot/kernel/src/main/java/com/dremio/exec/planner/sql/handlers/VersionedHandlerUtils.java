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
package com.dremio.exec.planner.sql.handlers;

import org.apache.calcite.sql.SqlIdentifier;

import com.dremio.common.exceptions.UserException;
import com.dremio.service.namespace.NamespaceKey;

public class VersionedHandlerUtils {

  private VersionedHandlerUtils() {
  }

  public static String resolveSourceName(SqlIdentifier sourceIdentifier, NamespaceKey defaultSchemaPath) {
    if (sourceIdentifier == null) {
      if (defaultSchemaPath == null) {
        throw UserException.validationError()
          .message("Unable to choose a source. \"[ IN sourceName ]\" was not specified and current context was not found.")
          .buildSilently();
      }
      return defaultSchemaPath.getRoot();
    }

    return sourceIdentifier.toString();
  }

}
