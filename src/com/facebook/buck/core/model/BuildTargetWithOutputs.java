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
package com.facebook.buck.core.model;

import org.immutables.value.Value;

/**
 * Wrapper for a build target and its output label.
 *
 * <p>For example, for the target {@code //:foo}, the build target would be {@code //:foo}, and the
 * output label would be empty.
 *
 * <p>For the target {@code //:foo[baz]}, the build target would be {@code //:foo}, and the output
 * label would be {@code baz}.
 *
 * <p>For the target {@code //:foo#flavor[baz]}, the build target would be {@code //:foo#flavor},
 * and the output label would be {@code baz}.
 *
 * <p>See also {@link UnconfiguredBuildTargetWithOutputs} for outputs with unconfigured build
 * targets.
 */
@Value.Immutable(prehash = true, builder = false)
public abstract class BuildTargetWithOutputs implements Comparable<BuildTargetWithOutputs> {
  @Value.Parameter
  /** Returns the associated {@link BuildTarget}. */
  public abstract BuildTarget getBuildTarget();

  @Value.Parameter
  /** Returns the output label associated with the build target, if any. */
  public abstract OutputLabel getOutputLabel();

  @Override
  public int compareTo(BuildTargetWithOutputs other) {
    if (this == other) {
      return 0;
    }

    int targetComparison = getBuildTarget().compareTo(other.getBuildTarget());
    if (targetComparison != 0) {
      return targetComparison;
    }

    return getOutputLabel().compareTo(other.getOutputLabel());
  }

  /**
   * Returns the string representation of a {@code BuildTargetWithOutputs} in the form of
   * target_name[output_label] if an output label is present. E.g. //foo:bar[baz]. If no output
   * label is present, the square brackets are omitted. E.g. //foo:bar
   */
  @Override
  public String toString() {
    return getOutputLabel()
        .getLabel()
        .map(ol -> String.format("%s[%s]", getBuildTarget(), ol))
        .orElse(getBuildTarget().getFullyQualifiedName());
  }
}
