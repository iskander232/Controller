package controller;

import com.google.common.collect.ImmutableList;
import controller.api.dto.ApiRequest;
import controller.api.dto.ClusterDto;
import controller.api.dto.ListenerDto;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;


@RestController
@RequestMapping("controller")
public class Api {
    SimpleSnapshot simpleSnapshot;

    Api(@Autowired SimpleSnapshot simpleSnapshot) {
        this.simpleSnapshot = simpleSnapshot;
    }

    @PostMapping("/add-config")
    public void greeting(@RequestBody ApiRequest apiRequest) {
        simpleSnapshot.setSnapshot(
            apiRequest.getNode(),
            Snapshot.create(
                ImmutableList.copyOf(apiRequest.getClusters().stream().map(ClusterDto::getCluster).collect(Collectors.toList())),
                ImmutableList.of(),
                ImmutableList.copyOf(apiRequest.getListeners().stream().map(ListenerDto::getListener).collect(Collectors.toList())),
                ImmutableList.of(),
                ImmutableList.of(),
                apiRequest.getVersion()));
    }

    @GetMapping("test")
    public ResponseEntity<String> aaa() {
        return ResponseEntity.ok("Hello world");
    }
}
