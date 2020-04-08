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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryGenericException;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import lombok.NonNull;
import org.xbill.DNS.Type;
import org.xbill.DNS.*;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class DnsResolver implements ServiceDiscoveryClient {
  private Resolver resolver;
  
  public DnsResolver() {
  }
  
  @Override
  public void init(@NonNull Builder builder) throws ServiceDiscoveryGenericException {
    try {
      resolver = new SimpleResolver();
      if (builder.getDnsHost() == null || builder.getDnsPort() == null) {
        List<InetSocketAddress> nameservers = ResolverConfig.getCurrentConfig().servers();
        if (nameservers == null || nameservers.isEmpty()) {
          throw new ServiceDiscoveryGenericException("Unable to find system's nameservers. Check your resolver file " +
                  "or explicitly set DNS host and port in builder");
        }
        ((SimpleResolver) resolver).setAddress(nameservers.get(0));
      } else {
        ((SimpleResolver) resolver).setAddress(new InetSocketAddress(builder.getDnsHost(), builder.getDnsPort()));
      }
      resolver.setTimeout(4);
    } catch (UnknownHostException ex) {
      throw new ServiceDiscoveryGenericException(ex);
    }
  }
  
  @Override
  public Stream<Service> getService(@NonNull ServiceQuery service) throws ServiceDiscoveryException {
    if (resolver == null) {
      throw new ServiceDiscoveryGenericException("DNS resolver has not been initialized");
    }
    
    try {
      List<Record> SRVRecords = getSRVRecords(service);
      return SRVRecords.stream()
          .filter(r -> r.getType() == Type.SRV)
          .map(r -> (SRVRecord) r)
          .map(srv -> {
            String aRecord = getARecord(srv);
            if (aRecord == null) {
              return null;
            }
            return Service.of(service.getName(), aRecord, srv.getPort());
          })
          .filter(Objects::nonNull);
    } catch (TextParseException ex) {
      throw new ServiceDiscoveryGenericException(ex);
    }
  }
  
  private List<Record> getSRVRecords(ServiceQuery service) throws TextParseException, ServiceNotFoundException {
    Lookup lookup = lookup(Name.fromString(service.getName()), Type.SRV);
    if (lookup.getResult() != Lookup.SUCCESSFUL) {
      throw new ServiceNotFoundException("Error: " + lookup.getErrorString() + " Could not find service " + service);
    }
    return Lists.newArrayList(lookup.getAnswers());
  }
  
  private String getARecord(SRVRecord srvRecord) {
    Lookup lookup = lookup(srvRecord.getTarget(), Type.A);
    Record[] aRecords = lookup.getAnswers();
    for (Record r : aRecords) {
      if (r.getType() == Type.A) {
        ARecord aRecord = (ARecord) r;
        return aRecord.getAddress().getHostAddress();
      }
    }
    return null;
  }
  
  @VisibleForTesting
  public Lookup lookup(Name name, int type) {
    Lookup lookup = new Lookup(name, type);
    lookup.setResolver(resolver);
    lookup.run();
    return lookup;
  }
  
  @Override
  public void close() {
  
  }
}
