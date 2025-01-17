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

package com.facebook.buck.shell;

import com.facebook.buck.android.toolchain.AndroidTools;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.HasOutputName;
import com.facebook.buck.core.model.impl.BuildPaths;
import com.facebook.buck.core.model.impl.BuildTargetPaths;
import com.facebook.buck.core.rules.BuildRule;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.rules.common.BuildableSupport;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.rules.args.WriteToFileArg;
import com.facebook.buck.rules.coercer.SourceSet;
import com.facebook.buck.rules.modern.ModernBuildRule;
import com.facebook.buck.sandbox.SandboxExecutionStrategy;
import com.facebook.buck.sandbox.SandboxProperties;
import com.facebook.buck.util.environment.Platform;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Build rule for generating a file via a shell command. For example, to generate the katana
 * AndroidManifest.xml from the wakizashi AndroidManifest.xml, such a rule could be defined as:
 *
 * <pre>
 * genrule(
 *   name = 'katana_manifest',
 *   srcs = [
 *     'wakizashi_to_katana_manifest.py',
 *     'AndroidManifest.xml',
 *   ],
 *   cmd = 'python wakizashi_to_katana_manifest.py ${SRCDIR}/AndroidManfiest.xml &gt; $OUT',
 *   out = 'AndroidManifest.xml',
 * )
 * </pre>
 *
 * The output of this rule would likely be used as follows:
 *
 * <pre>
 * android_binary(
 *   name = 'katana',
 *   manifest = ':katana_manifest',
 *   deps = [
 *     # Additional dependent android_library rules would be listed here, as well.
 *   ],
 * )
 * </pre>
 *
 * A <code>genrule</code> is evaluated by running the shell command specified by {@code cmd} with
 * the following environment variable substitutions:
 *
 * <ul>
 *   <li><code>SRCS</code> will be a space-delimited string expansion of the <code>srcs</code>
 *       attribute where each element of <code>srcs</code> will be translated into an absolute path.
 *   <li><code>SRCDIR</code> will be a directory containing all files mentioned in the srcs.
 *   <li><code>TMP</code> will be a temporary directory which can be used for intermediate results
 *   <li><code>OUT</code> is the output file for the <code>genrule()</code>. The file specified by
 *       this variable must always be written by this command. If not, the execution of this rule
 *       will be considered a failure, halting the build process.
 * </ul>
 *
 * In the above example, if the {@code katana_manifest} rule were defined in the {@code
 * src/com/facebook/wakizashi} directory, then the command that would be executed would be:
 *
 * <pre>
 * python convert_to_katana.py src/com/facebook/wakizashi/AndroidManifest.xml &gt; \
 *     buck-out/gen/src/com/facebook/wakizashi/AndroidManifest.xml
 * </pre>
 *
 * Note that {@code cmd} could be run on either Mac or Linux, so it should contain logic that works
 * on either platform. If this becomes an issue in the future (or we want to support building on
 * different platforms), then we could introduce a new attribute that is a map of target platforms
 * to the appropriate build command for that platform.
 *
 * <p>Note that the <code>SRCDIR</code> is populated by symlinking the sources.
 */
public class Genrule extends ModernBuildRule<GenruleBuildable> implements HasOutputName {
  protected Genrule(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      BuildRuleResolver buildRuleResolver,
      SandboxExecutionStrategy sandboxExecutionStrategy,
      SourceSet srcs,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe,
      Optional<String> type,
      String out,
      boolean enableSandboxingInGenrule,
      boolean isCacheable,
      Optional<String> environmentExpansionSeparator,
      Optional<AndroidTools> androidTools) {
    super(
        buildTarget,
        projectFilesystem,
        buildRuleResolver,
        new GenruleBuildable(
            buildTarget,
            projectFilesystem,
            sandboxExecutionStrategy,
            srcs,
            cmd,
            bash,
            cmdExe,
            type,
            out,
            enableSandboxingInGenrule,
            isCacheable,
            environmentExpansionSeparator.orElse(" "),
            enableSandboxingInGenrule
                ? Optional.of(
                    createSandboxConfiguration(
                        buildRuleResolver, buildTarget, projectFilesystem, srcs, cmd, bash, cmdExe))
                : Optional.empty(),
            androidTools.map(
                tools -> GenruleAndroidTools.of(tools, buildTarget, buildRuleResolver))));
  }

  @Override
  public SourcePath getSourcePathToOutput() {
    return getSourcePath(getBuildable().getOutput());
  }

  @Override
  public String getType() {
    return super.getType() + (getBuildable().type.map(typeStr -> "_" + typeStr).orElse(""));
  }

  /** Get the output name of the generated file, as listed in the BUCK file. */
  @Override
  public String getOutputName() {
    return getBuildable().out;
  }

  /** Get whether or not the output of this genrule can be cached. */
  @Override
  public final boolean isCacheable() {
    return getBuildable().isCacheable;
  }

  /**
   * Constructs a {@link SandboxProperties} for this genrule. There is not a good place for this
   * method because it sits at the intersection of two different implementation details:
   *
   * <ul>
   *   <li>The sandbox needs to know what filepaths this genrule is allowed to read and write to,
   *       which is itself an implementation detail of GenruleBuildable
   *   <li>The sandbox needs to know what targets this genrule depends on, since the genrule must
   *       then be allowed to read from the filepaths derived from dependent targets
   * </ul>
   *
   * Given this unfortunate collision, this method encodes the implementation details of the genrule
   * here and utilizes a {@link BuildRuleResolver} for the second detail. This works fine in
   * practice because this method uses the same underlying methods to calculate file paths as {@link
   * ModernBuildRule} and {@link GenruleBuildable}.
   */
  private static SandboxProperties createSandboxConfiguration(
      BuildRuleResolver buildRuleResolver,
      BuildTarget buildTarget,
      ProjectFilesystem filesystem,
      SourceSet srcs,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe) {
    SourcePathResolverAdapter sourcePathResolverAdapter = buildRuleResolver.getSourcePathResolver();

    // Note: this is a bit of a kludge to accommodate the need for a sandbox to know the structure
    // of GenruleBuildable's filesystem, which would ideally be an implementation detail of
    // ModernBuildRule and GenruleBuildable. This is the same calculation that MBR/GenruleBuildable
    // would do, so it works.
    Path pathToSrcDirectory = BuildPaths.getScratchDir(filesystem, buildTarget).resolve("srcs");
    Path pathToTmpDirectory = BuildPaths.getScratchDir(filesystem, buildTarget);
    Path pathToOutDirectory = BuildPaths.getGenDir(filesystem, buildTarget);
    Path pathToPublicOutDirectory = BuildTargetPaths.getGenPath(filesystem, buildTarget, "%s");

    SandboxProperties.Builder builder = SandboxProperties.builder();
    return builder
        .addAllAllowedToReadPaths(
            srcs.getPaths().stream()
                .map(sourcePathResolverAdapter::getAbsolutePath)
                .map(Object::toString)
                .collect(Collectors.toList()))
        .addAllAllowedToReadPaths(
            collectReadablePathsFromArguments(
                sourcePathResolverAdapter, buildRuleResolver, cmd, bash, cmdExe))
        .addAllowedToReadPaths(
            filesystem.resolve(pathToSrcDirectory).toString(),
            filesystem.resolve(pathToTmpDirectory).toString(),
            filesystem.resolve(pathToOutDirectory).toString(),
            filesystem.resolve(pathToPublicOutDirectory).toString(),
            filesystem.resolve(WriteToFileArg.getMacroPath(filesystem, buildTarget)).toString())
        .addAllowedToReadMetadataPaths(filesystem.getRootPath().toAbsolutePath().toString())
        .addAllowedToWritePaths(
            filesystem.resolve(pathToTmpDirectory).toString(),
            filesystem.resolve(pathToOutDirectory).toString(),
            filesystem.resolve(pathToPublicOutDirectory).toString())
        .addDeniedToReadPaths(filesystem.getRootPath().toAbsolutePath().toString())
        .build();
  }

  private static ImmutableList<String> collectReadablePathsFromArguments(
      SourcePathResolverAdapter resolver,
      BuildRuleResolver buildRuleResolver,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe) {
    ImmutableList.Builder<String> paths = ImmutableList.builder();
    cmd.ifPresent(c -> paths.addAll(collectExistingArgInputs(resolver, buildRuleResolver, c)));
    if (Platform.detect() == Platform.WINDOWS) {
      cmdExe.ifPresent(c -> paths.addAll(collectExistingArgInputs(resolver, buildRuleResolver, c)));
    } else {
      bash.ifPresent(c -> paths.addAll(collectExistingArgInputs(resolver, buildRuleResolver, c)));
    }
    return paths.build();
  }

  private static ImmutableList<String> collectExistingArgInputs(
      SourcePathResolverAdapter sourcePathResolverAdapter,
      BuildRuleResolver buildRuleResolver,
      Arg arg) {
    Collection<BuildRule> buildRules = BuildableSupport.getDepsCollection(arg, buildRuleResolver);
    ImmutableList.Builder<String> inputs = ImmutableList.builder();
    for (BuildRule buildRule : buildRules) {
      SourcePath inputPath = buildRule.getSourcePathToOutput();
      if (inputPath != null) {
        inputs.add(sourcePathResolverAdapter.getAbsolutePath(inputPath).toString());
      }
    }
    return inputs.build();
  }
}
