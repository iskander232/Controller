package controller;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Message;
import controller.api.dto.ApiResponse;
import controller.api.dto.EnvoyId;
import io.envoyproxy.controlplane.cache.*;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.envoyproxy.controlplane.cache.Resources.RESOURCE_TYPES_IN_ORDER;

@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SimpleSnapshot implements ConfigWatcher {

    NodeGroup<String> groups;

    ReadWriteLock lock = new ReentrantReadWriteLock();
    Lock readLock = lock.readLock();

    AtomicLong watchCount = new AtomicLong();

    ServiceDiscoveryClient serviceDiscoveryClient;
    ConcurrentMap<String, ConcurrentMap<Resources.ResourceType, CacheStatusInfo<String>>> statuses = new ConcurrentHashMap<>();

    public SimpleSnapshot(ServiceDiscoveryClient serviceDiscoveryClient) {
        this.serviceDiscoveryClient = serviceDiscoveryClient;
        this.groups = new NodeGroup<>() {
            @Override
            public String hash(io.envoyproxy.envoy.api.v2.core.Node node) {
                throw new IllegalArgumentException("Only v3 APi supported");
            }

            @Override
            public String hash(io.envoyproxy.envoy.config.core.v3.Node node) {
                return Utils.hash(node);
            }
        };
    }

    public void setSnapshot(EnvoyId envoyId, Snapshot snapshot) {
        ConcurrentMap<Resources.ResourceType, CacheStatusInfo<String>> status = statuses.get(Utils.hash(envoyId));;

        if (status == null) {
            return;
        }

        // Responses should be in specific order and typeUrls has a list of resources in the right
        // order.
        respondWithSpecificOrder(envoyId, snapshot, status);
    }

    protected void respondWithSpecificOrder(EnvoyId envoyId,
                                            Snapshot snapshot,
                                            ConcurrentMap<Resources.ResourceType, CacheStatusInfo<String>> statusMap)
    {
        for (Resources.ResourceType resourceType : RESOURCE_TYPES_IN_ORDER) {
            CacheStatusInfo<String> status = statusMap.get(resourceType);
            if (status == null) {
                continue;
            }

            status.watchesRemoveIf((id, watch) -> {
                if (!watch.request().getResourceType().equals(resourceType)) {
                    return false;
                }
                String version = snapshot.version(watch.request().getResourceType(),
                    watch.request().getResourceNamesList());

                if (!watch.request().getVersionInfo().equals(version)) {
                    if (log.isDebugEnabled()) {
                        log.debug("responding to open watch {}[{}] with new version {}",
                            id,
                            String.join(", ", watch.request().getResourceNamesList()),
                            version);
                    }

                    ApiResponse response = respond(watch, snapshot, envoyId);
                    serviceDiscoveryClient.updateStatus(response);

                    // Discard the watch. A new watch will be created for future snapshots once envoy ACKs the response.
                    return true;
                }

                // Do not discard the watch. The request version is the same as the snapshot version, so we wait to respond.
                return false;
            });
        }
    }

    @Override
    public Watch createWatch(
        boolean ads,
        XdsRequest request,
        Set<String> knownResourceNames,
        Consumer<Response> responseConsumer,
        boolean hasClusterChanged)
    {
        Resources.ResourceType requestResourceType = request.getResourceType();
        Preconditions.checkNotNull(requestResourceType, "unsupported type URL %s",
            request.getTypeUrl());
        String group = groups.hash(request.v3Request().getNode());

        // even though we're modifying, we take a readLock to allow multiple watches to be created in parallel since it
        // doesn't conflict
        readLock.lock();
        try {
            CacheStatusInfo<String> status = statuses.computeIfAbsent(group, g -> new ConcurrentHashMap<>())
                .computeIfAbsent(requestResourceType, s -> new CacheStatusInfo<>(group));
            status.setLastWatchRequestTime(System.currentTimeMillis());

            Snapshot snapshot;
            if (request.hasErrorDetail()) {
                serviceDiscoveryClient.updateStatus(ApiResponse.builder()
                .version(request.getVersionInfo())
                .error(request.v3Request().getErrorDetail().getMessage())
                    .envoy_id(EnvoyId.builder()
                        .cluster_id(request.v3Request().getNode().getCluster())
                        .node_id(request.v3Request().getNode().getId())
                        .build())
                .resources(request.v3Request().getResourceNamesList())
                .build());
                snapshot = null;
            } else {
                snapshot = serviceDiscoveryClient.getSnapshot(request.v3Request().getNode());
            }

            String version = snapshot == null ? "" : snapshot.version(requestResourceType,
                request.getResourceNamesList());

            Watch watch = new Watch(ads, request, responseConsumer);

            if (snapshot != null) {
                Set<String> requestedResources = ImmutableSet.copyOf(request.getResourceNamesList());

                // If the request is asking for resources we haven't sent to the proxy yet, see if we have additional resources.
                if (!knownResourceNames.equals(requestedResources)) {
                    Sets.SetView<String> newResourceHints = Sets.difference(requestedResources, knownResourceNames);

                    // If any of the newly requested resources are in the snapshot respond immediately. If not we'll fall back to
                    // version comparisons.
                    if (snapshot.resources(requestResourceType)
                        .keySet()
                        .stream()
                        .anyMatch(newResourceHints::contains)
                    ) {
                        respond(watch, snapshot, Utils.nodeToId(request.v3Request().getNode()));

                        return watch;
                    }
                } else if (hasClusterChanged
                        && (requestResourceType.equals(Resources.ResourceType.ENDPOINT))) {
                    respond(watch, snapshot, Utils.nodeToId(request.v3Request().getNode()));

                    return watch;
                }
            }

            // If the requested version is up-to-date or missing a response, leave an open watch.
            if (snapshot == null || request.getVersionInfo().equals(version)) {
                long watchId = watchCount.incrementAndGet();

                if (log.isDebugEnabled()) {
                    log.debug("open watch {} for {}[{}] from node {} for version {}",
                        watchId,
                        request.getTypeUrl(),
                        String.join(", ", request.getResourceNamesList()),
                        group,
                        request.getVersionInfo());
                }

                status.setWatch(watchId, watch);

                watch.setStop(() -> status.removeWatch(watchId));

                return watch;
            }

            // Otherwise, the watch may be responded immediately
            ApiResponse response = respond(watch, snapshot, Utils.nodeToId(request.v3Request().getNode()));
            serviceDiscoveryClient.updateStatus(response);

            if (response.getVersion() == null) {
                long watchId = watchCount.incrementAndGet();

                if (log.isDebugEnabled()) {
                    log.debug("did not respond immediately, leaving open watch {} for {}[{}] from node {} for version {}",
                        watchId,
                        request.getTypeUrl(),
                        String.join(", ", request.getResourceNamesList()),
                        group,
                        request.getVersionInfo());
                }

                status.setWatch(watchId, watch);

                watch.setStop(() -> status.removeWatch(watchId));
            }

            return watch;
        } finally {
            readLock.unlock();
        }
    }

    @Nonnull
    private ApiResponse respond(Watch watch, Snapshot snapshot, EnvoyId envoyId) {
        Map<String, ? extends Message> snapshotResources = snapshot.resources(watch.request().getResourceType());

        if (!watch.request().getResourceNamesList().isEmpty() && watch.ads()) {
            Collection<String> missingNames = watch.request().getResourceNamesList().stream()
                .filter(name -> !snapshotResources.containsKey(name))
                .collect(Collectors.toList());

            if (!missingNames.isEmpty()) {
                log.info(
                    "not responding in ADS mode for {} from node {} at version {} for request [{}] since [{}] not in snapshot",
                    watch.request().getTypeUrl(),
                    Utils.hash(envoyId),
                    snapshot.version(watch.request().getResourceType(), watch.request().getResourceNamesList()),
                    String.join(", ", watch.request().getResourceNamesList()),
                    String.join(", ", missingNames));

                return ApiResponse.builder()
                    .envoy_id(envoyId)
                    .build();
            }
        }

        String version = snapshot.version(watch.request().getResourceType(),
            watch.request().getResourceNamesList());

        log.debug("responding for {} from node {} at version {} with version {}",
            watch.request().getTypeUrl(),
            envoyId,
            watch.request().getVersionInfo(),
            version);

        Response response = createResponse(
            watch.request(),
            snapshotResources,
            version);

        try {
            watch.respond(response);
            return ApiResponse.builder()
                .envoy_id(envoyId)
                .version(response.version())
                .resources(response.resources().stream().map(Message::toString).collect(Collectors.toList()))
                .build();
        } catch (WatchCancelledException e) {
            log.error(
                "failed to respond for {} from node {} at version {} with version {} because watch was already cancelled",
                watch.request().getTypeUrl(),
                envoyId,
                watch.request().getVersionInfo(),
                version);
        }

        return ApiResponse.builder()
            .envoy_id(envoyId)
            .build();
    }

    private Response createResponse(XdsRequest request, Map<String, ? extends Message> resources, String version) {
        Collection<? extends Message> filtered = request.getResourceNamesList().isEmpty()
            ? resources.values()
            : request.getResourceNamesList().stream()
            .map(resources::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return Response.create(request, filtered, version);
    }
}
