package controller;

import controller.api.dto.EnvoyConfig;
import controller.api.dto.AddConfigsRequest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class AddConfigRequestsQueue {

    ReentrantLock lock = new ReentrantLock();
    Deque<EnvoyConfig> snapshotDeque = new ArrayDeque<>();
    ConfigWatcherImpl configWatcherImpl;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    boolean isProcessing = false;

    AddConfigRequestsQueue(ConfigWatcherImpl configWatcherImpl) {
        this.configWatcherImpl = configWatcherImpl;
    }

    public void addApiRequest(AddConfigsRequest apiRequest) {
        lock.lock();
        try {
            for (EnvoyConfig p: apiRequest.getConfigs()) {
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
        EnvoyConfig apiRequest;
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

            configWatcherImpl.setSnapshot(apiRequest.getEnvoy_id(), apiRequest.buildSnapshot());
        }
    }
}
