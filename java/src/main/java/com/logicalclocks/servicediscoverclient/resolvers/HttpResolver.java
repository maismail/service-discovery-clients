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
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryGenericException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.orbitz.consul.option.QueryOptions;
import lombok.NonNull;

import java.util.List;
import java.util.stream.Stream;

public class HttpResolver implements ServiceDiscoveryClient {
  private Consul client;
  
  
  public HttpResolver() {}
  
  @Override
  public void init(@NonNull Builder builder) throws ServiceDiscoveryGenericException {
    if (builder.getClient() != null) {
      this.client = builder.getClient();
    } else {
      this.client = createConsulClient(builder);
    }
  }
  
  @SuppressWarnings("UnstableApiUsage")
  private Consul createConsulClient(Builder builder) throws ServiceDiscoveryGenericException {
    try {
      HostAndPort hostAndPort = HostAndPort.fromParts(builder.getHttpHost(), builder.getHttpPort());
      return Consul.builder()
          .withHostAndPort(hostAndPort)
          .withHttps(builder.getHttps())
          .withSslContext(builder.getSslContext())
          .withHostnameVerifier(builder.getHostnameVerifier())
          .build();
    } catch (ConsulException ex) {
      throw new ServiceDiscoveryGenericException("Could not initialize client", ex);
    }
  }
  
  @Override
  public Stream<Service> getService(@NonNull ServiceQuery service) throws ServiceDiscoveryException {
    QueryOptions queryOptions = ImmutableQueryOptions.builder()
        .addAllTag(service.getTags())
        .build();
    List<ServiceHealth> serviceHealths = getServiceHealth(service.getName(), queryOptions);
    if (serviceHealths.isEmpty()) {
      throw new ServiceNotFoundException("Could not find service " + service);
    }
  
    return serviceHealths.stream().map(this::convertToService);
  }
  
  private List<ServiceHealth> getServiceHealth(String name, QueryOptions queryOptions)
      throws ServiceDiscoveryGenericException{
    try {
      HealthClient hc = client.healthClient();
      return hc.getHealthyServiceInstances(name, queryOptions).getResponse();
    } catch (ConsulException ex) {
      throw new ServiceDiscoveryGenericException(ex);
    }
  }
  
  private Service convertToService(ServiceHealth serviceHealth) {
    return Service.of(serviceHealth.getService().getService(),
        serviceHealth.getNode().getAddress(),
        serviceHealth.getService().getPort());
  }
  
  @Override
  public void close() {
    if (this.client != null) {
      this.client.destroy();
    }
  }
}
