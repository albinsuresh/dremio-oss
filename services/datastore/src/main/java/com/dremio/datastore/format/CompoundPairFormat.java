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
package com.dremio.datastore.format;

import com.dremio.datastore.DatastoreFatalException;
import com.dremio.datastore.FormatVisitor;
import com.dremio.datastore.format.compound.KeyPair;

/**
 * Format representing a collection of other formats.
 *
 * <p>The order the keys appear in the type arguments and the constructor will match the order they
 * appear when serialized. The types and order of the keys may not be altered after being deployed.
 *
 * @param <K1> - The type of the first key.
 * @param <K2> - The type of the second key.
 */
public final class CompoundPairFormat<K1, K2> implements Format<KeyPair<K1, K2>> {

  private final String key1Name;
  private final Format<K1> key1Format;
  private final String key2Name;
  private final Format<K2> key2Format;

  CompoundPairFormat(
      String key1Name, Format<K1> key1Format, String key2Name, Format<K2> key2Format) {
    this.key1Name = key1Name;
    this.key1Format = key1Format;
    this.key2Name = key2Name;
    this.key2Format = key2Format;
  }

  @Override
  public Class<KeyPair<K1, K2>> getRepresentedClass() {
    return (Class) KeyPair.class;
  }

  @Override
  public <RET> RET apply(FormatVisitor<RET> visitor) throws DatastoreFatalException {
    return visitor.visitCompoundPairFormat(key1Name, key1Format, key2Name, key2Format);
  }
}
