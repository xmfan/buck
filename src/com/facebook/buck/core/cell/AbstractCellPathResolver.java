/*
 * Copyright 2018-present Facebook, Inc.
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

package com.facebook.buck.core.cell;

import com.facebook.buck.core.cell.exception.UnknownCellException;
import com.facebook.buck.core.model.CellRelativePath;
import com.facebook.buck.core.model.UnflavoredBuildTargetView;
import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.Path;
import java.util.Optional;

/** Contains base logic for {@link CellPathResolver}. */
public abstract class AbstractCellPathResolver implements CellPathResolver {

  /** @return sorted set of known roots in reverse natural order */
  @Override
  public ImmutableSortedSet<Path> getKnownRoots() {
    return ImmutableSortedSet.<Path>reverseOrder()
        .addAll(getCellPathsByRootCellExternalName().values())
        .add(getCellPathOrThrow(Optional.empty()))
        .build();
  }

  @Override
  public Path getCellPathOrThrow(Optional<String> cellName) {
    return getCellPath(cellName)
        .orElseThrow(
            () ->
                new UnknownCellException(cellName, getCellPathsByRootCellExternalName().keySet()));
  }

  @Override
  public Path getCellPathOrThrow(UnflavoredBuildTargetView buildTarget) {
    return getNewCellPathResolver().getCellPath(buildTarget.getCell());
  }

  @Override
  public Path resolveCellRelativePath(CellRelativePath cellRelativePath) {
    Path cellPath = getNewCellPathResolver().getCellPath(cellRelativePath.getCellName());
    return cellPath.resolve(cellRelativePath.getPath().toPath(cellPath.getFileSystem()));
  }
}
