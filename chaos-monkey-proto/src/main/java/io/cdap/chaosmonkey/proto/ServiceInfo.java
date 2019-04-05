/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.chaosmonkey.proto;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the disruptions available to a service
 */
public class ServiceInfo {
  private String name;
  private Collection<String> disruptions;

  public ServiceInfo(String name, Collection<String> disruptions) {
    this.name = name;
    this.disruptions = disruptions;
  }

  public String getName() {
    return name;
  }

  public Collection<String> getDisruptions() {
    return disruptions == null ? new ArrayList<String>() : disruptions;
  }
}
