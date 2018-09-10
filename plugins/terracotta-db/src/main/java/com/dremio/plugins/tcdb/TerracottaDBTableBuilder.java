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

import io.protostuff.ByteString;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.ValueVector;
import org.apache.arrow.vector.types.pojo.Field;

import com.dremio.common.exceptions.ExecutionSetupException;
import com.dremio.common.exceptions.UserException;
import com.dremio.common.expression.CompleteType;
import com.dremio.exec.physical.base.GroupScan;
import com.dremio.exec.planner.cost.ScanCostFactor;
import com.dremio.exec.record.BatchSchema;
import com.dremio.exec.server.SabotContext;
import com.dremio.exec.store.SampleMutator;
import com.dremio.plugins.tcdb.internal.TerracottaDBArrowTypeMapper;
import com.dremio.plugins.tcdb.internal.TerracottaDBRecordReader;
import com.dremio.plugins.tcdb.internal.TerracottaDBSubScanSpec;
import com.dremio.plugins.tcdb.proto.TerracottaDBPluginProto;
import com.dremio.service.namespace.DatasetHelper;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.SourceTableDefinition;
import com.dremio.service.namespace.dataset.proto.DatasetConfig;
import com.dremio.service.namespace.dataset.proto.DatasetField;
import com.dremio.service.namespace.dataset.proto.DatasetSplit;
import com.dremio.service.namespace.dataset.proto.DatasetType;
import com.dremio.service.namespace.dataset.proto.PhysicalDataset;
import com.dremio.service.namespace.dataset.proto.ReadDefinition;
import com.dremio.service.namespace.dataset.proto.ScanStats;
import com.dremio.service.namespace.dataset.proto.ScanStatsType;
import com.dremio.service.namespace.proto.EntityId;
import com.dremio.service.users.SystemUser;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.Type;
import com.terracottatech.store.manager.DatasetManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TerracottaDBTableBuilder implements SourceTableDefinition {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TerracottaDBTableBuilder.class);

  private final String name;
  private final Type type;

  private final NamespaceKey namespaceKey;
  private final DatasetManager datasetManager;
  private final Dataset<?> dataset;
  private DatasetConfig datasetConfig;
  private final SabotContext context;

  List<DatasetSplit> splits;

  public TerracottaDBTableBuilder(NamespaceKey namespaceKey, DatasetManager datasetManager, SabotContext context) {
    this.namespaceKey = namespaceKey;
    this.datasetManager = datasetManager;
    this.name = namespaceKey.getName();
    this.context = context;
    try {
      this.type = datasetManager.listDatasets().get(this.name);
      this.dataset = datasetManager.getDataset(name, type);
    } catch (StoreException e) {
      throw UserException.dataReadError(e).message("Failure while fetching dataset list.").build(logger);
    }
  }

  @Override
  public NamespaceKey getName() {
    return this.namespaceKey;
  }

  @Override
  public DatasetConfig getDataset() throws Exception {
    if (datasetConfig == null) {
      String tableName = namespaceKey.getName();
      ReadDefinition readDefinition = new ReadDefinition()
        .setExtendedProperty(
          ByteString.copyFrom(
            TerracottaDBPluginProto.TCDBTableXattr.newBuilder()
              .setTable(com.google.protobuf.ByteString.copyFrom(tableName.getBytes())).build().toByteArray()
          )
        )
      .setScanStats(new ScanStats()
        .setRecordCount(100L)   //TODO estimate
        .setType(ScanStatsType.NO_EXACT_ROW_COUNT)
        .setScanFactor(ScanCostFactor.OTHER.getFactor())
      );

      BatchSchema schema = getSchema();

      this.datasetConfig = new DatasetConfig()
        .setFullPathList(namespaceKey.getPathComponents())
        .setId(new EntityId(UUID.randomUUID().toString()))
        .setType(DatasetType.PHYSICAL_DATASET)
        .setName(namespaceKey.getName())
        .setOwner(SystemUser.SYSTEM_USERNAME)
        .setPhysicalDataset(new PhysicalDataset())
        .setRecordSchema(schema.toByteString())
        .setSchemaVersion(DatasetHelper.CURRENT_VERSION)
        .setReadDefinition(readDefinition);
    }

    return this.datasetConfig;
  }

  private BatchSchema getSchema() {
    ByteString bytes = datasetConfig != null ? DatasetHelper.getSchemaBytes(datasetConfig) : null;
    if(bytes != null) {
      return BatchSchema.deserialize(bytes);
    }

    List<Field> fields = dataset.reader().records()
      .limit(1)
      .flatMap(record -> record.stream())
      .map(cell -> cell.definition())
      .sorted(Comparator.comparing(cellDefinition -> cellDefinition.name()))
      .map(TerracottaDBArrowTypeMapper::mapCellDefinitionToField)
      .collect(Collectors.toList());
    return BatchSchema.newBuilder().addFields(fields).build();

//    final TerracottaDBSubScanSpec spec = new TerracottaDBSubScanSpec(namespaceKey.getName());
//    try (
//      BufferAllocator allocator = context.getAllocator().newChildAllocator("tcdb-sample", 0, Long.MAX_VALUE);
//      SampleMutator mutator = new SampleMutator(allocator);
//      TerracottaDBRecordReader reader = new TerracottaDBRecordReader(null, GroupScan.ALL_COLUMNS, spec, datasetManager);
//    ) {
//      if(oldSchema != null) {
//        oldSchema.materializeVectors(GroupScan.ALL_COLUMNS, mutator);
//      }
//
////      // add row key.
////      mutator.addField(CompleteType.VARBINARY.toField(HBaseRecordReader.ROW_KEY), ValueVector.class);
////
////      // add all column families.
////      for (HColumnDescriptor col : descriptor.getFamilies()) {
////        mutator.addField(CompleteType.struct().toField(col.getNameAsString()), ValueVector.class);
////      }
//
//      reader.setup(mutator);
//      reader.next();
//      mutator.getContainer().buildSchema(BatchSchema.SelectionVectorMode.NONE);
//      return mutator.getContainer().getSchema();
//    } catch (ExecutionSetupException e) {
//      throw UserException.dataReadError(e).message("Unable to sample schema for table %s.", namespaceKey.getName()).build(logger);
//    }
  }

  @Override
  public List<DatasetSplit> getSplits() throws Exception {
    return Collections.emptyList();
//    if (splits == null) {
//      DatasetSplit split = new DatasetSplit().setSplitKey("foo").setSize(5L).setRowCount(5L);
//      splits = Collections.singletonList(split);
//    }
//    return splits;
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
