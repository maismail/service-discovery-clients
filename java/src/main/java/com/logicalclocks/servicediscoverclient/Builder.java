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
package com.logicalclocks.servicediscoverclient;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.resolvers.CachingResolver;
import com.logicalclocks.servicediscoverclient.resolvers.DnsResolver;
import com.logicalclocks.servicediscoverclient.resolvers.HttpResolver;
import com.logicalclocks.servicediscoverclient.resolvers.Type;
import com.orbitz.consul.Consul;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.time.temporal.ChronoUnit;


public class Builder {
  private final Type resolverType;
  
  // For HTTP
  private String httpHost = "localhost";
  private Integer httpPort = 8500;
  private Boolean https = false;
  private SSLContext sslContext;
  private HostnameVerifier hostnameVerifier;
  private Consul client;
  
  // For DNS
  private String dnsHost = null;
  private Integer dnsPort = null;

  // For caching
  private ServiceDiscoveryClient serviceDiscoveryClient;
  private Duration cacheExpiration = Duration.of(1, ChronoUnit.MINUTES);
  
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
  
  public Builder withDnsHost(String dnsHost) {
    this.dnsHost = dnsHost;
    return this;
  }
  
  public Builder withDnsPort(Integer dnsPort) {
    this.dnsPort = dnsPort;
    return this;
  }

  public Builder withServiceDiscoveryClient(ServiceDiscoveryClient serviceDiscoveryClient) {
    this.serviceDiscoveryClient = serviceDiscoveryClient;
    return this;
  }

  public Builder withCacheExpiration(Duration cacheExpiration) {
    this.cacheExpiration = cacheExpiration;
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
  
  public String getDnsHost() {
    return dnsHost;
  }
  
  public Integer getDnsPort() {
    return dnsPort;
  }

  public ServiceDiscoveryClient getServiceDiscoveryClient() {
    return serviceDiscoveryClient;
  }

  public Duration getCacheExpiration() {
    return cacheExpiration;
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
      case CACHING:
        client = new CachingResolver();
        break;
      default:
        throw new RuntimeException("Unknown service discovery resolver type: " + resolverType);
    }
    client.init(this);
    return client;
  }
}
