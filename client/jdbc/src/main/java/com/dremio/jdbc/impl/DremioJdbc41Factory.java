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

package com.dremio.jdbc.impl;

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.client.DremioClient;
import com.dremio.exec.client.ServerMethod;
import com.dremio.exec.proto.UserProtos.BigDecimalMsg;
import com.dremio.exec.proto.UserProtos.CreatePreparedStatementResp;
import com.dremio.exec.proto.UserProtos.PreparedStatementParameterValue;
import com.dremio.exec.proto.UserProtos.RequestStatus;
import com.dremio.exec.proto.UserProtos.ResultColumnMetadata;
import com.dremio.exec.proto.UserProtos.TimeStamp;
import com.dremio.exec.rpc.RpcFuture;
import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.NClob;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.calcite.avatica.AvaticaConnection;
import org.apache.calcite.avatica.AvaticaParameter;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.Helper;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.Meta.Signature;
import org.apache.calcite.avatica.Meta.StatementHandle;
import org.apache.calcite.avatica.QueryState;

/**
 * Implementation of {@link net.hydromatic.avatica.AvaticaFactory} for Dremio and JDBC 4.1
 * (corresponds to JDK 1.7).
 */
// Note:  Must be public so net.hydromatic.avatica.UnregisteredDriver can
// (reflectively) call no-args constructor.
public class DremioJdbc41Factory extends DremioFactory {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(DremioJdbc41Factory.class);

  /** Creates a factory for JDBC version 4.1. */
  // Note:  Must be public so net.hydromatic.avatica.UnregisteredDriver can
  // (reflectively) call this constructor.
  public DremioJdbc41Factory() {
    this(4, 1);
  }

  /** Creates a JDBC factory with given major/minor version number. */
  protected DremioJdbc41Factory(int major, int minor) {
    super(major, minor);
  }

  @Override
  DremioConnectionImpl newConnection(
      DriverImpl driver, DremioFactory factory, String url, Properties info) throws SQLException {
    return new DremioConnectionImpl(driver, factory, url, info);
  }

  @Override
  public DremioDatabaseMetaDataImpl newDatabaseMetaData(AvaticaConnection connection) {
    return new DremioDatabaseMetaDataImpl(connection);
  }

  @Override
  public DremioStatementImpl newStatement(
      AvaticaConnection connection,
      StatementHandle h,
      int resultSetType,
      int resultSetConcurrency,
      int resultSetHoldability) {
    return new DremioStatementImpl(
        (DremioConnectionImpl) connection,
        h,
        resultSetType,
        resultSetConcurrency,
        resultSetHoldability);
  }

  @Override
  public DremioJdbc41PreparedStatement newPreparedStatement(
      AvaticaConnection connection,
      StatementHandle h,
      Meta.Signature signature,
      int resultSetType,
      int resultSetConcurrency,
      int resultSetHoldability)
      throws SQLException {
    DremioConnectionImpl dremioConnection = (DremioConnectionImpl) connection;
    DremioClient client = dremioConnection.getClient();
    if (dremioConnection.getConfig().isServerPreparedStatementDisabled()
        || !client.getSupportedMethods().contains(ServerMethod.PREPARED_STATEMENT)) {
      // fallback to client side prepared statement
      return new DremioJdbc41PreparedStatement(
          dremioConnection,
          h,
          signature,
          null,
          resultSetType,
          resultSetConcurrency,
          resultSetHoldability);
    }
    return newServerPreparedStatement(
        dremioConnection, h, signature, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  private DremioJdbc41PreparedStatement newServerPreparedStatement(
      DremioConnectionImpl connection,
      StatementHandle h,
      Meta.Signature signature,
      int resultSetType,
      int resultSetConcurrency,
      int resultSetHoldability)
      throws SQLException {
    String sql = signature.sql;

    try {
      RpcFuture<CreatePreparedStatementResp> respFuture =
          connection.getClient().createPreparedStatement(signature.sql);

      CreatePreparedStatementResp resp;
      try {
        resp = respFuture.get();
      } catch (InterruptedException e) {
        // Preserve evidence that the interruption occurred so that code higher up
        // on the call stack can learn of the interruption and respond to it if it
        // wants to.
        Thread.currentThread().interrupt();

        throw new SQLException("Interrupted", e);
      }

      final RequestStatus status = resp.getStatus();
      if (status != RequestStatus.OK) {
        final String errMsgFromServer = resp.getError() != null ? resp.getError().getMessage() : "";

        if (status == RequestStatus.TIMEOUT) {
          logger.error("Request timed out to create prepare statement: {}", errMsgFromServer);
          throw new SQLTimeoutException("Failed to create prepared statement: " + errMsgFromServer);
        }

        if (status == RequestStatus.FAILED) {
          logger.error("Failed to create prepared statement: {}", errMsgFromServer);
          throw new SQLException("Failed to create prepared statement: " + resp.getError());
        }

        logger.error(
            "Failed to create prepared statement. Unknown status: {}, Error: {}",
            status,
            errMsgFromServer);
        throw new SQLException(
            String.format(
                "Failed to create prepared statement. Unknown status: %s, Error: %s",
                status, errMsgFromServer));
      }

      // create and set avatica parameters into the signature from the parameters received from
      // the Dremio server.
      List<AvaticaParameter> parameters = new ArrayList<>();
      for (ResultColumnMetadata param : resp.getPreparedStatement().getParametersList()) {
        String parameterType = param.getDataType();
        if ("CHARACTER VARYING".equals(parameterType)) {
          parameterType = "VARCHAR";
        } else if ("BINARY VARYING".equals(parameterType)) {
          parameterType = "VARBINARY";
        }
        parameters.add(
            new AvaticaParameter(
                param.getSigned(),
                param.getPrecision(),
                param.getScale(),
                JDBCType.valueOf(parameterType).ordinal(),
                param.getDataType(),
                param.getDataType(),
                param.getColumnName()));
      }

      return new DremioJdbc41PreparedStatement(
          connection,
          h,
          new Signature(
              signature.columns,
              signature.sql,
              parameters,
              signature.internalParameters,
              signature.cursorFactory,
              signature.statementType),
          resp.getPreparedStatement(),
          resultSetType,
          resultSetConcurrency,
          resultSetHoldability);
    } catch (SQLException e) {
      throw e;
    } catch (RuntimeException e) {
      throw Helper.INSTANCE.createException("Error while preparing statement [" + sql + "]", e);
    } catch (Exception e) {
      throw Helper.INSTANCE.createException("Error while preparing statement [" + sql + "]", e);
    }
  }

  @Override
  public DremioResultSetImpl newResultSet(
      AvaticaStatement statement,
      QueryState state,
      Meta.Signature signature,
      TimeZone timeZone,
      Meta.Frame firstFrame)
      throws SQLException {
    final ResultSetMetaData metaData = newResultSetMetaData(statement, signature);
    return new DremioResultSetImpl(statement, state, signature, metaData, timeZone, firstFrame);
  }

  @Override
  public ResultSetMetaData newResultSetMetaData(
      AvaticaStatement statement, Meta.Signature signature) {
    return new DremioResultSetMetaDataImpl(statement, null, signature);
  }

  /** JDBC 4.1 version of {@link DremioPreparedStatementImpl}. */
  private static class DremioJdbc41PreparedStatement extends DremioPreparedStatementImpl {

    private static final String ERROR_MSG =
        "Parameter value has not been set properly for the index: %s";

    DremioJdbc41PreparedStatement(
        DremioConnectionImpl connection,
        StatementHandle h,
        Meta.Signature signature,
        com.dremio.exec.proto.UserProtos.PreparedStatement pstmt,
        int resultSetType,
        int resultSetConcurrency,
        int resultSetHoldability)
        throws SQLException {
      super(
          connection,
          h,
          signature,
          pstmt,
          resultSetType,
          resultSetConcurrency,
          resultSetHoldability);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
      throwIfClosed();
      super.setParamValue(
          parameterIndex,
          PreparedStatementParameterValue.newBuilder().setIsNullValue(true).build());
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("BOOLEAN")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex, PreparedStatementParameterValue.newBuilder().setBoolValue(x).build());
    }

    @Override
    public void setShort(int parameterIndex, short x) throws SQLException {
      throwIfClosed();
      switch (getParameter(parameterIndex).typeName) {
        case "NUMERIC":
        case "BIGINT":
        case "DECIMAL":
        case "INTEGER":
          super.setParamValue(
              parameterIndex, PreparedStatementParameterValue.newBuilder().setIntValue(x).build());
          break;
        default:
          throw UserException.validationError()
              .message(String.format(ERROR_MSG, (parameterIndex + 1)))
              .buildSilently();
      }
    }

    @Override
    public void setInt(int parameterIndex, int x) throws SQLException {
      throwIfClosed();
      switch (getParameter(parameterIndex).typeName) {
        case "NUMERIC":
        case "BIGINT":
        case "DECIMAL":
        case "INTEGER":
          super.setParamValue(
              parameterIndex, PreparedStatementParameterValue.newBuilder().setIntValue(x).build());
          break;
        default:
          throw UserException.validationError()
              .message(String.format(ERROR_MSG, (parameterIndex + 1)))
              .buildSilently();
      }
    }

    @Override
    public void setLong(int parameterIndex, long x) throws SQLException {
      throwIfClosed();
      switch (getParameter(parameterIndex).typeName) {
        case "NUMERIC":
        case "BIGINT":
        case "DECIMAL":
          super.setParamValue(
              parameterIndex, PreparedStatementParameterValue.newBuilder().setLongValue(x).build());
          break;
        default:
          throw UserException.validationError()
              .message(String.format(ERROR_MSG, (parameterIndex + 1)))
              .buildSilently();
      }
    }

    @Override
    public void setFloat(int parameterIndex, float x) throws SQLException {
      throwIfClosed();
      switch (getParameter(parameterIndex).typeName) {
        case "DOUBLE":
        case "FLOAT":
          super.setParamValue(
              parameterIndex,
              PreparedStatementParameterValue.newBuilder().setFloatValue(x).build());
          break;
        default:
          throw UserException.validationError()
              .message(String.format(ERROR_MSG, (parameterIndex + 1)))
              .buildSilently();
      }
    }

    @Override
    public void setDouble(int parameterIndex, double x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("DOUBLE")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex, PreparedStatementParameterValue.newBuilder().setDoubleValue(x).build());
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
      throwIfClosed();
      switch (getParameter(parameterIndex).typeName) {
        case "NUMERIC":
        case "BIGINT":
        case "DECIMAL":
          super.setParamValue(
              parameterIndex,
              PreparedStatementParameterValue.newBuilder()
                  .setBigDecimalValue(
                      BigDecimalMsg.newBuilder()
                          .setScale(x.scale())
                          .setPrecision(x.precision())
                          .setValue(ByteString.copyFrom(x.unscaledValue().toByteArray()))
                          .build())
                  .build());
          break;
        default:
          throw UserException.validationError()
              .message(String.format(ERROR_MSG, (parameterIndex + 1)))
              .buildSilently();
      }
    }

    @Override
    public void setString(int parameterIndex, String x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("CHARACTER VARYING")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex, PreparedStatementParameterValue.newBuilder().setStringValue(x).build());
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("BINARY VARYING")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex,
          PreparedStatementParameterValue.newBuilder()
              .setByteArrayValue(ByteString.copyFrom(x))
              .build());
    }

    @Override
    public void setDate(int parameterIndex, Date x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("DATE")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex,
          PreparedStatementParameterValue.newBuilder().setDateValue(x.getTime()).build());
    }

    @Override
    public void setTime(int parameterIndex, Time x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("TIME")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex,
          PreparedStatementParameterValue.newBuilder().setTimeValue(x.getTime()).build());
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
      throwIfClosed();
      if (!getParameter(parameterIndex).typeName.equals("TIMESTAMP")) {
        throw UserException.validationError()
            .message(String.format(ERROR_MSG, (parameterIndex + 1)))
            .buildSilently();
      }
      super.setParamValue(
          parameterIndex,
          PreparedStatementParameterValue.newBuilder()
              .setTimestampValue(
                  TimeStamp.newBuilder().setSeconds(x.getTime()).setNanos(x.getNanos()).build())
              .build());
    }

    @Override
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
      getSite(parameterIndex).setRowId(x);
    }

    @Override
    public void setNString(int parameterIndex, String value) throws SQLException {
      getSite(parameterIndex).setNString(value);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value, long length)
        throws SQLException {
      getSite(parameterIndex).setNCharacterStream(value, length);
    }

    @Override
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
      getSite(parameterIndex).setNClob(value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
      getSite(parameterIndex).setClob(reader, length);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException {
      getSite(parameterIndex).setBlob(inputStream, length);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
      getSite(parameterIndex).setNClob(reader, length);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
      getSite(parameterIndex).setSQLXML(xmlObject);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
      getSite(parameterIndex).setAsciiStream(x, length);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length)
        throws SQLException {
      getSite(parameterIndex).setBinaryStream(x, length);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, long length)
        throws SQLException {
      getSite(parameterIndex).setCharacterStream(reader, length);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
      getSite(parameterIndex).setAsciiStream(x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
      getSite(parameterIndex).setBinaryStream(x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
      getSite(parameterIndex).setCharacterStream(reader);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
      getSite(parameterIndex).setNCharacterStream(value);
    }

    @Override
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
      getSite(parameterIndex).setClob(reader);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
      getSite(parameterIndex).setBlob(inputStream);
    }

    @Override
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
      getSite(parameterIndex).setNClob(reader);
    }
  }
}

// End DremioJdbc41Factory.java
