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

package com.facebook.buck.features.js;

import com.facebook.buck.android.AndroidBuckConfig;
import com.facebook.buck.core.config.BuckConfig;
import com.facebook.buck.core.description.Description;
import com.facebook.buck.core.description.DescriptionCreationContext;
import com.facebook.buck.core.model.targetgraph.DescriptionProvider;
import com.facebook.buck.core.toolchain.ToolchainProvider;
import com.facebook.buck.util.environment.Platform;
import java.util.Arrays;
import java.util.Collection;
import org.pf4j.Extension;

@Extension
public class JsDescriptionsProvider implements DescriptionProvider {

  @Override
  public Collection<Description<?>> getDescriptions(DescriptionCreationContext context) {
    ToolchainProvider toolchainProvider = context.getToolchainProvider();

    BuckConfig config = context.getBuckConfig();
    AndroidBuckConfig androidBuckConfig = new AndroidBuckConfig(config, Platform.detect());

    return Arrays.asList(
        new JsLibraryDescription(),
        new JsBundleGenruleDescription(toolchainProvider, context.getSandboxExecutionStrategy()),
        new JsBundleDescription(toolchainProvider, androidBuckConfig));
  }
}
