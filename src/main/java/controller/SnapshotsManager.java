package controller;

import io.envoyproxy.controlplane.cache.v3.Snapshot;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class SnapshotsManager {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    private final Map<String, Snapshot> snapshots = new HashMap<>();

    public void remove(String group) {
        snapshots.remove(group);
    }

    public Snapshot get(String group) {
        log.info(group);
        return snapshots.get(group);
    }

    public void set(String group, Snapshot snapshot) {
        snapshots.put(group, snapshot);
    }
}
