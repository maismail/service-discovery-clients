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
import com.logicalclocks.servicediscoverclient.resolvers.ServiceNotFoundException;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.logicalclocks.servicediscoverclient.service.Service;
import com.logicalclocks.servicediscoverclient.service.ServiceQuery;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;

public class TestHttpResolver {
  private final Path KEYSTORE = Paths.get(System.getProperty("java.io.tmpdir"), "keystore.jks");
  private final Path TRUSTSTORE = Paths.get(System.getProperty("java.io.tmpdir"), "truststore.jks");
  private final Path PASSPHRASE = Paths.get(System.getProperty("java.io.tmpdir"), "passphrase");
  
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
    List<Service> namenodes = client.getService(ServiceQuery.of("namenode", Collections.EMPTY_SET));
    assertNotNull(namenodes);
    assertFalse(namenodes.isEmpty());
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
    List<Service> namenodes = client.getService(ServiceQuery.of("namenode", tags));
    assertNotNull(namenodes);
    assertFalse(namenodes.isEmpty());
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
}
