package controller.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
public class ApiResponse2 {
    String service_id;
    String version;
    String error;

    public ApiResponse2(ApiResponse a) {
        this.service_id = a.envoy_id.cluster_id;
        this.version = a.version;
        this.error = a.error;
    }
}
