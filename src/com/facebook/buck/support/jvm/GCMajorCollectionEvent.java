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
package com.facebook.buck.support.jvm;

import com.facebook.buck.event.EventKey;
import com.sun.management.GcInfo;

/** GC Buck event representing a major GC collection. */
public final class GCMajorCollectionEvent extends GCCollectionEvent {
  public GCMajorCollectionEvent(GcInfo info) {
    super(EventKey.unique(), info);
  }

  @Override
  public String getEventName() {
    return "GC.MajorCollection";
  }
}
