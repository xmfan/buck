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

package com.facebook.buck.core.build.context;

import com.facebook.buck.core.sourcepath.resolver.SourcePathResolverAdapter;
import com.facebook.buck.core.util.immutables.BuckStyleImmutable;
import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.jvm.core.JavaPackageFinder;
import java.nio.file.Path;
import org.immutables.value.Value;

@Value.Immutable(copy = true)
@BuckStyleImmutable
abstract class AbstractBuildContext {

  public abstract SourcePathResolverAdapter getSourcePathResolver();

  /** @return the absolute path of the cell in which the build was invoked. */
  public abstract Path getBuildCellRootPath();

  public abstract JavaPackageFinder getJavaPackageFinder();

  public abstract BuckEventBus getEventBus();

  public abstract boolean getShouldDeleteTemporaries();
}
