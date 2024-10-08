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
package com.dremio.exec.store;

import com.google.common.collect.Lists;
import java.util.List;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;

/** RecordDataType defines names and data types of columns in a static table. */
public abstract class RecordDataType {

  /**
   * @return the {@link org.apache.calcite.sql.type.SqlTypeName} of columns in the table
   */
  public abstract List<SqlTypeName> getFieldSqlTypeNames();

  /**
   * @return the column names in the table
   */
  public abstract List<String> getFieldNames();

  /**
   * This method constructs a {@link org.apache.calcite.rel.type.RelDataType} based on the {@link
   * com.dremio.exec.store.RecordDataType}'s field sql types and field names.
   *
   * @param factory helps construct a {@link org.apache.calcite.rel.type.RelDataType}
   * @return the constructed type
   */
  public final RelDataType getRowType(RelDataTypeFactory factory) {
    final List<SqlTypeName> types = getFieldSqlTypeNames();
    final List<String> names = getFieldNames();
    final List<RelDataType> fields = Lists.newArrayList();
    for (final SqlTypeName typeName : types) {
      switch (typeName) {
        case VARCHAR:
          fields.add(factory.createSqlType(typeName, Integer.MAX_VALUE));
          break;
        default:
          fields.add(factory.createSqlType(typeName));
      }
    }
    return factory.createStructType(fields, names);
  }
}
