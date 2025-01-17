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

package com.facebook.buck.core.parser.buildtargetparser;

import com.facebook.buck.core.cell.CellPathResolver;
import com.facebook.buck.core.cell.exception.UnknownCellException;
import com.facebook.buck.core.exceptions.BuildTargetParseException;
import com.facebook.buck.core.model.CanonicalCellName;
import com.facebook.buck.core.model.CellRelativePath;
import com.facebook.buck.core.model.ImmutableCellRelativePath;
import com.facebook.buck.core.model.ImmutableUnconfiguredBuildTargetWithOutputs;
import com.facebook.buck.core.model.UnconfiguredBuildTargetView;
import com.facebook.buck.core.path.ForwardRelativePath;
import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Context for parsing build target names. Fully-qualified target names are parsed the same
 * regardless of the context.
 */
// TODO(nga): this class is similar SimplePackageSpec parser,
//            but relies on filesystem paths instead of target labels.
//            We should merge these.
public abstract class BuildTargetMatcherParser<T> {

  private static final String BUILD_RULE_PREFIX = "//";
  private static final String WILDCARD_SEPARATOR = "/";
  private static final String WILDCARD_BUILD_RULE_SUFFIX = "...";
  private static final String BUILD_RULE_SEPARATOR = ":";

  private final UnconfiguredBuildTargetViewFactory unconfiguredBuildTargetFactory =
      new ParsingUnconfiguredBuildTargetViewFactory();

  /**
   * Matches the given {@code buildTargetPattern} according to the following rules:
   *
   * <ul>
   *   <li>//src/com/facebook/buck/cli:cli will be converted to a single build target;
   *   <li>//src/com/facebook/buck/cli: will match all in the same directory;
   *   <li>//src/com/facebook/buck/cli/... will match all in or under that directory.
   * </ul>
   *
   * For cases 2 and 3, parseContext is expected to be {@link
   * BuildTargetMatcherParser#forVisibilityArgument()}.
   */
  public final T parse(CellPathResolver cellNames, String buildTargetPattern) {
    Preconditions.checkArgument(
        buildTargetPattern.contains(BUILD_RULE_PREFIX),
        "'%s' must start with '//' or a cell followed by '//'",
        buildTargetPattern);

    BuildTargetOutputLabelParser.TargetWithOutputLabel targetWithOutputLabel =
        BuildTargetOutputLabelParser.getBuildTargetNameWithOutputLabel(buildTargetPattern);

    String wildcardSuffix = WILDCARD_SEPARATOR + WILDCARD_BUILD_RULE_SUFFIX;
    if (buildTargetPattern.contains(wildcardSuffix)) {
      if (targetWithOutputLabel.getOutputLabel().getLabel().isPresent()) {
        throw createOutputLabelParseException(targetWithOutputLabel);
      }
      if (!buildTargetPattern.endsWith(wildcardSuffix)) {
        throw new BuildTargetParseException(
            String.format("The %s pattern must occur at the end of the command", wildcardSuffix));
      }
      return createWildCardPattern(cellNames, buildTargetPattern);
    }

    UnconfiguredBuildTargetView target =
        unconfiguredBuildTargetFactory.createWithWildcard(
            cellNames, targetWithOutputLabel.getTargetName());
    if (target.getShortNameAndFlavorPostfix().isEmpty()) {
      if (targetWithOutputLabel.getOutputLabel().getLabel().isPresent()) {
        throw createOutputLabelParseException(targetWithOutputLabel);
      }
      return createForChildren(target.getCellRelativeBasePath());
    } else {
      return createForSingleton(
          ImmutableUnconfiguredBuildTargetWithOutputs.of(
              target, targetWithOutputLabel.getOutputLabel()));
    }
  }

  private T createWildCardPattern(CellPathResolver cellNames, String buildTargetPatternWithCell) {
    Path cellPath;
    String buildTargetPattern;
    int index = buildTargetPatternWithCell.indexOf(BUILD_RULE_PREFIX);
    if (index > 0) {
      try {
        cellPath =
            cellNames.getCellPathOrThrow(
                Optional.of(buildTargetPatternWithCell.substring(0, index)));
      } catch (UnknownCellException e) {
        throw new BuildTargetParseException(
            String.format(
                "When parsing %s: %s",
                buildTargetPatternWithCell, e.getHumanReadableErrorMessage()));
      }
      buildTargetPattern = buildTargetPatternWithCell.substring(index);
    } else {
      cellPath = cellNames.getCellPathOrThrow(Optional.empty());
      buildTargetPattern = buildTargetPatternWithCell;
    }

    if (buildTargetPattern.contains(BUILD_RULE_SEPARATOR)) {
      throw new BuildTargetParseException(
          String.format("'%s' cannot contain colon", buildTargetPattern));
    }

    if (!buildTargetPattern.equals(BUILD_RULE_PREFIX + WILDCARD_BUILD_RULE_SUFFIX)) {
      String basePathWithPrefix =
          buildTargetPattern.substring(
              0, buildTargetPattern.length() - WILDCARD_BUILD_RULE_SUFFIX.length() - 1);
      BaseNameParser.checkBaseName(basePathWithPrefix, buildTargetPattern);
    }

    String basePath =
        buildTargetPattern.substring(
            BUILD_RULE_PREFIX.length(),
            buildTargetPattern.length() - WILDCARD_BUILD_RULE_SUFFIX.length());
    if (basePath.endsWith("/")) {
      basePath = basePath.substring(0, basePath.length() - "/".length());
    }
    ForwardRelativePath forwardRelativePath = ForwardRelativePath.of(basePath);
    CanonicalCellName cellName = cellNames.getNewCellPathResolver().getCanonicalCellName(cellPath);
    return createForDescendants(new ImmutableCellRelativePath(cellName, forwardRelativePath));
  }

  /** Used when parsing target names in the {@code visibility} argument to a build rule. */
  public static BuildTargetMatcherParser<BuildTargetMatcher> forVisibilityArgument() {
    return new VisibilityContext();
  }

  /**
   * @return description of the target name and context being parsed when an error was encountered.
   *     Examples are ":azzetz in build file //first-party/orca/orcaapp/BUCK" and
   *     "//first-party/orca/orcaapp:mezzenger in context FULLY_QUALIFIED"
   */
  protected abstract T createForDescendants(CellRelativePath cellRelativePath);

  protected abstract T createForChildren(CellRelativePath cellRelativePath);

  protected abstract T createForSingleton(
      ImmutableUnconfiguredBuildTargetWithOutputs targetWithOutputs);

  private BuildTargetParseException createOutputLabelParseException(
      BuildTargetOutputLabelParser.TargetWithOutputLabel targetWithOutputs) {
    return new BuildTargetParseException(
        String.format(
            "%s should not have output label %s",
            targetWithOutputs.getTargetName(), targetWithOutputs.getOutputLabel()));
  }

  private static class VisibilityContext extends BuildTargetMatcherParser<BuildTargetMatcher> {

    @Override
    public BuildTargetMatcher createForDescendants(CellRelativePath cellRelativePath) {
      return SubdirectoryBuildTargetMatcher.of(cellRelativePath);
    }

    @Override
    public BuildTargetMatcher createForChildren(CellRelativePath cellRelativePath) {
      return ImmediateDirectoryBuildTargetMatcher.of(cellRelativePath);
    }

    @Override
    public BuildTargetMatcher createForSingleton(
        ImmutableUnconfiguredBuildTargetWithOutputs targetWithOutputs) {
      return SingletonBuildTargetMatcher.of(targetWithOutputs.getBuildTarget().getData());
    }
  }
}
