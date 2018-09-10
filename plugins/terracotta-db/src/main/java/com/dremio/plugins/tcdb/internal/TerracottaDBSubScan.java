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

import com.dremio.common.expression.SchemaPath;
import com.dremio.exec.catalog.StoragePluginId;
import com.dremio.exec.physical.base.SubScanWithProjection;
import com.dremio.exec.proto.UserBitShared;
import com.dremio.exec.record.BatchSchema;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("tcdb-sub-scan")
public class TerracottaDBSubScan extends SubScanWithProjection {

  private final StoragePluginId pluginId;
  private final List<TerracottaDBSubScanSpec> scans;
  private final String datasetName;

  public TerracottaDBSubScan(
    @JsonProperty("userName") String userName,
    @JsonProperty("schema") BatchSchema schema,
    @JsonProperty("tableSchemaPath") List<String> tableSchemaPath,
    @JsonProperty("columns") List<SchemaPath> columns,
    @JsonProperty("pluginId") StoragePluginId pluginId,
    @JsonProperty("scans") List<TerracottaDBSubScanSpec> scans,
    @JsonProperty("datasetName") String datasetName
  ) {
    super(userName, schema, tableSchemaPath, columns);
    this.scans = scans;
    this.pluginId = pluginId;
    this.datasetName = datasetName;
  }

  public StoragePluginId getPluginId() {
    return pluginId;
  }

  public List<TerracottaDBSubScanSpec> getScans() {
    return scans;
  }

  @Override
  public int getOperatorType() {
    return UserBitShared.CoreOperatorType.TCDB_SUB_SCAN_VALUE;
  }

  public String getDatasetName() {
    return datasetName;
  }
}
