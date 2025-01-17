/*
 * Copyright 2017-present Facebook, Inc.
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

package com.facebook.buck.rules.coercer;

import com.facebook.buck.core.cell.CellPathResolver;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.path.ForwardRelativePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.macros.BaseLocationMacro;
import com.facebook.buck.rules.macros.LocationMacro;
import com.facebook.buck.util.types.Pair;
import java.util.Optional;

/** Base class for expanding {@link BaseLocationMacro}s to strings. */
abstract class AbstractLocationMacroTypeCoercer<T extends BaseLocationMacro>
    implements MacroTypeCoercer<T> {

  private final TypeCoercer<BuildTarget> buildTargetTypeCoercer;

  public AbstractLocationMacroTypeCoercer(TypeCoercer<BuildTarget> buildTargetTypeCoercer) {
    this.buildTargetTypeCoercer = buildTargetTypeCoercer;
  }

  @Override
  public boolean hasElementClass(Class<?>[] types) {
    return buildTargetTypeCoercer.hasElementClass(types);
  }

  @Override
  public void traverse(CellPathResolver cellRoots, T macro, TypeCoercer.Traversal traversal) {
    buildTargetTypeCoercer.traverse(cellRoots, macro.getTarget(), traversal);
  }

  protected Pair<BuildTarget, Optional<String>> coerceTarget(
      CellPathResolver cellRoots,
      ProjectFilesystem filesystem,
      ForwardRelativePath pathRelativeToProjectRoot,
      TargetConfiguration targetConfiguration,
      TargetConfiguration hostConfiguration,
      String arg)
      throws CoerceFailedException {
    LocationMacro.SplitResult parts = LocationMacro.splitSupplementaryOutputPart(arg);
    BuildTarget target =
        buildTargetTypeCoercer.coerce(
            cellRoots,
            filesystem,
            pathRelativeToProjectRoot,
            targetConfiguration,
            hostConfiguration,
            parts.target);
    return new Pair<>(target, parts.supplementaryOutput);
  }
}
