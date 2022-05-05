package controller;

import controller.api.dto.ClusterDto;
import controller.api.dto.EndpointMapping;
import controller.api.dto.EnvoyId;
import controller.api.dto.ListenerDto;
import io.envoyproxy.envoy.config.core.v3.Node;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class Utils {
    public String hash(Node node) {
        return String.format("cluster - %s, id - %s", node.getCluster(), node.getId());
    }

    public String hash(EnvoyId envoyId) {
        return String.format("cluster - %s, id - %s", envoyId.getCluster_id(), envoyId.getNode_id());
    }

    public EnvoyId nodeToId(Node node) {
        return EnvoyId.builder()
            .cluster_id(node.getCluster())
            .node_id(node.getId())
            .build();
    }

    public Pair<List<ListenerDto>, List<ClusterDto>> processEndpoint(List<EndpointMapping> endpointDtos) {
        List<Pair<ListenerDto, ClusterDto>> listenerToCluster = new ArrayList<>();
        for (EndpointMapping e: endpointDtos) {
            boolean used = false;
            for (Pair<ListenerDto, ClusterDto> listenerDtoClusterDtoPair : listenerToCluster) {
                if (listenerDtoClusterDtoPair.getLeft().getListenerEndpoint().equals(e.getFrom())) {
                    listenerDtoClusterDtoPair.getRight().addEndpoint(e.getTo());
                    used = true;
                    break;
                }
            }
            if (!used) {
                String endpointName = e.getName() != null ? e.getName() : "endpoint_hash_code_" + e.hashCode();
                String clusterName = "cluster_" + endpointName;
                String listenerName = "listener_" + endpointName;
                listenerToCluster.add(Pair.of(new ListenerDto(e.getFrom(), listenerName, clusterName), new ClusterDto(e.getTo(), clusterName)));
            }
        }
        List<ClusterDto> clusters = new ArrayList<>();
        List<ListenerDto> listeners = new ArrayList<>();
        listenerToCluster.forEach(p -> {
            clusters.add(p.getRight());
            listeners.add(p.getLeft());
        });
        return Pair.of(listeners, clusters);
    }
}
