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
package com.dremio.exec.planner.acceleration;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.when;

import com.dremio.exec.catalog.Catalog;
import com.dremio.exec.ops.PlannerCatalogImpl;
import com.dremio.exec.planner.sql.SqlConverter;
import com.dremio.exec.planner.types.SqlTypeFactoryImpl;
import com.dremio.exec.store.CatalogService;
import com.dremio.service.namespace.NamespaceKey;
import java.util.Arrays;
import java.util.Collections;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/** Tests for MaterializationExpander */
public class TestMaterializationExpander {
  private final RelDataTypeFactory typeFactory = SqlTypeFactoryImpl.INSTANCE;

  /** row types that only differ by field names should be equal */
  @Test
  public void testAreRowTypesEqualIgnoresNames() {
    final RelDataType type1 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.INTEGER),
                typeFactory.createSqlType(SqlTypeName.BIGINT),
                typeFactory.createSqlType(SqlTypeName.FLOAT),
                typeFactory.createSqlType(SqlTypeName.DOUBLE),
                typeFactory.createSqlType(SqlTypeName.DATE),
                typeFactory.createSqlType(SqlTypeName.TIMESTAMP),
                typeFactory.createSqlType(SqlTypeName.VARCHAR),
                typeFactory.createSqlType(SqlTypeName.BOOLEAN)),
            asList("intC", "bigIntC", "floatC", "doubleC", "dateC", "tsC", "varcharC", "boolC"));

    final RelDataType type2 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.INTEGER),
                typeFactory.createSqlType(SqlTypeName.BIGINT),
                typeFactory.createSqlType(SqlTypeName.FLOAT),
                typeFactory.createSqlType(SqlTypeName.DOUBLE),
                typeFactory.createSqlType(SqlTypeName.DATE),
                typeFactory.createSqlType(SqlTypeName.TIMESTAMP),
                typeFactory.createSqlType(SqlTypeName.VARCHAR),
                typeFactory.createSqlType(SqlTypeName.BOOLEAN)),
            asList(
                "intC2",
                "bigIntC2",
                "floatC2",
                "doubleC2",
                "dateC2",
                "tsC2",
                "varcharC2",
                "boolC2"));

    Assert.assertTrue(MaterializationExpander.areRowTypesEqual(type1, type2));
  }

  /** row types that only differ by nullability should be equal */
  @Test
  public void testAreRowTypesEqualIgnoresNullability() {
    final RelDataType type1 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.INTEGER),
                typeFactory.createSqlType(SqlTypeName.DOUBLE),
                typeFactory.createSqlType(SqlTypeName.TIMESTAMP),
                typeFactory.createSqlType(SqlTypeName.BOOLEAN)),
            asList("intC", "doubleC", "tsC", "boolC"));

    final RelDataType type2 =
        typeFactory.createStructType(
            asList(
                typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.INTEGER), true),
                typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.DOUBLE), true),
                typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.TIMESTAMP), true),
                typeFactory.createTypeWithNullability(
                    typeFactory.createSqlType(SqlTypeName.BOOLEAN), true)),
            asList("intC2", "doubleC2", "tsC2", "boolC2"));

    Assert.assertTrue(MaterializationExpander.areRowTypesEqual(type1, type2));
  }

  @Test
  public void testAreRowTypesEqualIgnoresArrayFieldNullability() {
    final RelDataType type1 =
        typeFactory.createStructType(
            asList(
                typeFactory.createArrayType(
                    typeFactory.createTypeWithNullability(
                        typeFactory.createSqlType(SqlTypeName.VARCHAR), true),
                    -1)),
            asList("regexpsplit"));

    final RelDataType type2 =
        typeFactory.createStructType(
            asList(
                typeFactory.createArrayType(
                    typeFactory.createTypeWithNullability(
                        typeFactory.createSqlType(SqlTypeName.VARCHAR), false),
                    -1)),
            asList("regexpsplit"));

    Assert.assertTrue(MaterializationExpander.areRowTypesEqual(type1, type2));
  }

  /** row types that only differ for fields with ANY types should be equal */
  @Test
  public void testAreRowTypesEqualIgnoresAny() {
    final RelDataType type1 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.INTEGER),
                typeFactory.createSqlType(SqlTypeName.BIGINT)),
            asList("intC", "bigIntC"));

    final RelDataType type2 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.INTEGER),
                typeFactory.createSqlType(SqlTypeName.ANY)),
            asList("intC", "anyC"));

    Assert.assertTrue(MaterializationExpander.areRowTypesEqual(type1, type2));
  }

  /** CHAR<>VARCHAR fields should be equivalent */
  @Test
  public void testAreRowTypesEqualMatchesCharVarchar() {
    final RelDataType type1 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.CHAR),
                typeFactory.createSqlType(SqlTypeName.VARCHAR)),
            asList("charC", "varcharC"));

    final RelDataType type2 =
        typeFactory.createStructType(
            asList(
                typeFactory.createSqlType(SqlTypeName.VARCHAR),
                typeFactory.createSqlType(SqlTypeName.CHAR)),
            asList("varcharC", "charC"));

    Assert.assertTrue(MaterializationExpander.areRowTypesEqual(type1, type2));
  }

  /** row types with TIMESTAMP fields with different precisions should be equal */
  @Test
  public void testTimestampPrecisionMismatch() {
    final RelDataType type1 =
        typeFactory.createStructType(
            Collections.singletonList(typeFactory.createSqlType(SqlTypeName.TIMESTAMP, 0)),
            Collections.singletonList("ts0"));

    final RelDataType type2 =
        typeFactory.createStructType(
            Collections.singletonList(typeFactory.createSqlType(SqlTypeName.TIMESTAMP, 3)),
            Collections.singletonList("ts3"));

    Assert.assertTrue(MaterializationExpander.areRowTypesEqual(type1, type2));
  }

  /**
   * Verifies that if catalog.getTable throws exception, MaterializationExpander handles it
   * gracefully as an ExpansionException.
   */
  @Test(expected = MaterializationExpander.ExpansionException.class)
  public void testExpansionExceptionOnGetTableException() {

    NamespaceKey key = new NamespaceKey(Arrays.asList("__accelerator", "r1", "m1"));
    SqlConverter converter = Mockito.mock(SqlConverter.class);
    CatalogService catalogService = Mockito.mock(CatalogService.class);
    PlannerCatalogImpl catalog = Mockito.mock(PlannerCatalogImpl.class);
    Catalog simpleCatalog = Mockito.mock(Catalog.class);
    when(catalog.getCatalog()).thenReturn(simpleCatalog);
    when(catalog.getValidatedTableWithSchema(key))
        .thenThrow(new IllegalStateException("Some underlying storage plugin issue"));
    when(converter.getPlannerCatalog()).thenReturn(catalog);
    MaterializationExpander expander = MaterializationExpander.of(converter, catalogService);
    try {
      expander.expandSchemaPath(key.getPathComponents());
    } catch (MaterializationExpander.ExpansionException e) {
      Assert.assertEquals(
          "Unable to get accelerator table: [__accelerator, r1, m1]", e.getMessage());
      Assert.assertEquals("Some underlying storage plugin issue", e.getCause().getMessage());
      throw e;
    }
    Assert.fail("Expected MaterializationExpander.ExpansionException");
  }
}
