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
package com.logicalclocks.servicediscoveryclient.resolvers;

import com.logicalclocks.servicediscoverclient.Builder;
import com.logicalclocks.servicediscoverclient.ServiceDiscoveryClient;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.health.ImmutableNode;
import com.orbitz.consul.model.health.ImmutableService;
import com.orbitz.consul.model.health.ImmutableServiceHealth;
import com.orbitz.consul.model.health.Node;
import com.orbitz.consul.model.health.ServiceHealth;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestHttpResolver {
  private final Path KEYSTORE = Paths.get(System.getProperty("java.io.tmpdir"), "keystore.jks");
  private final Path TRUSTSTORE = Paths.get(System.getProperty("java.io.tmpdir"), "truststore.jks");
  private final Path PASSPHRASE = Paths.get(System.getProperty("java.io.tmpdir"), "passphrase");
  private final BigInteger RESPONSE_INDEX = BigInteger.ONE;
  
  private ServiceDiscoveryClient client;
  
  @AfterEach
  public void afterAll() {
    if (client != null) {
      client.close();
    }
  }
  
  @Test
  public void testWithConsulAgentHealthyService() throws Exception {
    assumeTrue(KEYSTORE.toFile().exists());
    assumeTrue(TRUSTSTORE.toFile().exists());
    assumeTrue(PASSPHRASE.toFile().exists());
    SSLContext sslContext = createSSLContext();
    client = new Builder(Type.HTTP)
        .withHttpPort(8501)
        .withHttps()
        .withSSLContext(sslContext)
        .withHostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String s, SSLSession sslSession) {
            return true;
          }
        })
        .build();
    Stream<Service> namenodes = client.getService(ServiceQuery.of("namenode", Collections.emptySet()));
    assertNotNull(namenodes);
    assertTrue(namenodes.count() > 0);
  }
  
  @Test
  public void testWithConsulAgentHealthyServiceWithTags() throws Exception {
    assumeTrue(KEYSTORE.toFile().exists());
    assumeTrue(TRUSTSTORE.toFile().exists());
    assumeTrue(PASSPHRASE.toFile().exists());
    SSLContext sslContext = createSSLContext();
    client = new Builder(Type.HTTP)
        .withHttpPort(8501)
        .withHttps()
        .withSSLContext(sslContext)
        .withHostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String s, SSLSession sslSession) {
            return true;
          }
        })
        .build();
    Set<String> tags = new HashSet<>();
    tags.add("rpc");
    Stream<Service> namenodes = client.getService(ServiceQuery.of("namenode", tags));
    assertNotNull(namenodes);
    assertTrue(namenodes.count() > 0);
  }
  
  @Test
  public void testWithConsulAgentUnhealthyService() throws Exception {
    assumeTrue(KEYSTORE.toFile().exists());
    assumeTrue(TRUSTSTORE.toFile().exists());
    assumeTrue(PASSPHRASE.toFile().exists());
    SSLContext sslContext = createSSLContext();
    client = new Builder(Type.HTTP)
        .withHttpPort(8501)
        .withHttps()
        .withSSLContext(sslContext)
        .withHostnameVerifier(new HostnameVerifier() {
          @Override
          public boolean verify(String s, SSLSession sslSession) {
            return true;
          }
        })
        .build();
    assertThrows(ServiceNotFoundException.class, () -> {
      client.getService(ServiceQuery.of("thisservicedoesnotexist", Collections.emptySet()));
    });
  }
  
  @Test
  public void testSuccessMock() throws Exception {
    Node node0 = ImmutableNode.builder().node("node0").address("10.0.0.1").build();
    Node node1 = ImmutableNode.builder().node("node1").address("10.0.0.2").build();
    com.orbitz.consul.model.health.Service service0 =
        ImmutableService.builder().id("s0").service("service0").address("10.0.0.1").port(8080).build();
    com.orbitz.consul.model.health.Service service1 =
        ImmutableService.builder().id("s0").service("service0").address("10.0.0.2").port(8080).build();
  
    ServiceHealth sh0 = ImmutableServiceHealth.builder().node(node0).service(service0).build();
    ServiceHealth sh1 = ImmutableServiceHealth.builder().node(node1).service(service1).build();
    Map<String, ServiceHealth> serviceHealthMap = new HashMap<>(2);
    serviceHealthMap.put("10.0.0.1", sh0);
    serviceHealthMap.put("10.0.0.2", sh1);
    
    List<ServiceHealth> response = new ArrayList<>(2);
    response.add(sh0);
    response.add(sh1);
    
    HealthClient hc = mock(HealthClient.class);
    when(hc.getHealthyServiceInstances(any(), any())).thenReturn(constructConsulResponse(response));
    Consul consulClient = mock(Consul.class);
    when(consulClient.healthClient()).thenReturn(hc);
    ServiceDiscoveryClient client = new Builder(Type.HTTP)
        .withClient(consulClient)
        .build();
    
    ServiceQuery sq = ServiceQuery.of("service0", Collections.emptySet());
    Stream<Service> reply = client.getService(sq);
    assertNotNull(reply);
    long count = reply.map(s -> {
      String serviceAddress = s.getAddress();
      ServiceHealth sh = serviceHealthMap.get(serviceAddress);
      assertNotNull(sh);
      assertEquals(sh.getService().getService(), s.getName());
      assertEquals(Integer.valueOf(sh.getService().getPort()), s.getPort());
      return s;
    }).count();
    assertEquals(2, count);
  }
  
  @Test
  public void testNoHealthyNodesMock() throws Exception {
    List<ServiceHealth> response = new ArrayList<>(0);
    HealthClient hc = mock(HealthClient.class);
    when(hc.getHealthyServiceInstances(any(), any())).thenReturn(constructConsulResponse(response));
    Consul consulClient = mock(Consul.class);
    when(consulClient.healthClient()).thenReturn(hc);
    ServiceDiscoveryClient client = new Builder(Type.HTTP)
        .withClient(consulClient)
        .build();
  
    ServiceQuery sq = ServiceQuery.of("service0", Collections.emptySet());
    assertThrows(ServiceNotFoundException.class, () -> {
      client.getService(sq);
    });
  }
  
  private SSLContext createSSLContext() throws Exception {
    String passphrase = new String(Files.readAllBytes(PASSPHRASE));
    passphrase = passphrase.trim();
    char[] pass = passphrase.toCharArray();
    return SSLContexts.custom()
        .loadKeyMaterial(KEYSTORE.toFile(), pass, pass)
        .loadTrustMaterial(TRUSTSTORE.toFile(), pass, new TrustStrategy() {
          @Override
          public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            return true;
          }
        })
        .build();
  }
  
  private <T> ConsulResponse<T> constructConsulResponse(T response) {
    return new ConsulResponse<>(response, System.currentTimeMillis(), true, RESPONSE_INDEX, "", "100");
  }
}
