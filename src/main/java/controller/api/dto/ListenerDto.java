package controller.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.protobuf.Any;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.listener.v3.Filter;
import io.envoyproxy.envoy.config.listener.v3.FilterChain;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.config.route.v3.*;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpFilter;
import lombok.*;

@AllArgsConstructor
@Builder
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ListenerDto {

    @NonNull
    String name;
    @NonNull
    String statPrefix;
    @NonNull
    String clusterName;
    @NonNull
    @Getter
    EndpointDto listenerEndpoint;

    public Listener getListener() {
        return Listener.newBuilder()
            .setName(name)
            .setAddress(Address.newBuilder()
                .setSocketAddress(listenerEndpoint.getSocketAddress())
                .build())
            .addFilterChains(FilterChain.newBuilder()
                .addFilters(Filter.newBuilder()
                    .setName("envoy.http_connection_manager")
                    .setTypedConfig(Any.pack(getHttpConnectionManager()))))
            .build();
    }

    private HttpConnectionManager getHttpConnectionManager() {
        return HttpConnectionManager.newBuilder()
            .setStatPrefix(statPrefix)
            .setRouteConfig(RouteConfiguration.newBuilder()
                .setName(name)
                .addVirtualHosts(VirtualHost.newBuilder()
                    .setName(name)
                    .addDomains("*")
                    .addRoutes(Route.newBuilder()
                        .setMatch(RouteMatch.newBuilder()
                            .setPrefix("/")
                            .build())
                        .setRoute(RouteAction.newBuilder()
                            .setCluster(clusterName)
                            .build())
                        .build())
                    .build())
                .build())
            .addHttpFilters(HttpFilter.newBuilder().setName("envoy.filters.http.router").build())
            .build();
    }

    public ListenerDto(EndpointDto endpointDto, String name, String clusterName) {
        this.clusterName = clusterName;
        this.listenerEndpoint = endpointDto;
        this.name = name;
        this.statPrefix = "/";
    }
}
