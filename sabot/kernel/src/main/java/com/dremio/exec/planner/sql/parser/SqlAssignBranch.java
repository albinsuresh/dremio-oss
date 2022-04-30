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

import java.lang.reflect.Constructor;
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

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.ops.QueryContext;
import com.dremio.exec.planner.sql.handlers.direct.SqlDirectHandler;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Implements SQL ALTER BRANCH ASSIGN to assign a new reference to the given branch. Represents
 * statements like: ALTER BRANCH branchName ASSIGN ( BRANCH | TAG | COMMIT ) reference IN source
 */
public final class SqlAssignBranch extends SqlVersionBase {
  public static final SqlSpecialOperator OPERATOR =
      new SqlSpecialOperator("ASSIGN_BRANCH", SqlKind.OTHER) {
        @Override
        public SqlCall createCall(
            SqlLiteral functionQualifier, SqlParserPos pos, SqlNode... operands) {
          Preconditions.checkArgument(
              operands.length == 4, "SqlAssignBranch.createCall() has to get 4 operands!");
          return new SqlAssignBranch(
              pos,
              (SqlIdentifier) operands[0],
              ((SqlLiteral) operands[1]).symbolValue(ReferenceType.class),
              (SqlIdentifier) operands[2],
              (SqlIdentifier) operands[3]);
        }
      };

  private final SqlIdentifier branchName;
  private final ReferenceType refType;
  private final SqlIdentifier reference;

  public SqlAssignBranch(
      SqlParserPos pos,
      SqlIdentifier branchName,
      ReferenceType refType,
      SqlIdentifier reference,
      SqlIdentifier source) {
    super(pos, source);
    this.branchName = branchName;
    this.refType = refType;
    this.reference = reference;
  }

  @Override
  public SqlOperator getOperator() {
    return OPERATOR;
  }

  @Override
  public List<SqlNode> getOperandList() {
    List<SqlNode> ops = Lists.newArrayList();
    ops.add(branchName);
    ops.add(SqlLiteral.createSymbol(refType, SqlParserPos.ZERO));
    ops.add(reference);
    ops.add(getSourceName());

    return ops;
  }

  @Override
  public void unparse(SqlWriter writer, int leftPrec, int rightPrec) {
    writer.keyword("ALTER");
    writer.keyword("BRANCH");
    branchName.unparse(writer, leftPrec, rightPrec);

    writer.keyword("ASSIGN");
    switch (refType) {
      case BRANCH:
        writer.keyword("BRANCH");
        break;
      case TAG:
        writer.keyword("TAG");
        break;
    }
    reference.unparse(writer, leftPrec, rightPrec);

    writer.keyword("IN");
    getSourceName().unparse(writer, leftPrec, rightPrec);
  }

  @Override
  public SqlDirectHandler<?> toDirectHandler(QueryContext context) {
    try {
      final Class<?> cl = Class.forName("com.dremio.exec.planner.sql.handlers.AssignBranchHandler");
      final Constructor<?> ctor = cl.getConstructor(QueryContext.class);

      return (SqlDirectHandler<?>) ctor.newInstance(context);
    } catch (ClassNotFoundException e) {
      throw UserException.unsupportedError(e)
          .message("ALTER BRANCH ASSIGN action is not supported.")
          .buildSilently();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  public SqlIdentifier getBranchName() {
    return branchName;
  }

  public SqlIdentifier getReference() {
    return reference;
  }
}
