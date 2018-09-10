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

import org.apache.arrow.vector.types.pojo.Field;

import com.dremio.common.expression.CompleteType;
import com.terracottatech.store.Type;
import com.terracottatech.store.definition.CellDefinition;

public class TerracottaDBArrowTypeMapper {

  public static Field mapCellDefinitionToField(CellDefinition<?> cellDefinition) {
    String name = cellDefinition.name();
    switch (cellDefinition.type().asEnum()) {
      case CHAR:
        return CompleteType.VARCHAR.toField(name);
      case INT:
        return CompleteType.INT.toField(name);
      case BOOL:
        return CompleteType.BIT.toField(name);
      case LONG:
        return CompleteType.BIGINT.toField(name);
      case DOUBLE:
        return CompleteType.DOUBLE.toField(name);
      case STRING:
        return CompleteType.VARCHAR.toField(name);
      case BYTES:
        return CompleteType.VARBINARY.toField(name);
      default:
        throw new AssertionError("Unsupported type");
    }
  }

}
