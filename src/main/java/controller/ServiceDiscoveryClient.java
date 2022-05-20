package controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import controller.api.dto.EnvoyConfig;
import controller.api.dto.ConfigUpdateStatusResponseRaw;
import controller.api.dto.ConfigUpdateStatusResponse;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.envoy.config.core.v3.Node;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServiceDiscoveryClient {

    RestTemplate restTemplate;

    String SERVICE_DISCOVERY_GROUP_URL = "http://localhost:8080/update-result?cluster_id=%s&node_id=%s";
    String SERVICE_DISCOVERY_UPDATE_URL = "http://localhost:8080/service/version/deploy";

    ObjectMapper mapper = new ObjectMapper();

    public ServiceDiscoveryClient(RestTemplateBuilder restTemplateBuilder) {
        restTemplate = restTemplateBuilder.build();
    }

    public Snapshot getSnapshot(Node node) {
        log.info("[ServiceDiscoveryClient] get snapshot for node cluster = {}, id = {}", node.getCluster(), node.getId());

        try {
            EnvoyConfig request = restTemplate.getForEntity(String.format(SERVICE_DISCOVERY_GROUP_URL, node.getCluster(), node.getId()), EnvoyConfig.class).getBody();
            log.info("[ServiceDiscoveryClient] request = {}", request);
            return request != null ? request.buildSnapshot() : null;
        } catch (HttpClientErrorException e){
            log.error("[ServiceDiscoveryClient] getSnapshot error", e);
            return null;
        }
    }

    public void updateStatus(ConfigUpdateStatusResponseRaw configUpdateStatusResponseRaw) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        try {
            String json = mapper.writeValueAsString(new ConfigUpdateStatusResponse(configUpdateStatusResponseRaw));
            log.info(json);
            HttpEntity<String> response = restTemplate.exchange(SERVICE_DISCOVERY_UPDATE_URL, HttpMethod.PUT, new HttpEntity<>(json, headers), String.class);
            log.info("[ServiceDiscoveryClient] updateStatus apiResponse = {}, ServiceDiscoveryResponse = {}", configUpdateStatusResponseRaw, response.getBody());
        } catch (HttpClientErrorException | JsonProcessingException e) {
            log.error("[ServiceDiscoveryClient] updateStatus error", e);
        }
    }
}
