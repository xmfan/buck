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

import static com.facebook.buck.core.cell.TestCellBuilder.createCellRoots;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.facebook.buck.core.cell.CellPathResolver;
import com.facebook.buck.core.cell.CellPathResolverView;
import com.facebook.buck.core.cell.TestCellPathResolver;
import com.facebook.buck.core.cell.nameresolver.TestCellNameResolver;
import com.facebook.buck.core.exceptions.BuildTargetParseException;
import com.facebook.buck.core.model.BuildTargetFactory;
import com.facebook.buck.core.model.CanonicalCellName;
import com.facebook.buck.core.model.ImmutableCellRelativePath;
import com.facebook.buck.core.model.ImmutableUnconfiguredBuildTargetWithOutputs;
import com.facebook.buck.core.model.OutputLabel;
import com.facebook.buck.core.model.UnconfiguredBuildTargetFactoryForTests;
import com.facebook.buck.core.model.UnconfiguredBuildTargetView;
import com.facebook.buck.core.model.UnconfiguredTargetConfiguration;
import com.facebook.buck.core.path.ForwardRelativePath;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.io.filesystem.impl.FakeProjectFilesystem;
import com.facebook.buck.parser.exceptions.NoSuchBuildTargetException;
import com.facebook.buck.parser.spec.BuildTargetMatcherTargetNodeParser;
import com.facebook.buck.parser.spec.BuildTargetSpec;
import com.facebook.buck.parser.spec.TargetNodeSpec;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.nio.file.FileSystem;
import java.util.Optional;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BuildTargetMatcherParserTest {

  private ProjectFilesystem filesystem;
  private FileSystem vfs;

  @Rule public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    filesystem = new FakeProjectFilesystem();
    vfs = filesystem.getRootPath().getFileSystem();
  }

  @Test
  public void testParse() throws NoSuchBuildTargetException {
    BuildTargetMatcherParser<BuildTargetMatcher> buildTargetPatternParser =
        BuildTargetMatcherParser.forVisibilityArgument();

    assertEquals(
        ImmediateDirectoryBuildTargetMatcher.of(
            new ImmutableCellRelativePath(
                CanonicalCellName.rootCell(),
                ForwardRelativePath.of("test/com/facebook/buck/parser"))),
        buildTargetPatternParser.parse(
            createCellRoots(filesystem), "//test/com/facebook/buck/parser:"));

    assertEquals(
        SingletonBuildTargetMatcher.of(
            BuildTargetFactory.newInstance("//test/com/facebook/buck/parser:parser")
                .getUnflavoredBuildTarget()
                .getData()),
        buildTargetPatternParser.parse(
            createCellRoots(filesystem), "//test/com/facebook/buck/parser:parser"));

    assertEquals(
        SubdirectoryBuildTargetMatcher.of(
            new ImmutableCellRelativePath(
                CanonicalCellName.unsafeRootCell(),
                ForwardRelativePath.of("test/com/facebook/buck/parser"))),
        buildTargetPatternParser.parse(
            createCellRoots(filesystem), "//test/com/facebook/buck/parser/..."));
  }

  @Test
  public void testParseRootPattern() throws NoSuchBuildTargetException {
    BuildTargetMatcherParser<BuildTargetMatcher> buildTargetPatternParser =
        BuildTargetMatcherParser.forVisibilityArgument();

    assertEquals(
        ImmediateDirectoryBuildTargetMatcher.of(
            new ImmutableCellRelativePath(
                CanonicalCellName.rootCell(), ForwardRelativePath.of(""))),
        buildTargetPatternParser.parse(createCellRoots(filesystem), "//:"));

    assertEquals(
        SingletonBuildTargetMatcher.of(
            BuildTargetFactory.newInstance("//:parser").getUnflavoredBuildTarget().getData()),
        buildTargetPatternParser.parse(createCellRoots(filesystem), "//:parser"));

    assertEquals(
        SubdirectoryBuildTargetMatcher.of(
            new ImmutableCellRelativePath(
                CanonicalCellName.unsafeRootCell(), ForwardRelativePath.of(""))),
        buildTargetPatternParser.parse(createCellRoots(filesystem), "//..."));
  }

  @Test
  public void visibilityCanContainCrossCellReference() {
    BuildTargetMatcherParser<BuildTargetMatcher> buildTargetPatternParser =
        BuildTargetMatcherParser.forVisibilityArgument();

    CellPathResolver cellNames =
        TestCellPathResolver.create(
            filesystem.resolve("foo/root"),
            ImmutableMap.of("other", filesystem.getPath("../other")));

    assertEquals(
        SingletonBuildTargetMatcher.of(
            BuildTargetFactory.newInstance("other//:something")
                .getUnflavoredBuildTarget()
                .getData()),
        buildTargetPatternParser.parse(cellNames, "other//:something"));
    assertEquals(
        SubdirectoryBuildTargetMatcher.of(
            new ImmutableCellRelativePath(
                CanonicalCellName.unsafeOf(Optional.of("other")), ForwardRelativePath.of("sub"))),
        buildTargetPatternParser.parse(cellNames, "other//sub/..."));
  }

  @Test
  public void visibilityCanMatchCrossCellTargets() {
    BuildTargetMatcherParser<BuildTargetMatcher> buildTargetPatternParser =
        BuildTargetMatcherParser.forVisibilityArgument();

    ProjectFilesystem filesystem = FakeProjectFilesystem.createJavaOnlyFilesystem();
    CellPathResolver rootCellPathResolver =
        TestCellPathResolver.create(
            filesystem.resolve("root").normalize(),
            ImmutableMap.of(
                "other", filesystem.getPath("../other").normalize(),
                "root", filesystem.getPath("../root").normalize()));
    CellPathResolver otherCellPathResolver =
        new CellPathResolverView(
            rootCellPathResolver,
            TestCellNameResolver.forSecondary("other", Optional.of("root")),
            ImmutableSet.of("root"),
            filesystem.resolve("other").normalize());
    UnconfiguredBuildTargetViewFactory unconfiguredBuildTargetFactory =
        new ParsingUnconfiguredBuildTargetViewFactory();

    // Root cell visibility from non-root cell
    Stream.of("other//lib:lib", "other//lib:", "other//lib/...")
        .forEach(
            patternString -> {
              BuildTargetMatcher pattern =
                  buildTargetPatternParser.parse(rootCellPathResolver, patternString);
              assertTrue(
                  "from root matching something in non-root: " + pattern,
                  pattern.matches(
                      unconfiguredBuildTargetFactory
                          .create(otherCellPathResolver, "//lib:lib")
                          .configure(UnconfiguredTargetConfiguration.INSTANCE)));
              assertFalse(
                  "from root failing to match something in root: " + pattern,
                  pattern.matches(
                      unconfiguredBuildTargetFactory
                          .create(rootCellPathResolver, "//lib:lib")
                          .configure(UnconfiguredTargetConfiguration.INSTANCE)));
            });

    // Non-root cell visibility from root cell.
    Stream.of("root//lib:lib", "root//lib:", "root//lib/...")
        .forEach(
            patternString -> {
              BuildTargetMatcher pattern =
                  buildTargetPatternParser.parse(otherCellPathResolver, patternString);
              assertTrue(
                  "from non-root matching something in root: " + pattern,
                  pattern.matches(
                      unconfiguredBuildTargetFactory
                          .create(rootCellPathResolver, "//lib:lib")
                          .configure(UnconfiguredTargetConfiguration.INSTANCE)));
              assertFalse(
                  "from non-root matching something in non-root: " + pattern,
                  pattern.matches(
                      unconfiguredBuildTargetFactory
                          .create(otherCellPathResolver, "//lib:lib")
                          .configure(UnconfiguredTargetConfiguration.INSTANCE)));
            });
  }

  @Test
  public void testParseAbsolutePath() {
    // Exception should be thrown by BuildTargetParser.checkBaseName()
    BuildTargetMatcherParser<BuildTargetMatcher> buildTargetPatternParser =
        BuildTargetMatcherParser.forVisibilityArgument();

    exception.expect(BuildTargetParseException.class);
    exception.expectMessage("absolute");
    exception.expectMessage("(found ///facebookorca/...)");
    buildTargetPatternParser.parse(createCellRoots(filesystem), "///facebookorca/...");
  }

  @Test
  public void testIncludesTargetNameInMissingCellErrorMessage() {
    BuildTargetMatcherParser<BuildTargetMatcher> buildTargetPatternParser =
        BuildTargetMatcherParser.forVisibilityArgument();

    ProjectFilesystem filesystem = FakeProjectFilesystem.createJavaOnlyFilesystem();
    CellPathResolver rootCellPathResolver =
        TestCellPathResolver.create(
            filesystem.getPath("root").normalize(),
            ImmutableMap.of("localreponame", filesystem.getPath("localrepo").normalize()));

    exception.expect(BuildTargetParseException.class);
    // It contains the pattern
    exception.expectMessage("lclreponame//facebook/...");
    // The invalid cell
    exception.expectMessage("Unknown cell: lclreponame");
    // And the suggestion
    exception.expectMessage("localreponame");
    buildTargetPatternParser.parse(rootCellPathResolver, "lclreponame//facebook/...");
  }

  @Test
  public void parsesOutputLabel() {
    BuildTargetMatcherParser<TargetNodeSpec> buildTargetPatternParser =
        new BuildTargetMatcherTargetNodeParser();
    UnconfiguredBuildTargetView unconfiguredBuildTargetView =
        UnconfiguredBuildTargetFactoryForTests.newInstance(
            filesystem, "//test/com/facebook/buck/parser:parser");

    assertEquals(
        BuildTargetSpec.from(
            ImmutableUnconfiguredBuildTargetWithOutputs.of(
                unconfiguredBuildTargetView, new OutputLabel("label"))),
        buildTargetPatternParser.parse(
            createCellRoots(filesystem), "//test/com/facebook/buck/parser:parser[label]"));

    assertEquals(
        BuildTargetSpec.from(
            ImmutableUnconfiguredBuildTargetWithOutputs.of(
                unconfiguredBuildTargetView, OutputLabel.DEFAULT)),
        buildTargetPatternParser.parse(
            createCellRoots(filesystem), "//test/com/facebook/buck/parser:parser"));
  }

  @Test
  public void descendantSyntaxCannotHaveOutputLabel() {
    exception.expect(Matchers.instanceOf(BuildTargetParseException.class));
    exception.expectMessage("//test/com/facebook/buck/parser: should not have output label noms");

    new BuildTargetMatcherTargetNodeParser()
        .parse(createCellRoots(filesystem), "//test/com/facebook/buck/parser:[noms]");
  }

  @Test
  public void wildcardSyntaxCannotHaveOutputLabel() {
    exception.expect(Matchers.instanceOf(BuildTargetParseException.class));
    exception.expectMessage(
        "//test/com/facebook/buck/parser/... should not have output label noms");

    new BuildTargetMatcherTargetNodeParser()
        .parse(createCellRoots(filesystem), "//test/com/facebook/buck/parser/...[noms]");
  }
}
