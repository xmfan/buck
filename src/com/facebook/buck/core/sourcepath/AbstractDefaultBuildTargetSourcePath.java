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

package com.facebook.buck.core.sourcepath;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.BuildTargetWithOutputs;
import com.facebook.buck.core.model.ImmutableBuildTargetWithOutputs;
import com.facebook.buck.core.model.OutputLabel;
import com.facebook.buck.core.util.immutables.BuckStyleTuple;
import com.google.common.hash.HashCode;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * A {@link BuildTargetSourcePath} which resolves to the default output of the {@link
 * com.facebook.buck.core.rules.BuildRule} referred to by its target.
 */
@BuckStyleTuple
@Value.Immutable(prehash = true)
public abstract class AbstractDefaultBuildTargetSourcePath implements BuildTargetSourcePath {

  public abstract BuildTargetWithOutputs getTargetWithOutputs();

  @Override
  public BuildTarget getTarget() {
    return getTargetWithOutputs().getBuildTarget();
  }

  /**
   * Returns a default {@link com.facebook.buck.core.sourcepath.BuildTargetSourcePath} with an empty
   * output label.
   */
  public static DefaultBuildTargetSourcePath of(BuildTarget target) {
    return DefaultBuildTargetSourcePath.of(
        ImmutableBuildTargetWithOutputs.of(target, OutputLabel.DEFAULT));
  }

  @Override
  @Value.Parameter(value = false)
  public abstract Optional<HashCode> getPrecomputedHash();

  @Override
  public int hashCode() {
    return getTargetWithOutputs().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof AbstractDefaultBuildTargetSourcePath)) {
      return false;
    }

    AbstractDefaultBuildTargetSourcePath that = (AbstractDefaultBuildTargetSourcePath) other;
    return getTargetWithOutputs().equals(that.getTargetWithOutputs());
  }

  @Override
  public String toString() {
    return String.valueOf(getTargetWithOutputs());
  }

  @Override
  public int compareTo(SourcePath other) {
    if (this == other) {
      return 0;
    }

    int classComparison = compareClasses(other);
    if (classComparison != 0) {
      return classComparison;
    }

    AbstractDefaultBuildTargetSourcePath that = (AbstractDefaultBuildTargetSourcePath) other;
    return getTargetWithOutputs().compareTo(that.getTargetWithOutputs());
  }
}
