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
package com.dremio.exec.planner.sql.handlers.commands;

import static com.dremio.exec.planner.sql.CalciteArrowHelper.fromCalciteRowType;

import com.dremio.exec.ops.QueryContext;
import com.dremio.exec.planner.observer.AttemptObserver;
import com.dremio.exec.planner.sql.handlers.SqlHandlerConfig;
import com.dremio.exec.planner.sql.handlers.query.SqlToPlanHandler;
import com.dremio.exec.proto.UserProtos.CreatePreparedStatementResp;
import com.dremio.exec.record.BatchSchema;
import com.google.common.cache.Cache;
import org.apache.calcite.sql.SqlNode;

/** Take a sql node, plan it and then return an async response. */
public class HandlerToPreparePlan extends HandlerToPreparePlanBase<CreatePreparedStatementResp> {

  public HandlerToPreparePlan(
      QueryContext context,
      SqlNode sqlNode,
      SqlToPlanHandler handler,
      Cache<Long, PreparedPlan> planCache,
      String sql,
      AttemptObserver observer,
      SqlHandlerConfig config) {
    super(context, sqlNode, handler, planCache, sql, observer, config);
  }

  @Override
  public CreatePreparedStatementResp execute() {
    final QueryContext context = getContext();
    if (isParameterEnabled()) {
      return PreparedStatementProvider.build(
          fromCalciteRowType(getRowType()),
          getState(),
          context.getQueryId(),
          context.getSession().getCatalogName(),
          context.getSession().getRecordBatchFormat(),
          fromCalciteRowType(getPreparedRowType()));
    }
    return PreparedStatementProvider.build(
        getPlan().getRoot().getProps().getSchema(),
        getState(),
        context.getQueryId(),
        context.getSession().getCatalogName(),
        context.getSession().getRecordBatchFormat(),
        BatchSchema.EMPTY);
  }
}
