/*
 * Copyright 2016-present Facebook, Inc.
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
import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.path.ForwardRelativePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.versions.Constraint;
import com.facebook.buck.versions.ExactConstraint;
import com.facebook.buck.versions.Version;

public class ConstraintTypeCoercer extends LeafTypeCoercer<Constraint> {

  @Override
  public Class<Constraint> getOutputClass() {
    return Constraint.class;
  }

  @Override
  public Constraint coerce(
      CellPathResolver cellRoots,
      ProjectFilesystem filesystem,
      ForwardRelativePath pathRelativeToProjectRoot,
      TargetConfiguration targetConfiguration,
      TargetConfiguration hostConfiguration,
      Object object)
      throws CoerceFailedException {
    if (object instanceof String) {
      return ExactConstraint.of(Version.of((String) object));
    }
    throw CoerceFailedException.simple(object, getOutputClass());
  }
}
