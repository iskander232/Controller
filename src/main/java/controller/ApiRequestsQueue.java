package controller;

import controller.api.dto.ApiRequestPart;
import controller.api.dto.Apirequest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ApiRequestsQueue {

    ReentrantLock lock = new ReentrantLock();
    Deque<ApiRequestPart> snapshotDeque = new ArrayDeque<>();
    SimpleSnapshot simpleSnapshot;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    boolean isProcessing = false;

    ApiRequestsQueue(SimpleSnapshot simpleSnapshot) {
        this.simpleSnapshot = simpleSnapshot;
    }

    public void addApiRequest(Apirequest apiRequest) {
        lock.lock();
        try {
            for (ApiRequestPart p: apiRequest.getConfigs()) {
                p.setVersion(apiRequest.getVersion());
                snapshotDeque.addLast(p);
            }
            if (!isProcessing) {
                isProcessing = true;
                executorService.execute(this::readNext);
            }
        } finally {
            lock.unlock();
        }
    }

    private void readNext() {
        ApiRequestPart apiRequest;
        while (true) {
            lock.lock();
            try {
                apiRequest = snapshotDeque.pollFirst();
                if (apiRequest == null) {
                    isProcessing = false;
                    return;
                }
            } finally {
                lock.unlock();
            }

            simpleSnapshot.setSnapshot(apiRequest.getEnvoy_id(), apiRequest.buildSnapshot());
        }
    }
}
