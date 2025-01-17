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

package com.facebook.buck.rules.macros;

import com.facebook.buck.core.macros.MacroException;
import com.facebook.buck.core.rules.BuildRule;
import com.facebook.buck.core.rules.tool.BinaryBuildRule;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.core.toolchain.tool.Tool;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.rules.args.ToolArg;

/** Resolves to the executable command for a build target referencing a {@link BinaryBuildRule}. */
public class ExecutableMacroExpander<M extends AbstractExecutableTargetOrHostMacro>
    extends BuildTargetMacroExpander<M> {
  private final Class<M> macroClass;

  public ExecutableMacroExpander(Class<M> macroClass) {
    this.macroClass = macroClass;
  }

  @Override
  public Class<M> getInputClass() {
    return macroClass;
  }

  protected Tool getTool(BuildRule rule) throws MacroException {
    if (!(rule instanceof BinaryBuildRule)) {
      throw new MacroException(
          String.format(
              "%s used in executable macro does not correspond to a binary rule",
              rule.getBuildTarget()));
    }
    return ((BinaryBuildRule) rule).getExecutableCommand();
  }

  @Override
  protected Arg expand(SourcePathResolverAdapter resolver, M ignored, BuildRule rule)
      throws MacroException {
    return ToolArg.of(getTool(rule));
  }
}
