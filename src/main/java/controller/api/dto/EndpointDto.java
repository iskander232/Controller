package controller.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.envoyproxy.envoy.config.core.v3.Address;
import io.envoyproxy.envoy.config.core.v3.SocketAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EndpointDto {
    private int port;
    private String address;

    public Address getAddress() {
        return Address.newBuilder()
            .setSocketAddress(getSocketAddress())
            .build();
    }

    public SocketAddress getSocketAddress() {
        return SocketAddress.newBuilder()
            .setAddress(address)
            .setPortValue(port)
            .build();
    }
}
