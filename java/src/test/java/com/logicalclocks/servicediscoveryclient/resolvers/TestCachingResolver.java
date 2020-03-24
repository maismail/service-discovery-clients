package com.logicalclocks.servicediscoveryclient.resolvers;

import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.resolvers.CachingResolver;
import com.logicalclocks.servicediscoverclient.resolvers.DnsResolver;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class TestCachingResolver {

    @Test
    public void testCacheWithDNSResolver() throws Exception {
        String service = "namenode.service.lc.";
        int servicePort = 8020;
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

        Builder cachingResolverBuilder = new Builder(Type.CACHING)
                .withServiceDiscoveryClient(client);
        CachingResolver cachingResolver = mock(CachingResolver.class);
        doCallRealMethod().when(cachingResolver).init(any(Builder.class));
        when(cachingResolver.getService(any(ServiceQuery.class))).thenCallRealMethod();
        cachingResolver.init(cachingResolverBuilder);

        ServiceQuery query = ServiceQuery.of(service, Collections.emptySet());
        Stream<Service> answer = cachingResolver.getService(query);
        assertNotNull(answer);
        long count = answer.map(s -> {
            Service expectedService = expectedServicesMap.get(s.getAddress());
            assertNotNull(expectedService);
            assertEquals(expectedService, s);
            return s;
        }).count();
        assertEquals(2, count);

        // Real resolver must have been called this time
        verify(cachingResolver, times(1)).getService(query);
        verify(client, times(1)).getService(eq(query));

        answer = cachingResolver.getService(query);
        assertNotNull(answer);
        count = answer.map(s -> {
            Service expectedService = expectedServicesMap.get(s.getAddress());
            assertNotNull(expectedService);
            assertEquals(expectedService, s);
            return s;
        }).count();
        assertEquals(2, count);

        // Now it should hit the cache
        verify(cachingResolver, times(2)).getService(query);
        verify(client, times(1)).getService(eq(query));
    }

    @Test
    public void testCachingResolverNoAnswer() throws Exception {
        String service = "thisservicedoesnotexist.lc";
        Builder resolverBuilder = new Builder(Type.DNS)
                .withDnsHost("localhost")
                .withDnsPort(53);
        DnsResolver client = mock(DnsResolver.class);
        doCallRealMethod().when(client).init(any(Builder.class));
        when(client.getService(any())).thenCallRealMethod();
        client.init(resolverBuilder);

        Lookup mockedSRVLookup = mock(Lookup.class);
        when(mockedSRVLookup.getResult()).thenReturn(Lookup.TRY_AGAIN);
        when(client.lookup(any(), eq(org.xbill.DNS.Type.SRV))).thenReturn(mockedSRVLookup);

        Builder cachingResolverBuilder = new Builder(Type.CACHING).withServiceDiscoveryClient(client);
        CachingResolver cachingResolver = mock(CachingResolver.class);
        doCallRealMethod().when(cachingResolver).init(any(Builder.class));
        when(cachingResolver.getService(any(ServiceQuery.class))).thenCallRealMethod();
        cachingResolver.init(cachingResolverBuilder);

        assertThrows(ServiceNotFoundException.class, () -> {
            cachingResolver.getService(ServiceQuery.of(service, Collections.emptySet()));
        });
    }
}
