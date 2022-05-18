package controller;

import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.grpc.BindableService;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ControllerConfiguration {

    @Bean
    public ServiceDiscoveryClient serviceDiscoveryClient(RestTemplateBuilder restTemplateBuilder) {
        return new ServiceDiscoveryClient(restTemplateBuilder);
    }

    @Bean
    public SimpleSnapshot simpleSnapshot(ServiceDiscoveryClient serviceDiscoveryClient) {
        return new SimpleSnapshot(serviceDiscoveryClient);
    }

    @Bean
    public ApiRequestsQueue apiRequestsQueue(SimpleSnapshot simpleSnapshot) {
        return new ApiRequestsQueue(simpleSnapshot);
    }

    @Bean
    public V3DiscoveryServer v3DiscoveryServer(SimpleSnapshot simpleSnapshot) {
        return new V3DiscoveryServer(simpleSnapshot);
    }

    @GrpcService
    public BindableService x1(V3DiscoveryServer v3DiscoveryServer) {
        return v3DiscoveryServer.getAggregatedDiscoveryServiceImpl();
    }
}
