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

package com.facebook.buck.jvm.java.toolchain;

import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.core.toolchain.Toolchain;
import com.facebook.buck.core.toolchain.ToolchainProvider;
import com.facebook.buck.core.util.immutables.BuckStyleImmutable;
import com.facebook.buck.jvm.java.JavaOptions;
import com.facebook.buck.util.MoreFunctions;
import java.util.function.Function;
import org.immutables.value.Value;

@Value.Immutable(builder = false, copy = false)
@BuckStyleImmutable
public abstract class AbstractJavaOptionsProvider implements Toolchain {
  public static final String DEFAULT_NAME = "java-options";

  @Override
  public String getName() {
    return DEFAULT_NAME;
  }

  @Value.Parameter
  abstract JavaOptions getJavaOptions();

  @Value.Parameter
  abstract JavaOptions getJavaOptionsForTests();

  private static JavaOptionsProvider getDefault(
      ToolchainProvider provider, TargetConfiguration toolchainTargetConfiguration) {
    return provider.getByName(
        DEFAULT_NAME, toolchainTargetConfiguration, JavaOptionsProvider.class);
  }

  public static Function<TargetConfiguration, JavaOptions> getDefaultJavaOptions(
      ToolchainProvider provider) {
    return MoreFunctions.memoize(c -> getDefault(provider, c).getJavaOptions());
  }

  public static Function<TargetConfiguration, JavaOptions> getDefaultJavaOptionsForTests(
      ToolchainProvider provider) {
    return MoreFunctions.memoize(c -> getDefault(provider, c).getJavaOptionsForTests());
  }
}
