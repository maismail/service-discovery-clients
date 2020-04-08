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
package com.logicalclocks.servicediscoveryclient.resolvers;

import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.resolvers.DnsResolver;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestDnsResolver {
  private static final String DNS_IP = "127.0.0.1";
  private static final int DNS_PORT = 5453;
  
  @Test
  public void testHealthyService() throws Exception {
    assumeTrue(isServerRunning());
    ServiceDiscoveryClient client = new Builder(Type.DNS)
        .withDnsHost(DNS_IP)
        .withDnsPort(DNS_PORT)
        .build();
    Stream<Service> services = client.getService(ServiceQuery.of("namenode.service.lc", Collections.emptySet()));
    assertNotNull(services);
    assertTrue(services.count() > 0);
  }
  
  @Test
  public void testUnhealthyService() throws Exception {
    assumeTrue(isServerRunning());
    ServiceDiscoveryClient client = new Builder(Type.DNS)
        .withDnsHost(DNS_IP)
        .withDnsPort(DNS_PORT)
        .build();
    assertThrows(ServiceNotFoundException.class, () -> {
      client.getService(ServiceQuery.of("servicedoesnotexist.service.lc", Collections.emptySet()));
    });
  }
  
  @Test
  public void testHealthyServiceMock() throws Exception {
    String service = "namenode.service.lc.";
    int servicePort = 8080;
    String target0 = "node0.lc.";
    String target0IP = "10.0.0.1";
    InetAddress node0Address = new InetSocketAddress(target0IP, 3).getAddress();
    String target1 = "node1.lc.";
    String target1IP = "10.0.0.2";
    InetAddress node1Address = new InetSocketAddress(target1IP, 3).getAddress();
  
    Map<String, Service> expectedServicesMap = new HashMap<>(2);
    expectedServicesMap.put(target0IP, Service.of(service, target0IP, servicePort));
    expectedServicesMap.put(target1IP, Service.of(service, target1IP, servicePort));
    
    Builder resolverBuilder = new Builder(Type.DNS)
        .withDnsHost("localhost")
        .withDnsPort(53);
    DnsResolver client = mock(DnsResolver.class);
    doCallRealMethod().when(client).init(any(Builder.class));
    when(client.getService(any())).thenCallRealMethod();
    client.init(resolverBuilder);
    
    
    // SRV lookup
    Lookup mockedSRVLookup = mock(Lookup.class);
    SRVRecord srvRecord0 = new SRVRecord(Name.fromString(service), 1, 500, 1, 8080,
        servicePort, Name.fromString(target0));
    SRVRecord srvRecord1 = new SRVRecord(Name.fromString(service), 1, 500, 1, 8080,
        servicePort, Name.fromString(target1));
    
    Record[] SRVAnswer = new Record[2];
    SRVAnswer[0] = srvRecord0;
    SRVAnswer[1] = srvRecord1;
    when(mockedSRVLookup.getAnswers()).thenReturn(SRVAnswer);
    
    // A lookup for 10.0.0.1
    Lookup mockedALookup0 = mock(Lookup.class);
    ARecord aRecord0 = new ARecord(Name.fromString(target0), 1, 500, node0Address);
    Record[] AAnswer0 = new Record[1];
    AAnswer0[0] = aRecord0;
    when(mockedALookup0.getAnswers()).thenReturn(AAnswer0);
  
    // A lookup for 10.0.0.2
    Lookup mockedALookup1 = mock(Lookup.class);
    ARecord aRecord1 = new ARecord(Name.fromString(target1), 1, 500, node1Address);
    Record[] AAnswer1 = new Record[1];
    AAnswer1[0] = aRecord1;
    when(mockedALookup1.getAnswers()).thenReturn(AAnswer1);
    
    when(client.lookup(eq(Name.fromString(service)), eq(org.xbill.DNS.Type.SRV)))
        .thenReturn(mockedSRVLookup);
    
    when(client.lookup(eq(Name.fromString(target0)), eq(org.xbill.DNS.Type.A)))
        .thenReturn(mockedALookup0);
    when(client.lookup(eq(Name.fromString(target1)), eq(org.xbill.DNS.Type.A)))
        .thenReturn(mockedALookup1);
    
    Stream<Service> answer = client.getService(ServiceQuery.of(service, Collections.emptySet()));
    assertNotNull(answer);
    long count = answer.map(s -> {
      Service expectedService = expectedServicesMap.get(s.getAddress());
      assertNotNull(expectedService);
      assertEquals(expectedService, s);
      return s;
    }).count();
    assertEquals(2, count);
  }
  
  @Test
  public void testUnhealthyServiceMock() throws Exception {
    String service = "thisservicedoesnotexist.lc";
    Builder resolverBuilder = new Builder(Type.DNS);
    DnsResolver client = mock(DnsResolver.class);
    doCallRealMethod().when(client).init(any(Builder.class));
    when(client.getService(any())).thenCallRealMethod();
    client.init(resolverBuilder);
    
    Lookup mockedSRVLookup = mock(Lookup.class);
    when(mockedSRVLookup.getResult()).thenReturn(Lookup.TRY_AGAIN);
    
    when(client.lookup(any(), eq(org.xbill.DNS.Type.SRV)))
        .thenReturn(mockedSRVLookup);
    assertThrows(ServiceNotFoundException.class, () -> {
      client.getService(ServiceQuery.of(service, Collections.emptySet()));
    });
  }
  
  private boolean isServerRunning() {
    DatagramSocket socket = null;
    try {
      socket = new DatagramSocket(new InetSocketAddress(DNS_IP, DNS_PORT));
      socket.setReuseAddress(true);
      return false;
    } catch (IOException ex) {
      return true;
    } finally {
      if (socket != null) {
        socket.close();
      }
    }
  }
  
  
}
