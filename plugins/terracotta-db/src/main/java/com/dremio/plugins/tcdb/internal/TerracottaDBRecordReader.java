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

import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.NullableBigIntVector;
import org.apache.arrow.vector.NullableBitVector;
import org.apache.arrow.vector.NullableFloat8Vector;
import org.apache.arrow.vector.NullableIntVector;
import org.apache.arrow.vector.NullableVarBinaryVector;
import org.apache.arrow.vector.NullableVarCharVector;
import org.apache.arrow.vector.ValueVector;
import org.apache.arrow.vector.complex.NullableMapVector;
import org.apache.arrow.vector.complex.impl.VectorContainerWriter;
import org.apache.arrow.vector.types.pojo.Field;

import com.dremio.common.exceptions.ExecutionSetupException;
import com.dremio.common.exceptions.UserException;
import com.dremio.common.expression.CompleteType;
import com.dremio.common.expression.SchemaPath;
import com.dremio.exec.store.AbstractRecordReader;
import com.dremio.exec.vector.complex.fn.WorkingBuffer;
import com.dremio.sabot.exec.context.OperatorContext;
import com.dremio.sabot.op.scan.OutputMutator;
import com.dremio.service.namespace.dataset.proto.DatasetField;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetReader;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.Type;
import com.terracottatech.store.definition.CellDefinition;
import com.terracottatech.store.definition.IntCellDefinition;
import com.terracottatech.store.definition.LongCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;
import com.terracottatech.store.manager.DatasetManager;
import com.terracottatech.store.stream.RecordStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TerracottaDBRecordReader extends AbstractRecordReader {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TerracottaDBRecordReader.class);

  private final Dataset<?> dataset;
  private final DatasetReader<?> datasetReader;

  private VectorContainerWriter complexWriter;
  private final WorkingBuffer workingBuffer;
  private Map<CellDefinition<?>, ValueVector> columnMap;

  public TerracottaDBRecordReader(OperatorContext context, List<SchemaPath> columns, TerracottaDBSubScanSpec subScanSpec,
                                  DatasetManager datasetManager) {
    super(context, columns);
    this.workingBuffer = new WorkingBuffer(context.getManagedBuffer());
    String tableName = subScanSpec.getTableName();
    Type type = null;
    try {
      type = datasetManager.listDatasets().get(tableName);
      this.dataset = datasetManager.getDataset(tableName, type);
    } catch (StoreException e) {
      throw UserException.dataReadError(e).message("Failure while connecting to Terracotta.").build(logger);
    }
    this.datasetReader = dataset.reader();
  }

  @Override
  public void setup(OutputMutator outputMutator) throws ExecutionSetupException {
    complexWriter = new VectorContainerWriter(outputMutator);
    complexWriter.allocate();
    complexWriter.reset();

    columnMap = new LinkedHashMap<>();
    datasetReader.records()
      .limit(1)
      .flatMap(record -> record.stream())
      .map(cell -> cell.definition())
      .forEach(cellDef -> addFieldsToOutput(outputMutator, cellDef));
  }

  Field getField(String name, Type<?> type) {
    switch (type.asEnum()) {
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

  void addFieldsToOutput(OutputMutator outputMutator, CellDefinition<?> cellDefinition) {
    String name = cellDefinition.name();
    Field field;
    Class<? extends ValueVector> vectorClass;
    switch (cellDefinition.type().asEnum()) {
      case CHAR:
        field = CompleteType.VARCHAR.toField(name);
        vectorClass = NullableVarCharVector.class;
        break;
      case INT:
        field = CompleteType.INT.toField(name);
        vectorClass = NullableIntVector.class;
        break;
      case BOOL:
        field = CompleteType.BIT.toField(name);
        vectorClass = NullableBitVector.class;
        break;
      case LONG:
        field = CompleteType.BIGINT.toField(name);
        vectorClass = NullableBigIntVector.class;
        break;
      case DOUBLE:
        field = CompleteType.DOUBLE.toField(name);
        vectorClass = NullableFloat8Vector.class;
        break;
      case STRING:
        field = CompleteType.VARCHAR.toField(name);
        vectorClass = NullableVarCharVector.class;
        break;
      case BYTES:
        field = CompleteType.VARBINARY.toField(name);
        vectorClass = NullableVarBinaryVector.class;
        break;
      default:
        throw new AssertionError("Unsupported type");
    }
    ValueVector valueVector = outputMutator.addField(field, vectorClass);
    valueVector.allocateNew();
    columnMap.put(cellDefinition, valueVector);
  }


  @Override
  public int next() {
    for (ValueVector v : columnMap.values()) {
      v.clear();
      v.allocateNew();
    }

    datasetReader.records().limit(getNumRowsPerBatch()).forEach(record -> {
      AtomicInteger counter = new AtomicInteger(0);
      columnMap.forEach((cellDef, vector) -> {
        int index = counter.getAndIncrement();
        switch (cellDef.type().asEnum()) {
          case CHAR:
            //TODO Complete
            NullableVarCharVector typedVector = (NullableVarCharVector) vector;
            break;
          case INT:
            NullableIntVector intVector = (NullableIntVector) vector;
            IntCellDefinition intCellDefinition = (IntCellDefinition) cellDef;
            complexWriter.integer(cellDef.name()).writeInt(record.get(intCellDefinition).get());
            break;
          case BOOL:
            //TODO Complete
            NullableBitVector bitVector = (NullableBitVector) vector;
            break;
          case LONG:
            LongCellDefinition longCellDefinition = (LongCellDefinition) cellDef;
            complexWriter.bigInt(cellDef.name()).writeBigInt(record.get(longCellDefinition).get());
            break;
          case DOUBLE:
            //TODO Complete
            NullableFloat8Vector float8Vector = (NullableFloat8Vector) vector;
          case STRING:
            NullableVarCharVector varCharVector = (NullableVarCharVector) vector;
            StringCellDefinition stringCellDefinition = (StringCellDefinition) cellDef;
            String stringVal = record.get(stringCellDefinition).get();
            try {
              complexWriter.varChar(cellDef.name()).writeVarChar(0, workingBuffer.prepareVarCharHolder(stringVal), workingBuffer.getBuf());
            } catch (IOException e) {
              throw new RuntimeException("Encoding failed for: " + stringVal);
            }
            break;
          case BYTES:
            //TODO Complete
            NullableVarBinaryVector varBinaryVector = (NullableVarBinaryVector) vector;
          default:
            throw new AssertionError("Unsupported type");
        }
      });
    });

    return 0;
  }

  @Override
  public void close() {

  }
}
