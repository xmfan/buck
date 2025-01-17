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

package com.facebook.buck.core.rules;

import com.facebook.buck.core.sourcepath.BuildTargetSourcePath;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.stream.Stream;

public interface SourcePathRuleFinder {

  ImmutableSet<BuildRule> filterBuildRuleInputs(Iterable<? extends SourcePath> sources);

  ImmutableSet<BuildRule> filterBuildRuleInputs(SourcePath... sources);

  Stream<BuildRule> filterBuildRuleInputs(Stream<SourcePath> sources);

  Stream<BuildRule> filterBuildRuleInputs(Optional<SourcePath> sourcePath);

  /**
   * @return An {@link Optional} containing the {@link BuildRule} whose output {@code sourcePath}
   *     refers to, or {@code absent} if {@code sourcePath} doesn't refer to the output of a {@link
   *     BuildRule}.
   */
  Optional<BuildRule> getRule(SourcePath sourcePath);

  /** @return The {@link BuildRule} whose output {@code sourcePath} refers to its output. */
  BuildRule getRule(BuildTargetSourcePath sourcePath);

  SourcePathResolverAdapter getSourcePathResolver();
}
