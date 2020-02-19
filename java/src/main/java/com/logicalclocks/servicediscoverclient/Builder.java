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
package com.logicalclocks.servicediscoverclient;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.resolvers.DnsResolver;
import com.logicalclocks.servicediscoverclient.resolvers.HttpResolver;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.orbitz.consul.Consul;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;


public final class Builder {
  private final Type resolverType;
  
  private String httpHost = "localhost";
  private Integer httpPort = 8500;
  private Boolean https = false;
  private SSLContext sslContext;
  private HostnameVerifier hostnameVerifier;
  private Consul client;
  
  public Builder(Type resolverType) {
    this.resolverType = resolverType;
  }
  
  public Builder withHttpHost(String httpHost) {
    this.httpHost = httpHost;
    return this;
  }
  
  public Builder withHttpPort(Integer httpPort) {
    this.httpPort = httpPort;
    return this;
  }
  
  public Builder withHttps() {
    this.https = true;
    return this;
  }
  
  public Builder withSSLContext(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }
  
  public Builder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
    return this;
  }
  
  public Builder withClient(Consul client) {
    this.client = client;
    return this;
  }
  
  public String getHttpHost() {
    return httpHost;
  }
  
  public Integer getHttpPort() {
    return httpPort;
  }
  
  public Boolean getHttps() {
    return https;
  }
  
  public SSLContext getSslContext() {
    return sslContext;
  }
  
  public HostnameVerifier getHostnameVerifier() {
    return hostnameVerifier;
  }
  
  public Consul getClient() {
    return client;
  }
  
  public ServiceDiscoveryClient build() throws ServiceDiscoveryException  {
    ServiceDiscoveryClient client;
    switch (resolverType) {
      case DNS:
        client = new DnsResolver();
        break;
      case HTTP:
        client = new HttpResolver();
        break;
      default:
        throw new RuntimeException("Unknown service discovery resolver type: " + resolverType);
    }
    client.init(this);
    return client;
  }
}
