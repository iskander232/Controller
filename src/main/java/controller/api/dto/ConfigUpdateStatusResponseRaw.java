package controller.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConfigUpdateStatusResponseRaw {

    EnvoyId envoy_id;
    String version;
    Collection<String> resources;
    String error;
}
