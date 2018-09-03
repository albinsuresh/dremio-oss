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
package com.dremio.plugins.tcdb;

import com.dremio.common.exceptions.UserException;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.SourceTableDefinition;
import com.dremio.service.namespace.dataset.proto.DatasetConfig;
import com.dremio.service.namespace.dataset.proto.DatasetField;
import com.dremio.service.namespace.dataset.proto.DatasetSplit;
import com.dremio.service.namespace.dataset.proto.DatasetType;
import com.dremio.service.namespace.dataset.proto.PhysicalDataset;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.Type;
import com.terracottatech.store.manager.DatasetManager;

import java.util.List;

public class TerracottaDBTableBuilder implements SourceTableDefinition {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TerracottaDBTableBuilder.class);

  private final String name;
  private final Type type;
  private final NamespaceKey namespaceKey;
  private final DatasetManager datasetManager;
  private DatasetConfig datasetConfig;

  public TerracottaDBTableBuilder(String name, Type type, NamespaceKey namespaceKey, DatasetManager datasetManager) {
    this.name = name;
    this.type = type;
    this.datasetManager = datasetManager;

    this.namespaceKey = namespaceKey;
  }

  @Override
  public NamespaceKey getName() {
    return this.namespaceKey;
  }

  @Override
  public DatasetConfig getDataset() throws Exception {
    return null;
//    return new DatasetConfig().setCreatedAt()
//      .setPhysicalDataset(new PhysicalDataset())
//      .setDatasetFieldsList()
//      .setFullPathList()
//      .setId()
//      .setName()
//      .setOwner()
//      .setRecordSchema()
//      .setSchemaVersion()
//      .setType()
//      .setVersion();
  }

//  private String getDatasetColumnNames() {
//    try {
//      Dataset<?> dataset = datasetManager.getDataset(name, type);
//      dataset.reader().records().limit(1).flatMap(record -> record.stream()).map(cell -> cell.definition()).map(cellDef -> new DatasetField().setFieldName(cellDef.name()).setFieldSchema())
//    } catch (StoreException e) {
//      throw UserException.dataReadError(e).message("Failure while connecting to Terracotta.").build(logger);
//    }
//
//  }

  @Override
  public List<DatasetSplit> getSplits() throws Exception {
    return null;
  }

  @Override
  public boolean isSaveable() {
    return true;
  }

  @Override
  public DatasetType getType() {
    return DatasetType.PHYSICAL_DATASET;
  }
}
