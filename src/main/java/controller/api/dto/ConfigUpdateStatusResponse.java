package controller.api.dto;

import lombok.Data;

@Data
public class ConfigUpdateStatusResponse {
    String service_id;
    String version;
    String error;

    public ConfigUpdateStatusResponse(ConfigUpdateStatusResponseRaw a) {
        this.service_id = a.envoy_id.cluster_id;
        this.version = a.version;
        this.error = a.error;
    }
}
