/*
 * Copyright 2019-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.buck.core.cell.nameresolver;

import com.facebook.buck.core.model.CanonicalCellName;
import com.facebook.buck.core.model.ImmutableCanonicalCellName;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Optional;

public class TestCellNameResolver extends DefaultCellNameResolver {
  private final ImmutableMap<Optional<String>, CanonicalCellName> mapping;

  public TestCellNameResolver(ImmutableMap<Optional<String>, CanonicalCellName> mapping) {
    this.mapping = mapping;
  }

  public TestCellNameResolver withAlias(String alias, String canonical) {
    return new TestCellNameResolver(
        ImmutableMap.<Optional<String>, CanonicalCellName>builder()
            .putAll(mapping)
            .put(
                Optional.of(alias),
                canonical.isEmpty()
                    ? CanonicalCellName.rootCell()
                    : ImmutableCanonicalCellName.of(Optional.of(canonical)))
            .build());
  }

  public static TestCellNameResolver forRoot(String... names) {
    ImmutableMap.Builder<Optional<String>, CanonicalCellName> mapBuilder =
        ImmutableMap.<Optional<String>, CanonicalCellName>builder()
            .put(Optional.empty(), CanonicalCellName.rootCell());
    Arrays.stream(names)
        .forEach(
            name ->
                mapBuilder.put(
                    Optional.of(name), ImmutableCanonicalCellName.of(Optional.of(name))));
    return new TestCellNameResolver(mapBuilder.build());
  }

  public static TestCellNameResolver forSecondary(
      String selfName, Optional<String> rootName, String... visibleRootNames) {
    ImmutableMap.Builder<Optional<String>, CanonicalCellName> mapBuilder =
        ImmutableMap.<Optional<String>, CanonicalCellName>builder()
            .put(Optional.empty(), ImmutableCanonicalCellName.of(Optional.of(selfName)));
    rootName.ifPresent(ignored -> mapBuilder.put(rootName, CanonicalCellName.rootCell()));
    Arrays.stream(visibleRootNames)
        .forEach(
            name ->
                mapBuilder.put(
                    Optional.of(name), ImmutableCanonicalCellName.of(Optional.of(name))));
    return new TestCellNameResolver(mapBuilder.build());
  }

  @Override
  public ImmutableMap<Optional<String>, CanonicalCellName> getKnownCells() {
    return mapping;
  }
}
