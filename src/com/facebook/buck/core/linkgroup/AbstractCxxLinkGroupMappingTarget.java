/*
 * Copyright 2014-present Facebook, Inc.
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

package com.facebook.buck.core.linkgroup;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.rulekey.AddToRuleKey;
import com.facebook.buck.core.util.immutables.BuckStyleImmutable;
import com.google.common.collect.ComparisonChain;
import java.util.Optional;
import java.util.regex.Pattern;
import org.immutables.value.Value;

/**
 * Represents how a single build target should be mapped to a link group.
 *
 * <p>When used in BUCK files, it would be expressed as:
 *
 * <pre>
 *   link_group_map = [
 *     ("...", [("//Some:Target", "tree")]),
 *   ],
 * </pre>
 *
 * In this case, {@link AbstractCxxLinkGroupMappingTarget} represents the tuple <code>
 * ("//Some:Target", "tree")</code>.
 */
@Value.Immutable(copy = true)
@BuckStyleImmutable
abstract class AbstractCxxLinkGroupMappingTarget
    implements Comparable<AbstractCxxLinkGroupMappingTarget> {

  /**
   * Defines how nodes should be included starting from the root as specified by the build target.
   */
  public enum Traversal {
    /** The target and all of its transitive dependencies are included in the link group. */
    TREE,
    /** The target is included in the link group. */
    NODE,
  }

  @Value.Parameter
  @AddToRuleKey
  public abstract BuildTarget getBuildTarget();

  @Value.Parameter
  @AddToRuleKey
  public abstract Traversal getTraversal();

  @Value.Parameter
  @AddToRuleKey
  public abstract Optional<Pattern> getLabelPattern();

  @Override
  public int compareTo(AbstractCxxLinkGroupMappingTarget that) {
    if (this == that) {
      return 0;
    }

    int labelComparison = compareLabelPattern(that);
    if (labelComparison != 0) {
      return labelComparison;
    }

    return ComparisonChain.start()
        .compare(this.getBuildTarget(), that.getBuildTarget())
        .compare(this.getTraversal(), that.getTraversal())
        .result();
  }

  private int compareLabelPattern(AbstractCxxLinkGroupMappingTarget that) {
    Optional<Pattern> thisLabelPattern = this.getLabelPattern();
    Optional<Pattern> thatLabelPattern = that.getLabelPattern();

    if (thisLabelPattern.isPresent() == thatLabelPattern.isPresent()) {
      if (thisLabelPattern.isPresent()) {
        return thisLabelPattern.get().pattern().compareTo(thatLabelPattern.get().pattern());
      }

      return 0;
    }

    if (!thisLabelPattern.isPresent()) {
      return -1;
    }

    return 1;
  }
}
