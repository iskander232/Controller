package controller.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.protobuf.Duration;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.endpoint.v3.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Builder
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClusterDto {
    @NonNull
    private String name;
    @NonNull
    private List<EndpointDto> endpointDtos;

    public Cluster getCluster() {
        return Cluster.newBuilder()
            .setName(name)
            .setConnectTimeout(Duration.newBuilder().setSeconds(5))
            .setType(Cluster.DiscoveryType.STRICT_DNS)
            .setDnsLookupFamily(Cluster.DnsLookupFamily.V4_ONLY)
            .setLoadAssignment(ClusterLoadAssignment.newBuilder()
                .setClusterName(name)
                .addEndpoints(getLbEndpoints())
                .build())
            .build();
    }

    private LocalityLbEndpoints getLbEndpoints() {
        List<LbEndpoint> endpoints = endpointDtos.stream()
            .map(EndpointDto::getAddress)
            .map(a -> LbEndpoint.newBuilder()
                .setEndpoint(Endpoint.newBuilder()
                    .setAddress(a)
                    .build())
                .build())
            .collect(Collectors.toList());
        LocalityLbEndpoints.Builder builder = LocalityLbEndpoints.newBuilder();

        endpoints.forEach(builder::addLbEndpoints);

        return builder.build();
    }

    public ClusterDto(EndpointDto endpointDto, String clusterName) {
        this.name = clusterName;
        this.endpointDtos = new ArrayList<>();
        addEndpoint(endpointDto);
    }

    public void addEndpoint(EndpointDto endpointDto) {
        endpointDtos.add(endpointDto);
    }
}
