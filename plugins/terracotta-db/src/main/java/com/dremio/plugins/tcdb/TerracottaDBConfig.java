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

import io.protostuff.Tag;

import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Provider;

import com.dremio.exec.catalog.StoragePluginId;
import com.dremio.exec.catalog.conf.ConnectionConf;
import com.dremio.exec.catalog.conf.Host;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.server.SabotContext;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Nothing
 */
@SourceType("TCDB")
public class TerracottaDBConfig extends ConnectionConf<TerracottaDBConfig, TerracottaDBStoragePlugin> {

  @NotEmpty
  @Tag(1)
  @JsonProperty("hostList")
  public List<Host> hosts;

  public TerracottaDBConfig() {
  }

  @Override
  public TerracottaDBStoragePlugin newPlugin(SabotContext context, String name, Provider<StoragePluginId> pluginIdProvider) {
    return new TerracottaDBStoragePlugin(name, context, this);
  }
}
