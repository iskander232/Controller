package controller.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.collect.ImmutableList;
import controller.Utils;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ApiRequestPart {
    String version;
    EnvoyId envoy_id;
    EndpointDto monitoring_endpoint;
    List<EndpointMapping> endpoint_mappings;

    public Snapshot buildSnapshot() {
        var processedEndpoints = Utils.processEndpoint(endpoint_mappings);
        return Snapshot.create(
            ImmutableList.copyOf(processedEndpoints.getRight().stream().map(ClusterDto::getCluster).collect(Collectors.toList())),
            ImmutableList.of(),
            ImmutableList.copyOf(processedEndpoints.getLeft().stream().map(ListenerDto::getListener).collect(Collectors.toList())),
            ImmutableList.of(),
            ImmutableList.of(),
            version);
    }
}
