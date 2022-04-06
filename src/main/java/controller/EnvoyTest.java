package controller;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import io.envoyproxy.controlplane.cache.NodeGroup;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import io.envoyproxy.envoy.config.endpoint.v3.ClusterLoadAssignment;
import io.envoyproxy.envoy.config.endpoint.v3.Endpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LbEndpoint;
import io.envoyproxy.envoy.config.endpoint.v3.LocalityLbEndpoints;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.*;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.ServerCalls;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

import static java.lang.Thread.sleep;


public class EnvoyTest {
//    public static String GROUP = "aaa";
//
//    public static void main(String[] args) throws IOException, InterruptedException {
//        SimpleSnapshot<String, Snapshot> simpleSnapshot = new SimpleSnapshot<>(new NodeGroup<>() {
//            @Override
//            public String hash(io.envoyproxy.envoy.api.v2.core.Node node) {
//                return GROUP;
//            }
//
//            @Override
//            public String hash(io.envoyproxy.envoy.config.core.v3.Node node) {
//                return GROUP;
//            }
//        });
//        int version = 0;
//        HttpConnectionManager manager = HttpConnectionManager.newBuilder()
//            .setCodecType(HttpConnectionManager.CodecType.AUTO)
//            .setStatPrefix("echo")
//            .setRouteConfig(RouteConfiguration.newBuilder()
//                .setName("local_route")
//                .addVirtualHosts(VirtualHost.newBuilder()
//                        .setName("local_service")
//                        .addDomains("*")
//                        .addRoutes(Route.newBuilder()
//                            .setMatch(RouteMatch.newBuilder()
//                                .setPrefix("/")
//                                .build())
//                            .setRoute(RouteAction.newBuilder()
//                                .setCluster("echo_cluster")
//                                .build())
//                            .build())
//                    .build())
//                .build())
//            .addHttpFilters(HttpFilter.newBuilder().setName("envoy.filters.http.router").build())
//            .build();
//        simpleSnapshot.setSnapshot(
//                GROUP,
//                Snapshot.create(
//                    ImmutableList.of(
//                        Cluster.newBuilder()
//                            .setName("echo_cluster")
//                            .setConnectTimeout(Duration.newBuilder().setSeconds(5))
//                            .setType(Cluster.DiscoveryType.STRICT_DNS)
//                            .setDnsLookupFamily(Cluster.DnsLookupFamily.V4_ONLY)
//                            .setLoadAssignment(ClusterLoadAssignment.newBuilder()
//                                .setClusterName("echo_cluster")
//                                    .addEndpoints(LocalityLbEndpoints.newBuilder()
//                                        .addLbEndpoints(LbEndpoint.newBuilder()
//                                                .setEndpoint(Endpoint.newBuilder()
//                                                    .setAddress(Address.newBuilder()
//                                                        .setSocketAddress(SocketAddress.newBuilder()
//                                                            .setAddress("127.0.0.1")
//                                                            .setPortValue(8080)
//                                                            .build())
//                                                        .build())
//                                                    .build())
//                                            .build())
//                                        .build())
//                                .build())
//                            .build()),
//                    ImmutableList.of(),
//                    ImmutableList.of(Listener.newBuilder()
//                        .setName("listenerName")
//                        .setAddress(Address.newBuilder()
//                            .setSocketAddress(SocketAddress.newBuilder()
//                                .setAddress("127.0.0.1")
//                                .setPortValue(10000)))
//                        .addFilterChains(FilterChain.newBuilder()
//                            .addFilters(Filter.newBuilder()
//                                .setName("envoy.http_connection_manager")
//                                .setTypedConfig(Any.pack(manager))))
//                        .build()),
//                    ImmutableList.of(),
//                    ImmutableList.of(),
//                    Integer.valueOf(version).toString()));
//
//        V3DiscoveryServer v3DiscoveryServer = new V3DiscoveryServer(simpleSnapshot);
//
//
////        ServerBuilder<NettyServerBuilder> builder = NettyServerBuilder.forPort(18000)
////            .addService(v3DiscoveryServer.getAggregatedDiscoveryServiceImpl())
////            .addService(v3DiscoveryServer.getClusterDiscoveryServiceImpl())
////            .addService(v3DiscoveryServer.getEndpointDiscoveryServiceImpl())
////            .addService(v3DiscoveryServer.getListenerDiscoveryServiceImpl())
////            .addService(v3DiscoveryServer.getRouteDiscoveryServiceImpl());
//
////        Server server = builder.build();
////        server.start();
//
////        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
//
//        int port = 8080;
//        while (true) {
//            port = 8000 + 8080 - port;
//            sleep(3000);
//            version += 1;
//            simpleSnapshot.setSnapshot(
//                    GROUP,
//                    Snapshot.create(
//                            ImmutableList.of(
//                                    Cluster.newBuilder()
//                                            .setName("echo_cluster")
//                                            .setConnectTimeout(Duration.newBuilder().setSeconds(5))
//                                            .setType(Cluster.DiscoveryType.STRICT_DNS)
//                                            .setDnsLookupFamily(Cluster.DnsLookupFamily.V4_ONLY)
//                                            .setLoadAssignment(ClusterLoadAssignment.newBuilder()
//                                                    .setClusterName("echo_cluster")
//                                                    .addEndpoints(LocalityLbEndpoints.newBuilder()
//                                                            .addLbEndpoints(LbEndpoint.newBuilder()
//                                                                    .setEndpoint(Endpoint.newBuilder()
//                                                                            .setAddress(Address.newBuilder()
//                                                                                    .setSocketAddress(SocketAddress.newBuilder()
//                                                                                            .setAddress("127.0.0.1")
//                                                                                            .setPortValue(port)
//                                                                                            .build())
//                                                                                    .build())
//                                                                            .build())
//                                                                    .build())
//                                                            .build())
//                                                    .build())
//                                            .build()),
//                            ImmutableList.of(),
//                            ImmutableList.of(Listener.newBuilder()
//                                    .setName("listenerName")
//                                    .setAddress(Address.newBuilder()
//                                            .setSocketAddress(SocketAddress.newBuilder()
//                                                    .setAddress("127.0.0.1")
//                                                    .setPortValue(10000)))
//                                    .addFilterChains(FilterChain.newBuilder()
//                                            .addFilters(Filter.newBuilder()
//                                                    .setName("envoy.http_connection_manager")
//                                                    .setTypedConfig(Any.pack(manager))))
//                                    .build()),
//                            ImmutableList.of(),
//                            ImmutableList.of(),
//                            Integer.valueOf(version).toString()));
//        }
//    }
}
