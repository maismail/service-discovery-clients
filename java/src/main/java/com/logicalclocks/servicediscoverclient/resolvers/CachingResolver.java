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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryGenericException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import lombok.NonNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CachingResolver implements ServiceDiscoveryClient {
    private ServiceDiscoveryClient resolver;

    private LoadingCache<ServiceQuery, List<Service>> serviceCache;

    public CachingResolver() {
    }

    @Override
    public void init(@NonNull Builder builder) throws ServiceDiscoveryException {
        if (builder.getServiceDiscoveryClient() == null) {
            // Fallback to DNS resolver
            resolver = new Builder(Type.DNS)
                    .withDnsHost(builder.getDnsHost())
                    .withDnsPort(builder.getDnsPort())
                    .build();
        } else {
            resolver = builder.getServiceDiscoveryClient();
        }
        serviceCache = CacheBuilder.newBuilder()
                .expireAfterWrite(builder.getCacheExpiration())
                .weakValues()
                .build(new CacheLoader<ServiceQuery, List<Service>>() {
                    @Override
                    public List<Service> load(ServiceQuery serviceQuery) throws Exception {
                        return getServiceInternal(serviceQuery);
                    }
                });
    }

    @Override
    public Stream<Service> getService(@NonNull ServiceQuery service) throws ServiceDiscoveryException {
        if (serviceCache == null || resolver == null) {
            throw new ServiceDiscoveryGenericException("Caching resolver has not been initialized");
        }
        try {
            return serviceCache.get(service).stream();
        } catch (ExecutionException ex) {
            if (ex.getCause() != null && ex.getCause() instanceof ServiceNotFoundException) {
                throw (ServiceNotFoundException) ex.getCause();
            }
            throw new ServiceDiscoveryException(ex);
        }
    }

    @Override
    public void close() {
        if (resolver != null) {
            resolver.close();
        }
    }

    private List<Service> getServiceInternal(ServiceQuery service) throws ServiceDiscoveryException {
        return resolver.getService(service).collect(Collectors.toList());
    }
}
