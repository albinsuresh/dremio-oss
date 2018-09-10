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
import com.dremio.exec.physical.base.AbstractGroupScan;
import com.dremio.exec.physical.base.SubScan;
import com.dremio.exec.planner.fragment.DistributionAffinity;
import com.dremio.exec.proto.UserBitShared;
import com.dremio.exec.record.BatchSchema;
import com.dremio.exec.store.SplitWork;
import com.dremio.exec.store.TableMetadata;
import com.dremio.exec.store.dfs.easy.EasySubScan;
import com.dremio.service.namespace.dataset.proto.DatasetSplit;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;

public class TerracottaDBGroupScan extends AbstractGroupScan {

  public TerracottaDBGroupScan(TableMetadata dataset, List<SchemaPath> columns) {
    super(dataset, columns);
  }

  @Override
  public SubScan getSpecificScan(List<SplitWork> work) throws ExecutionSetupException {
    List<TerracottaDBSubScanSpec> splitWork = FluentIterable.from(work).transform(new Function<SplitWork, TerracottaDBSubScanSpec>(){
      @Override
      public TerracottaDBSubScanSpec apply(SplitWork input) {
        return toSubScan(input.getSplit());
      }}).toList();


    return new TerracottaDBSubScan(
      getUserName(),
      this.getSchema(),
      getDataset().getName().getPathComponents(),
      columns,
      dataset.getStoragePluginId(),
      splitWork,
      dataset.getName().getName()
    );
  }

  private TerracottaDBSubScanSpec toSubScan(DatasetSplit split) {
    return new TerracottaDBSubScanSpec(getDataset().getName().getName());
  }

  @Override
  public DistributionAffinity getDistributionAffinity() {
    return DistributionAffinity.NONE;
  }

  @Override
  public int getOperatorType() {
    return UserBitShared.CoreOperatorType.TCDB_SUB_SCAN_VALUE;
  }
}
