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
package com.dremio.plugins.tcdb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.protostuff.ByteString;

import com.dremio.common.exceptions.UserException;
import com.dremio.exec.planner.logical.ViewTable;

import com.dremio.exec.server.SabotContext;
import com.dremio.exec.store.SchemaConfig;
import com.dremio.exec.store.StoragePlugin;
import com.dremio.exec.store.StoragePluginRulesFactory;
import com.dremio.service.namespace.NamespaceKey;
import com.dremio.service.namespace.SourceState;
import com.dremio.service.namespace.SourceTableDefinition;
import com.dremio.service.namespace.capabilities.SourceCapabilities;
import com.dremio.service.namespace.dataset.proto.DatasetConfig;
import com.google.common.collect.ImmutableList;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.Type;
import com.terracottatech.store.manager.DatasetManager;

/**
 * Nothing
 */
public class TerracottaDBStoragePlugin implements StoragePlugin {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TerracottaDBStoragePlugin.class);

  private final String name;
  private final SabotContext context;
  private final TerracottaDBConfig storeConfig;
  private DatasetManager datasetManager;


  public TerracottaDBStoragePlugin(String name, SabotContext context, TerracottaDBConfig storeConfig) {
    this.name = name;
    this.context = context;
    this.storeConfig = storeConfig;
  }

  public DatasetManager getDatasetManager() {
    return datasetManager;
  }

  @Override
  public Iterable<SourceTableDefinition> getDatasets(String user, boolean ignoreAuthErrors) throws Exception {
    return datasetManager.listDatasets().entrySet().stream()
      .map(dsEntry -> {
        NamespaceKey namespaceKey = new NamespaceKey(ImmutableList.of(name, dsEntry.getKey()));
        return new TerracottaDBTableBuilder(namespaceKey, datasetManager, context);
      })
      .collect(Collectors.toList());
  }

  @Override
  public SourceTableDefinition getDataset(NamespaceKey datasetPath, DatasetConfig oldDataset, boolean ignoreAuthErrors) throws Exception {
    return new TerracottaDBTableBuilder(datasetPath, datasetManager, context);
  }

  @Override
  public boolean containerExists(NamespaceKey key) {
    return true;
  }

  @Override
  public boolean datasetExists(NamespaceKey key) {
    return true;
  }

  @Override
  public boolean hasAccessPermission(String user, NamespaceKey key, DatasetConfig datasetConfig) {
    return true;
  }

  @Override
  public SourceState getState() {
    return SourceState.GOOD;
  }

  @Override
  public SourceCapabilities getSourceCapabilities() {
    return SourceCapabilities.NONE;
  }

  @Override
  public ViewTable getView(List<String> tableSchemaPath, SchemaConfig schemaConfig) {
    return null;
  }

  @Override
  public Class<? extends StoragePluginRulesFactory> getRulesFactoryClass() {
    return StoragePluginRulesFactory.NoOpPluginRulesFactory.class;
  }

  @Override
  public CheckResult checkReadSignature(ByteString key, DatasetConfig datasetConfig) throws Exception {
    return CheckResult.UNCHANGED;
  }

  @Override
  public void start() throws IOException {
    List<InetSocketAddress> hosts = storeConfig.hosts.stream()
      .map(host -> InetSocketAddress.createUnresolved(host.hostname, host.port))
      .collect(Collectors.toList());
    try {
      this.datasetManager = DatasetManager.clustered(hosts).build();
    } catch (StoreException e) {
      throw UserException.dataReadError(e).message("Failure while connecting to Terracotta.").build(logger);
    }
  }

  @Override
  public void close() throws Exception {
    datasetManager.close();
  }
}
