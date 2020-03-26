/**
 * This file is part of service-discovery-clients
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 *
 * service-discovery-clients is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * service-discovery-clients is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.logicalclocks.servicediscoverclient.service;

import lombok.ToString;
import lombok.Value;

import java.util.Set;

@ToString
@Value(staticConstructor = "of")
public final class ServiceQuery {
  String name;
  Set<String> tags;
}
