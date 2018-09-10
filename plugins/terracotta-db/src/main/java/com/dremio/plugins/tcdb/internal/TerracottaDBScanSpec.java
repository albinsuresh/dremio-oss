/*
 * Copyright (C) 2017-2018 Dremio Corporation
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
package com.dremio.plugins.tcdb.internal;

import com.dremio.service.namespace.NamespaceKey;
import com.google.common.base.Objects;

public class TerracottaDBScanSpec {

  private NamespaceKey tableName;

  public TerracottaDBScanSpec(NamespaceKey key) {
    this.tableName = key;
  }

  public NamespaceKey getTableName() {
    return tableName;
  }

  @Override
  public String toString() {
    return "TerracottaDBScanSpec [tableName=" + tableName
           + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TerracottaDBScanSpec that = (TerracottaDBScanSpec) o;
    return Objects.equal(tableName, that.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(tableName);
  }
}
