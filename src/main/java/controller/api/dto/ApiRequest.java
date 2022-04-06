package controller.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@Builder
public class ApiRequest {
    @NonNull
    String version;
    @NonNull
    String node;
    @NonNull
    List<ListenerDto> listeners;
    @NonNull
    List<ClusterDto> clusters;
}
