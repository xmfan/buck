/*
 * Copyright 2012-present Facebook, Inc.
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

import com.facebook.buck.core.model.impl.ImmutableUnconfiguredBuildTargetView;
import com.facebook.buck.core.model.impl.ImmutableUnflavoredBuildTargetView;
import com.facebook.buck.support.cli.args.BuckCellArg;
import com.facebook.buck.util.stream.RichStream;
import com.google.common.base.Preconditions;

/**
 * Exposes some {@link com.facebook.buck.core.model.BuildTarget} logic that is only visible for
 * testing.
 */
public class BuildTargetFactory {

  private BuildTargetFactory() {
    // Utility class
  }

  public static BuildTarget newInstance(String fullyQualifiedName) {
    BuckCellArg arg = BuckCellArg.of(fullyQualifiedName);
    ImmutableCanonicalCellName cellName = ImmutableCanonicalCellName.of(arg.getCellName());
    String[] parts = arg.getBasePath().split(":");
    Preconditions.checkArgument(parts.length == 2);
    String[] nameAndFlavor = parts[1].split("#");
    if (nameAndFlavor.length != 2) {
      return ImmutableUnconfiguredBuildTargetView.of(
              ImmutableUnflavoredBuildTargetView.of(cellName, parts[0], parts[1]))
          .configure(UnconfiguredTargetConfiguration.INSTANCE);
    }
    String[] flavors = nameAndFlavor[1].split(",");
    return ImmutableUnconfiguredBuildTargetView.of(
            ImmutableUnflavoredBuildTargetView.of(cellName, parts[0], nameAndFlavor[0]),
            RichStream.from(flavors).map(InternalFlavor::of))
        .configure(UnconfiguredTargetConfiguration.INSTANCE);
  }

  public static BuildTarget newInstance(String baseName, String shortName) {
    BuckCellArg arg = BuckCellArg.of(baseName);
    return ImmutableUnconfiguredBuildTargetView.of(
            ImmutableUnflavoredBuildTargetView.of(
                ImmutableCanonicalCellName.of(arg.getCellName()), arg.getBasePath(), shortName))
        .configure(UnconfiguredTargetConfiguration.INSTANCE);
  }

  public static BuildTarget newInstance(String baseName, String shortName, Flavor... flavors) {
    BuckCellArg arg = BuckCellArg.of(baseName);
    return ImmutableUnconfiguredBuildTargetView.of(
            ImmutableUnflavoredBuildTargetView.of(
                ImmutableCanonicalCellName.of(arg.getCellName()), arg.getBasePath(), shortName),
            RichStream.from(flavors))
        .configure(UnconfiguredTargetConfiguration.INSTANCE);
  }
}
