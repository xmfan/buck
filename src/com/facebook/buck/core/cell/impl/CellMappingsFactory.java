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
package com.facebook.buck.core.cell.impl;

import com.facebook.buck.core.cell.ImmutableDefaultNewCellPathResolver;
import com.facebook.buck.core.cell.NewCellPathResolver;
import com.facebook.buck.core.cell.nameresolver.CellNameResolver;
import com.facebook.buck.core.cell.nameresolver.ImmutableDefaultCellNameResolver;
import com.facebook.buck.core.model.CanonicalCellName;
import com.facebook.buck.core.model.ImmutableCanonicalCellName;
import com.facebook.buck.util.config.Config;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Utilities for creating {@link NewCellPathResolver} and {@link CellNameResolver}. */
public class CellMappingsFactory {
  private CellMappingsFactory() {}

  /**
   * Creates a {@link NewCellPathResolver} from the root cell's path and config. We currently
   * require that all cells appear in the root cell's config.
   */
  public static NewCellPathResolver create(Path rootPath, Config rootConfig) {
    // TODO(cjhopman): We should support cells that the root cell doesn't know about. To do that, we
    // should probably continue to compute this mapping first (because that's hardest to get wrong).
    // It would require here that we be able to traverse all the other cells in the build and look
    // at their buckconfigs. We could construct canonical names for newly discovered cells by adding
    // a namespacing of some sort (i.e. secondary#tertiary).
    ImmutableSortedMap<String, Path> cellMapping = getCellMapping(rootPath, rootConfig);

    Map<Path, CanonicalCellName> canonicalNameMap = new LinkedHashMap<>();

    canonicalNameMap.put(rootPath, CanonicalCellName.rootCell());
    cellMapping.forEach(
        (name, path) ->
            canonicalNameMap.computeIfAbsent(
                path, ignored -> ImmutableCanonicalCellName.of(Optional.of(name))));

    return new ImmutableDefaultNewCellPathResolver(ImmutableMap.copyOf(canonicalNameMap));
  }

  /** Creates a {@link CellNameResolver} for a cell. */
  public static CellNameResolver createCellNameResolver(
      Path cellPath, Config config, NewCellPathResolver cellPathResolver) {
    ImmutableSortedMap<String, Path> cellMapping = getCellMapping(cellPath, config);

    Map<Optional<String>, CanonicalCellName> builder = new LinkedHashMap<>();
    builder.put(Optional.empty(), cellPathResolver.getCanonicalCellName(cellPath));
    cellMapping.forEach(
        (name, path) ->
            builder.put(Optional.of(name), cellPathResolver.getCanonicalCellName(path)));
    return new ImmutableDefaultCellNameResolver(builder);
  }

  private static ImmutableSortedMap<String, Path> getCellMapping(Path cellRoot, Config cellConfig) {
    return ImmutableSortedMap.copyOf(
        DefaultCellPathResolver.getCellPathsFromConfigRepositoriesSection(
            cellRoot, cellConfig.get(DefaultCellPathResolver.REPOSITORIES_SECTION)));
  }
}
