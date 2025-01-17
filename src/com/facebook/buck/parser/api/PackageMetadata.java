/*
 * Copyright 2019-present Facebook, Inc.
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
package com.facebook.buck.parser.api;

import com.facebook.buck.core.util.immutables.BuckStyleValue;
import com.google.common.collect.ImmutableList;

/** Describes the attributes of a package rule in a package file. */
@BuckStyleValue
public abstract class PackageMetadata {

  /** A default instance with no visibility/within_view entries. */
  public static final ImmutablePackageMetadata SINGLETON =
      new ImmutablePackageMetadata(ImmutableList.of(), ImmutableList.of());

  /** @return the visibility defined in the package. */
  public abstract ImmutableList<String> getVisibility();

  /** @return the within_view defined in the package. */
  public abstract ImmutableList<String> getWithinView();
}
