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
package com.facebook.buck.shell;

import com.facebook.buck.core.build.context.BuildContext;
import com.facebook.buck.core.build.execution.context.ExecutionContext;
import com.facebook.buck.core.exceptions.HumanReadableException;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.impl.BuildTargetPaths;
import com.facebook.buck.core.rulekey.AddToRuleKey;
import com.facebook.buck.core.rulekey.CustomFieldBehavior;
import com.facebook.buck.core.rulekey.DefaultFieldInputs;
import com.facebook.buck.core.rulekey.ExcludeFromRuleKey;
import com.facebook.buck.core.sourcepath.SourcePath;
import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.io.BuildCellRelativePath;
import com.facebook.buck.io.file.MorePaths;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.rules.coercer.SourceSet;
import com.facebook.buck.rules.macros.WorkerMacroArg;
import com.facebook.buck.rules.modern.BuildCellRelativePathFactory;
import com.facebook.buck.rules.modern.Buildable;
import com.facebook.buck.rules.modern.CustomFieldInputs;
import com.facebook.buck.rules.modern.CustomFieldSerialization;
import com.facebook.buck.rules.modern.OutputPath;
import com.facebook.buck.rules.modern.OutputPathResolver;
import com.facebook.buck.rules.modern.PublicOutputPath;
import com.facebook.buck.rules.modern.RemoteExecutionEnabled;
import com.facebook.buck.rules.modern.ValueCreator;
import com.facebook.buck.rules.modern.ValueVisitor;
import com.facebook.buck.sandbox.NoSandboxExecutionStrategy;
import com.facebook.buck.sandbox.SandboxExecutionStrategy;
import com.facebook.buck.sandbox.SandboxProperties;
import com.facebook.buck.shell.programrunner.DirectProgramRunner;
import com.facebook.buck.shell.programrunner.ProgramRunner;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.facebook.buck.step.fs.SymlinkTreeStep;
import com.facebook.buck.worker.WorkerJobParams;
import com.facebook.buck.worker.WorkerProcessIdentity;
import com.facebook.buck.worker.WorkerProcessParams;
import com.facebook.buck.worker.WorkerProcessPoolFactory;
import com.facebook.buck.zip.ZipScrubberStep;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Buildable for {@link Genrule} suitable for building Genrules directly and also for subclasses
 * extending the functionality of a bare Genrule.
 */
public class GenruleBuildable implements Buildable {
  /**
   * Name of the "srcs" subdirectory in the gen directory tree. GenruleBuildable symlinks all source
   * files into this directory and sets this directory to be the working directory of the command.
   */
  protected static final String SRC_DIRECTORY_PATTERN = "%s__srcs";

  /** The build target for this genrule. */
  @AddToRuleKey protected final BuildTarget buildTarget;

  /**
   * SourceSet for this Genrule, exposed as SRCS in the genrule command.
   *
   * <p>The order in which elements are specified in the {@code srcs} attribute of a genrule
   * matters.
   */
  @AddToRuleKey protected final SourceSet srcs;

  /**
   * The shell command to run to generate the output file. Used as the fallback if neither bash nor
   * cmdExe are provided.
   */
  @AddToRuleKey protected final Optional<Arg> cmd;

  /** The bash shell command to generate the output file. Only used on platforms that have bash. */
  @AddToRuleKey protected final Optional<Arg> bash;

  /** The cmd shell command to generate the output file. Only used on Windows. */
  @AddToRuleKey protected final Optional<Arg> cmdExe;

  /** The name of the output file that this genrule intends to generate. */
  @AddToRuleKey protected final String out;

  /** Whether this target should be executed in a sandbox, if one is supported on this platform. */
  @AddToRuleKey private final boolean enableSandboxingInGenrule;

  /** The delimiter between paths in environment variables. */
  @AddToRuleKey private final String environmentExpansionSeparator;

  /** Whether or not the tool being invoked in this genrule is a worker_tool . */
  @AddToRuleKey private final boolean isWorkerGenrule;

  /**
   * The output path of the file generated by this genrule. Note that this output path is Public
   * because it uses a folder name that is exactly equal to the target name, unlike other MBRs which
   * use the target name suffixed by the flavor (or __ if no flavor is provided). This is for
   * backwards compatability with users of Buck that have hardcoded their paths.
   */
  @AddToRuleKey protected final PublicOutputPath outputPath;

  /**
   * Whether or not this genrule can be cached. This is not used within this class, but is required
   * to be a part of the rule key.
   */
  @AddToRuleKey protected final boolean isCacheable;

  /** Whether or not this genrule can be executed remotely. Fails serialization if false. */
  @AddToRuleKey
  @CustomFieldBehavior(RemoteExecutionEnabled.class)
  private final boolean executeRemotely = false;

  /** Type for this genrule, if one was provided. */
  @AddToRuleKey protected final Optional<String> type;

  /**
   * The set of optional Android tools to make available inside the genrule's environment. This rule
   * should not be remote-executed if this field is present!
   */
  @ExcludeFromRuleKey(
      reason = "GenruleAndroidTools contains paths to things outside of the repo",
      serialization = GenruleAndroidToolsBehavior.class,
      inputs = DefaultFieldInputs.class)
  private final Optional<GenruleAndroidTools> androidTools;

  /**
   * This genrule's sandbox execution strategy. If sandboxing is enabled for genrules and the
   * platform supports it, this strategy can be used to generate a sandbox suitable for running the
   * genrule command.
   *
   * <p>Note that this field is serialized as {@link NoSandboxExecutionStrategy}, since it does not
   * make sense to use a sandbox when executing remotely.
   */
  @ExcludeFromRuleKey(
      reason = "Non-default sandbox execution not useful when executing remotely",
      serialization = SandboxExecutionStrategyBehavior.class,
      inputs = SandboxExecutionStrategyBehavior.class)
  private final SandboxExecutionStrategy sandboxExecutionStrategy;

  /**
   * Sandbox properties for this genrule. The properties contain the set of permissions available to
   * the genrule process. This field is optional since retains a significant amount of memory when
   * present, even if left empty.
   *
   * <p>This field is also serialized as an empty property set since sandboxing does not make sense
   * when executing remotely.
   */
  @ExcludeFromRuleKey(
      reason = "Non-default sandbox execution not useful when executing remotely",
      serialization = SandboxPropertiesBehavior.class,
      inputs = SandboxPropertiesBehavior.class)
  private final Optional<SandboxProperties> sandboxProperties;

  public GenruleBuildable(
      BuildTarget buildTarget,
      ProjectFilesystem filesystem,
      SandboxExecutionStrategy sandboxExecutionStrategy,
      SourceSet srcs,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe,
      Optional<String> type,
      String out,
      boolean enableSandboxingInGenrule,
      boolean isCacheable,
      String environmentExpansionSeparator,
      Optional<SandboxProperties> sandboxProperties,
      Optional<GenruleAndroidTools> androidTools) {
    this.buildTarget = buildTarget;
    this.sandboxExecutionStrategy = sandboxExecutionStrategy;
    this.srcs = srcs;
    this.cmd = cmd;
    this.bash = bash;
    this.cmdExe = cmdExe;
    this.type = type;
    this.out = out;
    this.enableSandboxingInGenrule = enableSandboxingInGenrule;
    this.isCacheable = isCacheable;
    this.environmentExpansionSeparator = environmentExpansionSeparator;
    this.sandboxProperties = sandboxProperties;
    this.isWorkerGenrule = isWorkerGenrule();
    this.androidTools = androidTools;

    // Sanity check for the output path.
    Path legacyBasePath =
        BuildTargetPaths.getGenPath(filesystem, buildTarget, "%s").resolve(out).normalize();
    if (legacyBasePath.isAbsolute() || out.isEmpty()) {
      throw new HumanReadableException(
          "The 'out' parameter of genrule %s is '%s', which is not a valid file name.",
          buildTarget, out);
    }

    this.outputPath = new PublicOutputPath(legacyBasePath);
  }

  public OutputPath getOutput() {
    return outputPath;
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext buildContext,
      ProjectFilesystem filesystem,
      OutputPathResolver outputPathResolver,
      BuildCellRelativePathFactory buildCellPathFactory) {
    ImmutableList.Builder<Step> commands = ImmutableList.builder();

    // Genrules traditionally used an un-postfixed folder name to deposit their outputs. Modern
    // build rules suffix "__" to an unflavored target's output directory. To avoid breaking things,
    // we deposit our outputs in un-postfix (legacy) folder name.
    //
    // Not that it is not sufficient to create the parent directory of `outputPath`; there are rules
    // that consist of nested directory and file paths that will not be correct. The contract of
    // genrule is that only the legacyBasePath is created. All other paths must be created by the
    // shell script.
    Path legacyBasePath = BuildTargetPaths.getGenPath(filesystem, buildTarget, "%s");
    commands.addAll(
        MakeCleanDirectoryStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                buildContext.getBuildCellRootPath(), filesystem, legacyBasePath)));

    // If we use the OutputPathResolver's temp path, we don't need to create the directory; it will
    // be automatically created for us.
    Path tmpPath = outputPathResolver.getTempPath();

    // Create a directory to hold all the source files. Ideally this would be under the temp path,
    // but there exist tools (namely the Protobuf compiler) that have a hard dependency on the
    // compiler's working directory sharing a directory tree with the files being compiled.
    Path srcPath = BuildTargetPaths.getGenPath(filesystem, buildTarget, SRC_DIRECTORY_PATTERN);
    commands.addAll(
        MakeCleanDirectoryStep.of(
            BuildCellRelativePath.fromCellRelativePath(
                buildContext.getBuildCellRootPath(), filesystem, srcPath)));

    addSymlinkCommands(buildContext, filesystem, srcPath, commands);

    // Create a shell command that corresponds to this.cmd.
    if (this.isWorkerGenrule) {
      commands.add(
          createWorkerShellStep(buildContext, outputPathResolver, filesystem, srcPath, tmpPath));
    } else {
      commands.add(
          createGenruleStep(buildContext, outputPathResolver, filesystem, srcPath, tmpPath));
    }

    // ZipScrubberStep requires that its argument path be absolute.
    Path pathToOutFile =
        filesystem.getPathForRelativePath(outputPathResolver.resolvePath(outputPath));
    if (MorePaths.getFileExtension(pathToOutFile).equals("zip")) {
      commands.add(ZipScrubberStep.of(pathToOutFile));
    }

    return commands.build();
  }

  @VisibleForTesting
  final void addSymlinkCommands(
      BuildContext context,
      ProjectFilesystem filesystem,
      Path pathToSrcDirectory,
      ImmutableList.Builder<Step> commands) {
    if (srcs.isEmpty()) {
      return;
    }

    Map<Path, Path> linksBuilder = new HashMap<>();
    // Symlink all sources into the temp directory so that they can be used in the genrule.
    srcs.getNamedSources()
        .ifPresent(
            srcs ->
                addLinksForNamedSources(
                    context.getSourcePathResolver(), filesystem, srcs, linksBuilder));
    srcs.getUnnamedSources()
        .ifPresent(
            srcs ->
                addLinksForAnonymousSources(
                    context.getSourcePathResolver(), filesystem, srcs, linksBuilder));
    commands.add(
        new SymlinkTreeStep(
            "genrule_srcs",
            filesystem,
            pathToSrcDirectory,
            ImmutableSortedMap.copyOf(linksBuilder)));
  }

  private void addLinksForNamedSources(
      SourcePathResolverAdapter pathResolver,
      ProjectFilesystem filesystem,
      ImmutableMap<String, SourcePath> srcs,
      Map<Path, Path> links) {
    srcs.forEach(
        (name, src) -> {
          Path absolutePath = pathResolver.getAbsolutePath(src);
          Path target = filesystem.relativize(absolutePath);
          links.put(filesystem.getPath(name), target);
        });
  }

  private void addLinksForAnonymousSources(
      SourcePathResolverAdapter pathResolver,
      ProjectFilesystem filesystem,
      ImmutableSet<SourcePath> srcs,
      Map<Path, Path> links) {

    // To preserve legacy behavior, we allow duplicate targets and just ignore all but the
    // last.
    Set<Path> seenTargets = new HashSet<>();
    Path basePath =
        buildTarget.getCellRelativeBasePath().getPath().toPath(filesystem.getFileSystem());
    ImmutableList.copyOf(srcs)
        .reverse()
        .forEach(
            src -> {
              Path relativePath = pathResolver.getRelativePath(src);
              Path absolutePath = pathResolver.getAbsolutePath(src);
              Path canonicalPath = absolutePath.normalize();

              // By the time we get this far, all source paths (the keys in the map) have
              // been converted
              // to paths relative to the project root. We want the path relative to the
              // build target, so
              // strip the base path.
              Path localPath;
              if (absolutePath.equals(canonicalPath)) {
                if (relativePath.startsWith(basePath) || basePath.toString().isEmpty()) {
                  localPath = MorePaths.relativize(basePath, relativePath);
                } else {
                  localPath = canonicalPath.getFileName();
                }
              } else {
                localPath = relativePath;
              }

              Path target = filesystem.relativize(absolutePath);
              if (!seenTargets.contains(target)) {
                seenTargets.add(target);
                links.put(localPath, target);
              }
            });
  }

  @VisibleForTesting
  final boolean isWorkerGenrule() {
    Arg cmdArg = cmd.orElse(null);
    Arg bashArg = bash.orElse(null);
    Arg cmdExeArg = cmdExe.orElse(null);
    if ((cmdArg instanceof WorkerMacroArg)
        || (bashArg instanceof WorkerMacroArg)
        || (cmdExeArg instanceof WorkerMacroArg)) {
      if ((cmdArg != null && !(cmdArg instanceof WorkerMacroArg))
          || (bashArg != null && !(bashArg instanceof WorkerMacroArg))
          || (cmdExeArg != null && !(cmdExeArg instanceof WorkerMacroArg))) {
        throw new HumanReadableException(
            "You cannot use a worker macro in one of the cmd, bash, "
                + "or cmd_exe properties and not in the others for genrule %s.",
            buildTarget.getFullyQualifiedName());
      }
      return true;
    }
    return false;
  }

  @VisibleForTesting
  public Optional<Arg> getCmd() {
    return cmd;
  }

  @VisibleForTesting
  final AbstractGenruleStep createGenruleStep(
      BuildContext context,
      OutputPathResolver outputPathResolver,
      ProjectFilesystem filesystem,
      Path srcPath,
      Path tmpPath) {
    SourcePathResolverAdapter sourcePathResolverAdapter = context.getSourcePathResolver();

    // The user's command (this.cmd) should be run from the directory that contains only the
    // symlinked files. This ensures that the user can reference only the files that were declared
    // as srcs. Without this, a genrule is not guaranteed to be hermetic.

    ProgramRunner programRunner;

    if (sandboxExecutionStrategy.isSandboxEnabled() && enableSandboxingInGenrule) {
      Preconditions.checkState(
          sandboxProperties.isPresent(),
          "SandboxProperties must have been calculated earlier if sandboxing was requested");
      programRunner = sandboxExecutionStrategy.createSandboxProgramRunner(sandboxProperties.get());
    } else {
      programRunner = new DirectProgramRunner();
    }

    return new AbstractGenruleStep(
        filesystem,
        new AbstractGenruleStep.CommandString(
            Arg.flattenToSpaceSeparatedString(cmd, sourcePathResolverAdapter),
            Arg.flattenToSpaceSeparatedString(bash, sourcePathResolverAdapter),
            Arg.flattenToSpaceSeparatedString(cmdExe, sourcePathResolverAdapter)),
        BuildCellRelativePath.fromCellRelativePath(
                context.getBuildCellRootPath(), filesystem, srcPath)
            .getPathRelativeToBuildCellRoot(),
        programRunner) {
      @Override
      protected void addEnvironmentVariables(
          ExecutionContext executionContext,
          ImmutableMap.Builder<String, String> environmentVariablesBuilder) {
        GenruleBuildable.this.addEnvironmentVariables(
            sourcePathResolverAdapter,
            outputPathResolver,
            filesystem,
            srcPath,
            tmpPath,
            environmentVariablesBuilder);
      }
    };
  }

  /**
   * Adds the standard set of environment variables to the genrule, which are then exposed to the
   * genrule command.
   *
   * <p>This method populates these well-known environment variables:
   *
   * <ul>
   *   <li><code>SRCS</code>, a delimited list of source file inputs to the genrule
   *   <li><code>OUT</code>, the genrule's output file
   *   <li><code>GEN_DIR</code>, Buck's gendir
   *   <li><code>SRCDIR</code>, the symlink-populated source directory readable to the command
   *   <li><code>TMP</code>, the temp directory usable by the command
   *   <li><code>ANDROID_HOME</code>, the path to the Android SDK (if present)
   *   <li><code>DX</code>, the path to the Android DX executable (if present)
   *   <li><code>ZIPALIGN</code>, the path to the Android Zipalign executable (if present)
   *   <li><code>AAPT</code>, the path to the Android AAPT executable (if present)
   *   <li><code>AAPT2</code>, the path to the Android AAPT2 executable (if present)
   *   <li><code>NDK_HOME</code>, the path to the Android NDK (if present)
   * </ul>
   *
   * This method also sets <code>NO_BUCKD</code> to <code>1</code>.
   *
   * @param pathResolver Path resolver for resolving paths for <code>SRCS</code>
   * @param outputPathResolver Path resolver for resolving <code>OUT</code>
   * @param filesystem Filesystem for resolving relative paths for <code>SRCDIR</code> and <code>TMP
   *     </code>
   * @param srcPath Path to the generated symlink source directory
   * @param tmpPath Path to the genrule temporary directory
   * @param environmentVariablesBuilder Environment map builder
   */
  protected void addEnvironmentVariables(
      SourcePathResolverAdapter pathResolver,
      OutputPathResolver outputPathResolver,
      ProjectFilesystem filesystem,
      Path srcPath,
      Path tmpPath,
      ImmutableMap.Builder<String, String> environmentVariablesBuilder) {
    Path resolvedOutput = filesystem.resolve(outputPathResolver.resolvePath(outputPath));
    environmentVariablesBuilder.put(
        "SRCS",
        srcs.getPaths().stream()
            .map(pathResolver::getAbsolutePath)
            .map(Object::toString)
            .collect(Collectors.joining(this.environmentExpansionSeparator)));
    environmentVariablesBuilder.put("OUT", resolvedOutput.toString());

    environmentVariablesBuilder.put(
        "GEN_DIR", filesystem.resolve(filesystem.getBuckPaths().getGenDir()).toString());
    environmentVariablesBuilder.put("SRCDIR", filesystem.resolve(srcPath).toString());
    environmentVariablesBuilder.put("TMP", filesystem.resolve(tmpPath).toString());

    // TODO(mbolin): This entire hack needs to be removed. The [tools] section of .buckconfig
    // should be generalized to specify local paths to tools that can be used in genrules.

    androidTools.ifPresent(
        tools -> {
          environmentVariablesBuilder.put("ANDROID_HOME", tools.getAndroidSdkLocation().toString());
          environmentVariablesBuilder.put("DX", tools.getAndroidPathToDx().toString());
          environmentVariablesBuilder.put("ZIPALIGN", tools.getAndroidPathToZipalign().toString());
          environmentVariablesBuilder.put(
              "AAPT", String.join(" ", tools.getAaptTool().getCommandPrefix(pathResolver)));
          environmentVariablesBuilder.put(
              "AAPT2", String.join(" ", tools.getAapt2Tool().getCommandPrefix(pathResolver)));
          tools
              .getAndroidNdkLocation()
              .ifPresent(ndk -> environmentVariablesBuilder.put("NDK_HOME", ndk.toString()));
        });

    // TODO(t5302074): This shouldn't be necessary. Speculatively disabling.
    environmentVariablesBuilder.put("NO_BUCKD", "1");
  }

  private WorkerShellStep createWorkerShellStep(
      BuildContext context,
      OutputPathResolver outputPathResolver,
      ProjectFilesystem filesystem,
      Path srcPath,
      Path tmpPath) {
    return new WorkerShellStep(
        buildTarget,
        convertToWorkerJobParams(filesystem, context.getSourcePathResolver(), cmd),
        convertToWorkerJobParams(filesystem, context.getSourcePathResolver(), bash),
        convertToWorkerJobParams(filesystem, context.getSourcePathResolver(), cmdExe),
        new WorkerProcessPoolFactory(filesystem)) {
      @Override
      protected ImmutableMap<String, String> getEnvironmentVariables() {
        ImmutableMap.Builder<String, String> envVarBuilder = ImmutableMap.builder();
        GenruleBuildable.this.addEnvironmentVariables(
            context.getSourcePathResolver(),
            outputPathResolver,
            filesystem,
            srcPath,
            tmpPath,
            envVarBuilder);
        return envVarBuilder.build();
      }
    };
  }

  private static Optional<WorkerJobParams> convertToWorkerJobParams(
      ProjectFilesystem filesystem, SourcePathResolverAdapter resolver, Optional<Arg> arg) {
    return arg.map(
        arg1 -> {
          WorkerMacroArg workerMacroArg = (WorkerMacroArg) arg1;
          return WorkerJobParams.of(
              workerMacroArg.getJobArgs(resolver),
              WorkerProcessParams.of(
                  workerMacroArg.getTempDir(filesystem),
                  workerMacroArg.getStartupCommand(),
                  workerMacroArg.getEnvironment(),
                  workerMacroArg.getMaxWorkers(),
                  workerMacroArg.getPersistentWorkerKey().isPresent()
                      ? Optional.of(
                          WorkerProcessIdentity.of(
                              workerMacroArg.getPersistentWorkerKey().get(),
                              workerMacroArg.getWorkerHash()))
                      : Optional.empty()));
        });
  }

  /**
   * Serialization strategy for {@link SandboxExecutionStrategy} that deserializes to only {@link
   * NoSandboxExecutionStrategy} and takes up no bytes on the wire.
   */
  private static class SandboxExecutionStrategyBehavior
      implements CustomFieldSerialization<SandboxExecutionStrategy>,
          CustomFieldInputs<SandboxExecutionStrategy> {

    @Override
    public void getInputs(SandboxExecutionStrategy value, Consumer<SourcePath> consumer) {
      // No inputs, don't populate anything.
    }

    @Override
    public <E extends Exception> void serialize(
        SandboxExecutionStrategy value, ValueVisitor<E> serializer) throws E {
      // Don't place anything on the wire, there's no information to convey.
    }

    @Override
    public <E extends Exception> SandboxExecutionStrategy deserialize(ValueCreator<E> deserializer)
        throws E {
      return new NoSandboxExecutionStrategy();
    }
  }

  /**
   * Serialization strategy for {@link SandboxProperties} that deserializes to the default value of
   * the builder and takes up no bytes on the wire.
   */
  private static class SandboxPropertiesBehavior
      implements CustomFieldSerialization<Optional<SandboxProperties>>,
          CustomFieldInputs<Optional<SandboxProperties>> {

    @Override
    public void getInputs(Optional<SandboxProperties> value, Consumer<SourcePath> consumer) {
      // No inputs, don't populate anything.
    }

    @Override
    public <E extends Exception> void serialize(
        Optional<SandboxProperties> value, ValueVisitor<E> serializer) throws E {
      // Don't place anything on the wire, there's no information to convey.
    }

    @Override
    public <E extends Exception> Optional<SandboxProperties> deserialize(
        ValueCreator<E> deserializer) throws E {
      return Optional.empty();
    }
  }

  private static class GenruleAndroidToolsBehavior
      implements CustomFieldSerialization<Optional<GenruleAndroidToolsBehavior>> {
    @Override
    public <E extends Exception> void serialize(
        Optional<GenruleAndroidToolsBehavior> value, ValueVisitor<E> serializer) throws E {
      if (value.isPresent()) {
        throw new DisableRemoteExecutionException();
      }
    }

    @Override
    public <E extends Exception> Optional<GenruleAndroidToolsBehavior> deserialize(
        ValueCreator<E> deserializer) throws E {
      return Optional.empty();
    }

    private static class DisableRemoteExecutionException extends HumanReadableException {
      public DisableRemoteExecutionException() {
        super("Remote execution is not available to genrules that need Android tools.");
      }
    }
  }
}
