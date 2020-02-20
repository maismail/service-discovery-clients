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
import java.util.ArrayList;
import java.util.List;

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
  public List<Service> getService(@NonNull ServiceQuery service) throws ServiceDiscoveryException {
    if (resolver == null) {
      throw new ServiceDiscoveryGenericException("DNS resolver has not been initialized");
    }
    try {
      Lookup SRVLookup = lookup(Name.fromString(service.getName()), Type.SRV);
      if (SRVLookup.getResult() == Lookup.SUCCESSFUL) {
        Record[] SRVRecords = SRVLookup.getAnswers();
        ArrayList<Service> services = new ArrayList<>(SRVRecords.length);
        for (Record r : SRVRecords) {
          if (r.getType() == Type.SRV) {
            SRVRecord srv = (SRVRecord) r;
            Integer port = srv.getPort();
            Lookup ALookup = lookup(srv.getTarget(), Type.A);
            Record[] ARecords = ALookup.getAnswers();
            if (ARecords.length > 0) {
              Record record = ARecords[0];
              if (record.getType() == Type.A) {
                ARecord ARecord = (org.xbill.DNS.ARecord) record;
                String address = ARecord.getAddress().getHostAddress();
                services.add(Service.of(service.getName(), address, port));
              }
            }
          }
        }
        return services;
      }
      throw new ServiceNotFoundException("Error: " + SRVLookup.getErrorString() + " Could not find service " + service);
    } catch (TextParseException ex) {
      throw new ServiceDiscoveryGenericException(ex);
    }
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
