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
import com.facebook.buck.core.rules.ActionGraphBuilder;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.rules.args.Arg;
import java.nio.file.Path;

/** Handles '$(output ...)' macro which expands to the path of a named supplementary output. */
public class OutputMacroExpander extends AbstractMacroExpanderWithoutPrecomputedWork<OutputMacro> {
  @Override
  public Arg expandFrom(BuildTarget target, ActionGraphBuilder graphBuilder, OutputMacro input) {
    return new OutputArg(input, graphBuilder, target);
  }

  @Override
  public Class<OutputMacro> getInputClass() {
    return OutputMacro.class;
  }

  private static class OutputArg extends AbstractOutputArg {

    OutputArg(OutputMacro input, BuildRuleResolver resolver, BuildTarget target) {
      super(input.getOutputName(), resolver, target);
    }

    @Override
    Path sourcePathToArgPath(SourcePath path, SourcePathResolverAdapter resolver) {
      return resolver.getRelativePath(path);
    }
  }
}
