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
package com.dremio.exec.planner.sql.parser;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlSpecialOperator;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.parser.SqlParserPos;

/** ALTER TABLE tblname DROP [COLUMN] colname */
public class SqlAlterTableDropColumn extends SqlAlterTable {

  public static final SqlSpecialOperator DROP_COLUMN_OPERATOR =
      new SqlSpecialOperator("DROP_COLUMN", SqlKind.ALTER_TABLE) {

        @Override
        public SqlCall createCall(
            SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
          Preconditions.checkArgument(
              operands.length == 4,
              "SqlAlterTableDropColumn.createCall() " + "has to get 4 operands!");

          return new SqlAlterTableDropColumn(
              pos,
              (SqlIdentifier) operands[0],
              (SqlLiteral) operands[1],
              (SqlIdentifier) operands[2],
              (SqlTableVersionSpec) operands[3]);
        }
      };

  protected final SqlIdentifier columnToDrop;
  protected final SqlLiteral dropColumnKeywordPresent;
  protected final SqlTableVersionSpec sqlTableVersionSpec;

  public SqlAlterTableDropColumn(
      SqlParserPos pos,
      SqlIdentifier tblName,
      SqlLiteral dropColumnKeywordPresent,
      SqlIdentifier columnToDrop,
      SqlTableVersionSpec sqlTableVersionSpec) {
    super(pos, tblName);
    this.dropColumnKeywordPresent = dropColumnKeywordPresent;
    this.columnToDrop = columnToDrop;
    this.sqlTableVersionSpec = sqlTableVersionSpec;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    super.unparse(writer, leftPrec, rightPrec);
    writer.keyword("DROP");
    if (dropColumnKeywordPresent.booleanValue()) {
      writer.keyword("COLUMN");
    }
    columnToDrop.unparse(writer, leftPrec, rightPrec);
  }

  @Override
  public SqlOperator getOperator() {
    return DROP_COLUMN_OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    return Lists.newArrayList(tblName, dropColumnKeywordPresent, columnToDrop, sqlTableVersionSpec);
  }

  public String getColumnToDrop() {
    return columnToDrop.getSimple();
  }

  public SqlTableVersionSpec getSqlTableVersionSpec() {
    return sqlTableVersionSpec;
  }
}
