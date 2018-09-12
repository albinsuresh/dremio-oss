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

import com.dremio.common.exceptions.ExecutionSetupException;
import com.dremio.common.expression.SchemaPath;
import com.dremio.exec.physical.base.GroupScan;
import com.dremio.exec.store.RecordReader;
import com.dremio.plugins.tcdb.TerracottaDBStoragePlugin;
import com.dremio.sabot.exec.context.OperatorContext;
import com.dremio.sabot.exec.fragment.FragmentExecutionContext;
import com.dremio.sabot.op.scan.ScanOperator;
import com.dremio.sabot.op.spi.ProducerOperator;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import java.util.Arrays;
import java.util.List;

public class TerracottaDBScanCreator implements ProducerOperator.Creator<TerracottaDBSubScan>{

  @Override
  public ProducerOperator create(
      final FragmentExecutionContext fragmentExecContext,
      final OperatorContext context,
      TerracottaDBSubScan subScan) throws ExecutionSetupException {

     final List<SchemaPath> columns = subScan.getColumns() == null ? GroupScan.ALL_COLUMNS : subScan.getColumns();

    final TerracottaDBStoragePlugin plugin = fragmentExecContext.getStoragePlugin(subScan.getPluginId());

//    final Iterable<RecordReader> readers = FluentIterable.from(subScan.getScans())
//      .transform(scanSpec -> new TerracottaDBRecordReader(context, columns, scanSpec, plugin.getDatasetManager()));

    RecordReader reader = new TerracottaDBRecordReader(context, columns, new TerracottaDBSubScanSpec(subScan.getDatasetName()), plugin.getDatasetManager());
    final Iterable<RecordReader> readers = Arrays.asList(reader);
    return new ScanOperator(fragmentExecContext.getSchemaUpdater(), subScan, context, readers.iterator());
  }


}
