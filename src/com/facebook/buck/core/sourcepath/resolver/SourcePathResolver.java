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

package com.facebook.buck.core.sourcepath.resolver;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public interface SourcePathResolver {
  <T> ImmutableMap<T, ImmutableSortedSet<Path>> getMappedPaths(Map<T, SourcePath> sourcePathMap);

  ProjectFilesystem getFilesystem(SourcePath sourcePath);

  ImmutableSortedSet<Path> getAbsolutePath(SourcePath sourcePath);

  ImmutableSortedSet<Path> getAllAbsolutePaths(Collection<? extends SourcePath> sourcePaths);

  ImmutableSortedSet<Path> getRelativePath(SourcePath sourcePath);

  ImmutableSortedSet<Path> getIdeallyRelativePath(SourcePath sourcePath);

  ImmutableMap<String, SourcePath> getSourcePathNames(
      BuildTarget target, String parameter, Iterable<SourcePath> sourcePaths);

  <T> ImmutableMap<String, T> getSourcePathNames(
      BuildTarget target,
      String parameter,
      Iterable<T> objects,
      Predicate<T> filter,
      Function<T, SourcePath> objectSourcePathFunction);

  String getSourcePathName(BuildTarget target, SourcePath sourcePath);

  ImmutableCollection<Path> filterInputsToCompareToOutput(Iterable<? extends SourcePath> sources);

  /**
   * @return {@link Path} instances to the given {@link SourcePath} that is relative to the given
   *     {@link ProjectFilesystem}
   */
  ImmutableSortedSet<Path> getRelativePath(
      ProjectFilesystem projectFilesystem, SourcePath sourcePath);

  /**
   * Creates a map where given source paths are resolved relatively to the given base path and
   * stored (as keys) with their absolute paths (as values).
   */
  ImmutableMap<Path, Path> createRelativeMap(Path basePath, Iterable<SourcePath> sourcePaths);
}
