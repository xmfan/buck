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

package com.facebook.buck.parser.manifest;

import com.facebook.buck.core.graph.transformation.model.ClassBasedComputationIdentifier;
import com.facebook.buck.core.graph.transformation.model.ComputationIdentifier;
import com.facebook.buck.core.graph.transformation.model.ComputeKey;
import com.facebook.buck.parser.api.BuildFileManifest;
import com.google.common.base.Preconditions;
import java.nio.file.Path;
import org.immutables.value.Value;

/** Transformation key containing path to a build file to parse */
@Value.Immutable(builder = false, copy = false, prehash = true)
public abstract class BuildPackagePathToBuildFileManifestKey
    implements ComputeKey<BuildFileManifest> {

  public static final ComputationIdentifier<BuildFileManifest> IDENTIFIER =
      ClassBasedComputationIdentifier.of(
          BuildPackagePathToBuildFileManifestKey.class, BuildFileManifest.class);

  /**
   * Path of the root of the package to parse, relative to some root (usually cell root). The
   * physical folder should contain build file.
   */
  @Value.Parameter
  public abstract Path getPath();

  @Value.Check
  protected void check() {
    Preconditions.checkArgument(!getPath().isAbsolute());
  }

  @Override
  public ComputationIdentifier<BuildFileManifest> getIdentifier() {
    return IDENTIFIER;
  }
}
