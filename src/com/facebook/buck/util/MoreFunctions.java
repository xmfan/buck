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
package com.facebook.buck.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Utilities to work with {@link Function}. */
public class MoreFunctions {

  private MoreFunctions() {}

  /** Thread-safe memoizer: computed value is cached and taken from cache on the second call. */
  public static <A, B> Function<A, B> memoize(Function<A, B> function) {
    ConcurrentHashMap<A, B> map = new ConcurrentHashMap<>();
    return a -> map.computeIfAbsent(a, function);
  }
}
