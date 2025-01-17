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

package com.facebook.buck.rules.macros;

import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.util.immutables.BuckStyleTuple;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import org.immutables.value.Value;

/** Macro that resolves to the output location of a build rule. */
@Value.Immutable
@BuckStyleTuple
abstract class AbstractLocationMacro extends BaseLocationMacro {

  /** Shorthand for constructing a LocationMacro referring to the main output. */
  @VisibleForTesting
  public static LocationMacro of(BuildTarget buildTarget) {
    return LocationMacro.of(buildTarget, Optional.empty());
  }
}
