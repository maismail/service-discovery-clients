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
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class DnsResolver implements ServiceDiscoveryClient {
  private Resolver resolver;
  
  public DnsResolver() {
  }
  
  @Override
  public void init(@NonNull Builder builder) throws ServiceDiscoveryGenericException {
      resolver = new SimpleResolver(new InetSocketAddress(builder.getDnsHost(), builder.getDnsPort()));
      resolver.setTimeout(Duration.of(2, ChronoUnit.SECONDS));
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
