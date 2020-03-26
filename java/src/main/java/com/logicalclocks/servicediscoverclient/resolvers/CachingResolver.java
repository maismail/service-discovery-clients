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
