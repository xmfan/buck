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

package com.facebook.buck.apple;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import com.facebook.buck.core.model.BuildTargetFactory;
import com.facebook.buck.core.rules.ActionGraphBuilder;
import com.facebook.buck.core.rules.BuildRule;
import com.facebook.buck.core.rules.resolver.impl.TestActionGraphBuilder;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.cxx.CxxBinary;
import com.facebook.buck.cxx.CxxLink;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.rules.macros.LocationMacro;
import com.facebook.buck.rules.macros.StringWithMacrosUtils;
import com.facebook.buck.shell.Genrule;
import com.facebook.buck.shell.GenruleBuilder;
import com.facebook.buck.util.environment.Platform;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import org.hamcrest.Matchers;
import org.junit.Test;

public class AppleBinaryDescriptionTest {
  @Test
  public void linkerFlagsLocationMacro() {
    assumeThat(Platform.detect(), is(Platform.MACOS));
    ActionGraphBuilder graphBuilder = new TestActionGraphBuilder();
    Genrule dep =
        GenruleBuilder.newGenruleBuilder(BuildTargetFactory.newInstance("//:dep"))
            .setOut("out")
            .build(graphBuilder);
    AppleBinaryBuilder builder =
        new AppleBinaryBuilder(BuildTargetFactory.newInstance("//:rule"))
            .setLinkerFlags(
                ImmutableList.of(
                    StringWithMacrosUtils.format(
                        "--linker-script=%s", LocationMacro.of(dep.getBuildTarget()))));
    assertThat(builder.build().getExtraDeps(), Matchers.hasItem(dep.getBuildTarget()));
    BuildRule binary = ((CxxBinary) builder.build(graphBuilder)).getLinkRule();
    assertThat(binary, Matchers.instanceOf(CxxLink.class));

    SourcePath outputSourcePath = dep.getSourcePathToOutput();
    Path absoluteLinkerScriptPath =
        graphBuilder.getSourcePathResolver().getAbsolutePath(outputSourcePath);
    assertThat(
        Arg.stringify(((CxxLink) binary).getArgs(), graphBuilder.getSourcePathResolver()),
        Matchers.hasItem(String.format("--linker-script=%s", absoluteLinkerScriptPath)));
    assertThat(binary.getBuildDeps(), Matchers.hasItem(dep));
  }
}
