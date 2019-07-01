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
package com.facebook.buck.core.artifact;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.impl.BuildPaths;
import com.facebook.buck.core.rules.analysis.action.ActionAnalysisDataKey;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import java.nio.file.Path;

/**
 * Factory for managing and creating the various {@link Artifact}s for a specific {@link
 * BuildTarget}. All of the artifacts created by this instance of the factory will be rooted at the
 * package path as generated by {@link BuildPaths}.
 */
public class BuildArtifactFactory {

  protected final BuildTarget target;
  private final Path packagePath;

  protected BuildArtifactFactory(BuildTarget target, ProjectFilesystem filesystem) {
    this.target = target;
    this.packagePath = BuildPaths.getGenDir(filesystem, target);
  }

  /**
   * @param output the output {@link Path} relative to the package path for the current rule that
   *     the {@link com.facebook.buck.core.rules.actions.Action}s are being created for
   * @return a {@link DeclaredArtifact} for the given path
   * @throws ArtifactDeclarationException if the provided output path is invalid in some way
   */
  protected DeclaredArtifact createDeclaredArtifact(Path output)
      throws ArtifactDeclarationException {
    return ArtifactImpl.of(target, packagePath, output);
  }

  /**
   * @param key the {@link ActionAnalysisDataKey} corresponding to an {@link
   *     com.facebook.buck.core.rules.actions.Action} to bind the given {@link Artifact} to.
   * @param artifact the {@link Artifact} to bind
   * @return a {@link BuildArtifact} by binding the action to the given artifact
   */
  protected BuildArtifact bindtoBuildArtifact(ActionAnalysisDataKey key, Artifact artifact) {
    return artifact.asDeclared().materialize(key);
  }
}
