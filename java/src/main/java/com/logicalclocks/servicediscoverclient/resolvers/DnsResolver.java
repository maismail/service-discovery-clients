/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.logicalclocks.servicediscoverclient.resolvers;

import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryGenericException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import lombok.NonNull;

import java.util.List;

public final class DnsResolver implements ServiceDiscoveryClient {
  public DnsResolver() {
  }
  
  @Override
  public void init(@NonNull Builder builder) throws ServiceDiscoveryGenericException {
  
  }
  
  @Override
  public List<Service> getService(@NonNull ServiceQuery service) throws ServiceNotFoundException {
    throw new RuntimeException("Not implemented yet");
  }
  
  @Override
  public void close() {
  
  }
}
