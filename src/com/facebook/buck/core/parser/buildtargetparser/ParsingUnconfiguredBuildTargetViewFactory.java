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
package com.facebook.buck.core.parser.buildtargetparser;

import com.facebook.buck.core.cell.CellPathResolver;
import com.facebook.buck.core.model.UnconfiguredBuildTargetView;
import com.facebook.buck.core.model.UnflavoredBuildTargetView;
import com.facebook.buck.core.path.ForwardRelativePath;

/** A factory that parses a given build target name using the provided {@link BuildTargetParser}. */
public class ParsingUnconfiguredBuildTargetViewFactory
    implements UnconfiguredBuildTargetViewFactory {

  private final BuildTargetParser buildTargetParser = BuildTargetParser.INSTANCE;

  @Override
  public UnconfiguredBuildTargetView create(
      CellPathResolver cellPathResolver, String buildTargetName) {
    return buildTargetParser.parse(cellPathResolver, buildTargetName, "", false);
  }

  @Override
  public UnconfiguredBuildTargetView createForBaseName(
      CellPathResolver cellPathResolver, String baseName, String buildTargetName) {
    return buildTargetParser.parse(cellPathResolver, buildTargetName, baseName, false);
  }

  @Override
  public UnconfiguredBuildTargetView createForPathRelativeToProjectRoot(
      CellPathResolver cellPathResolver,
      ForwardRelativePath pathRelativeToProjectRoot,
      String buildTargetName) {
    String baseName =
        UnflavoredBuildTargetView.BUILD_TARGET_PREFIX + pathRelativeToProjectRoot.toString();

    return createForBaseName(cellPathResolver, baseName, buildTargetName);
  }

  @Override
  public UnconfiguredBuildTargetView createWithWildcard(
      CellPathResolver cellPathResolver, String buildTargetName) {
    return buildTargetParser.parse(cellPathResolver, buildTargetName, "", true);
  }
}
