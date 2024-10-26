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
package com.dremio.exec.vector.complex.fn;

/**
 * Describes the default date output format to use for JSON. Dremio's default behavior for text
 * output formats is to use a string which can be implicitly casted back to its original type (so
 * the same format as the SQL literal format where applicable). However, in JSON, we also can use
 * extended types to specifically identify the data type of the output. In this case, Dremio outputs
 * ISO standard formats rather than SQL formats to ensure compatibility with other systems (namely
 * MongoDB).
 */
public enum DateOutputFormat {
  /**
   * The SQL literal format for dates. This means no timezone in times and a space in between the
   * date and time for timestamp.
   */
  SQL,

  /** The ISO standard format for dates/times. */
  ISO
}
