# Java client for Consul service discovery

This is a library to get services information from Consul.
There are two available resolvers.

1. Using Consul HTTP API
2. Using DNS

## Table of contents
1. [Installation](#installation)
2. [Usage](#usage)
3. [Testing](#testing)

## Installation
To use the library include the dependency in your `pom.xml` file

    <dependency>
        <groupId>com.logicalclocks</groupId>
        <artifactId>service-discovery-client</artifactId>
        <version>VERSION</version>
    </dependency>

You should also add our Maven repository.

## Usage
You can query for a service definition either with Consul HTTP API or
using the DNS interface and `SRV` records. The HTTP API is more expressive
as it can filter services also with a set of tags.

Both methods share a common interface and you can create them using the
provided `Builder`. Both methods will return a Stream of services registered
with Consul.

### HTTP API

In the following example we create an HTTP client with some options
and we lookup for a service with name `my-service-name` and some tags
associated with it. If there is no service with that name and with these
tags a `ServiceNotFoundException` will be thrown.

If you have Consul HTTP API configured with TLS, you should supply a
properly configured `SSLContext`

```java
ServiceDiscoveryClient client = null;
    try {
      client = new Builder(Type.HTTP)
          .withHttpHost("localhost")
          .withHttpPort(8501)
          .withHttps()
          .withHostnameVerifier(allowAllHostnameVerifier)
          .withSSLContext(sslContextWithConfiguredKeystores)
          .build();
      
      Set<String> tags = new HashSet<>(Arrays.asList("tag0", "tag1"));
      List<Service> services = client.getService(
          ServiceQuery.of("my-service-name", tags))
          .collect(Collectors.toList());
    } catch (ServiceDiscoveryException ex) {
      // Handle exception
    } finally {
      if (client != null) {
        client.close();
      }
    }
```

### DNS

When HTTP is not an option or you don't have access to the required keystores
you can use DNS to query Consul, provided that it's configured correctly.

The API is the same. One **important** difference is that the service name
should be the FQDN of the service. Also, you can't query with tags. By default
it will use the nameserver configured for your system e.g. `/etc/resolv.conf`
for *nix. If you want to provide another nameserver use the `Builder` methods
`withDnsHost` and `withDnsPort` - **both** must be set.

In the following example we take the first service registered with
`my-service-name.service.domain` domain name.

```java
ServiceDiscoveryClient client = null;
    try {
      client = new Builder(Type.DNS)
          .withDnsHost("127.0.0.1")
          .withDnsPort(53)
          .build();
      
      Optional<Service> service = client.getService(
          ServiceQuery.of("my-service-name.service.domain", Collections.emptySet()))
          .findFirst();
    } catch (ServiceDiscoveryException ex) {
      // Handle exception
    } finally {
      if (client != null) {
        client.close();
      }
    }
```

### Caching

`CachingResolver` is a Type of Resolver wrapping around the HTTP and DNS API and caching results for a configurable period
of time. When building the `CachingResolver` you can pass the underline resolver to use, otherwise it will default
to DNS queries on the local nameserver on port 53.

For example to create a caching client using the HTTP API see the snippet below. Mind the `Type.CACHING` when
building the cached client. In the following example the results will be invalidated after 30 seconds.

```java
ServiceDiscoveryClient client = null;
    try {
      ServiceDiscoveryClient httpClient = new Builder(Type.HTTP)
              .withHttpHost("localhost")
              .withHttpPort(8501)
              .withHttps()
              .withHostnameVerifier(allowAllHostnameVerifier)
              .withSSLContext(sslContextWithConfiguredKeystores)
              .build();
      
      client = new Builder(Type.CACHING)
              .withServiceDiscoveryClient(httpClient)
              .withCacheExpiration(Duration.of(30, ChronoUnit.SECONDS))
              .build();

      Set<String> tags = new HashSet<>(Arrays.asList("tag0", "tag1"));
      List<Service> services = client.getService(
              ServiceQuery.of("my-service-name", tags))
              .collect(Collectors.toList());
    } catch (ServiceDiscoveryException ex) {
      // Handle exception
    } finally {
      if (client != null) {
        client.close();
      }
    }
```

## Testing
There are tests that run against a real Consul installation in addition to
mocked tests. For the real tests to run you need the following.

### HTTP API tests
If you have secured Consul REST API with TLS copy the valid keystores and the
password to unlock them in:

* `/tmp/keystore.jks`
* `/tmp/truststore.jks`
* `/tmp/passphrase`

Assuming that the Consul agent runs on `my-remote-machine` and on port `8501` create
an SSH tunnel `ssh -L8501:localhost:8501 user@my-remote-machine`

### DNS tests
To run tests against a properly configured Consul installation at `my-remote-machine`
you need to:

1. `server# socat tcp4-listen:15353,reuseaddr,fork,bind=127.0.0.1 UDP:127.0.0.1:53`
2. `client# ssh -L 15353:localhost:15353 user@my-remote-machine`
3. `client# socat udp4-listen:5453,reuseaddr,fork tcp:localhost:15353`

Then any DNS request made to `127.0.0.1:5453` will be routed to the remote.

If none of the above hold, the tests will be skipped and only the mocked will run.