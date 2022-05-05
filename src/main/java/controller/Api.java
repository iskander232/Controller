package controller;

import controller.api.dto.Apirequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("controller")
@Slf4j
public class Api {
    ApiRequestsQueue apiRequestsQueue;

    Api(@Autowired ApiRequestsQueue apiRequestsQueue) {
        this.apiRequestsQueue = apiRequestsQueue;
    }

    @PostMapping("/add-config")
    public void greeting(@RequestBody Apirequest apiRequest) {
        log.info("add-config + " + apiRequest);
        apiRequestsQueue.addApiRequest(apiRequest);
    }

    @GetMapping("test")
    public ResponseEntity<String> aaa() {
        return ResponseEntity.ok("Hello world");
    }
}
