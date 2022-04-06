package controller;

import io.envoyproxy.controlplane.cache.NodeGroup;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.grpc.BindableService;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class Configuration {

    @Bean
    public SnapshotsManager snapshotsManager() {
        return new SnapshotsManager();
    }

    @Bean
    public SimpleSnapshot getSimpleSnapshot(SnapshotsManager snapshotsManager) {
        return new SimpleSnapshot(snapshotsManager);
    }

    @Bean
    public V3DiscoveryServer v3DiscoveryServer(SimpleSnapshot simpleSnapshot) {
        return new V3DiscoveryServer(simpleSnapshot);
    }

    @GrpcService
    public BindableService x1(V3DiscoveryServer v3DiscoveryServer) {
        return v3DiscoveryServer.getAggregatedDiscoveryServiceImpl();
    }

    @GrpcService
    public BindableService x2(V3DiscoveryServer v3DiscoveryServer) {
        return v3DiscoveryServer.getClusterDiscoveryServiceImpl();
    }

    @GrpcService
    public BindableService x3(V3DiscoveryServer v3DiscoveryServer) {
        return v3DiscoveryServer.getEndpointDiscoveryServiceImpl();
    }

    @GrpcService
    public BindableService x4(V3DiscoveryServer v3DiscoveryServer) {
        return v3DiscoveryServer.getListenerDiscoveryServiceImpl();
    }

    @GrpcService
    public BindableService x5(V3DiscoveryServer v3DiscoveryServer) {
        return v3DiscoveryServer.getRouteDiscoveryServiceImpl();
    }
}
