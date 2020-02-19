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

import com.google.common.net.HostAndPort;
import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import com.sun.xml.internal.ws.Closeable;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class HttpResolver implements ServiceDiscoveryClient, Closeable {
  private final Consul client;
  
  @SuppressWarnings("UnstableApiUsage")
  public HttpResolver(@NonNull Builder builder) {
    HostAndPort hostAndPort = HostAndPort.fromParts(builder.getHttpHost(), builder.getHttpPort());
    client = Consul.builder()
        .withHostAndPort(hostAndPort)
        .withHttps(builder.getHttps())
        .withSslContext(builder.getSslContext())
        .withHostnameVerifier(builder.getHostnameVerifier())
        .build();
  }
  
  @Override
  public List<Service> getService(@NonNull ServiceQuery service) throws ServiceNotFoundException {
    HealthClient hc = client.healthClient();
    QueryOptions queryOptions = ImmutableQueryOptions.builder()
        .addAllTag(service.getTags())
        .build();
    List<ServiceHealth> services = hc.getHealthyServiceInstances(service.getName(), queryOptions).getResponse();
    if (services.isEmpty()) {
      throw new ServiceNotFoundException("Could not find service " + service);
    }
    List<Service> healthyServices = new ArrayList<>(services.size());
    for (ServiceHealth s : services) {
      healthyServices.add(Service.of(
          s.getService().getService(),
          s.getNode().getAddress(),
          s.getService().getPort()));
    }
    return healthyServices;
  }
  
  @Override
  public void close() {
    if (this.client != null) {
      this.client.destroy();
    }
  }
}
