package controller;

import controller.api.dto.AddConfigsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("controller")
@Slf4j
public class Api {
    AddConfigRequestsQueue addConfigRequestsQueue;

    Api(@Autowired AddConfigRequestsQueue addConfigRequestsQueue) {
        this.addConfigRequestsQueue = addConfigRequestsQueue;
    }

    @PostMapping("/add-config")
    public void greeting(@RequestBody AddConfigsRequest apiRequest) {
        log.info("add-config + " + apiRequest);
        addConfigRequestsQueue.addApiRequest(apiRequest);
    }
}
