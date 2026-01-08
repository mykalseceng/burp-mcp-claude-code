package burpmcp.traffic;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TrafficStore {
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<StoredRequest>> requestsByDomain;
    private final AtomicLong idGenerator;
    private final int maxRequestsPerDomain;

    public TrafficStore(int maxRequestsPerDomain) {
        this.requestsByDomain = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong(0);
        this.maxRequestsPerDomain = maxRequestsPerDomain;
    }

    public long store(StoredRequest.Builder builder) {
        long id = idGenerator.incrementAndGet();
        StoredRequest request = builder.id(id).build();
        String domain = request.getHost().toLowerCase();

        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain
            .computeIfAbsent(domain, k -> new ConcurrentLinkedDeque<>());

        queue.addFirst(request);

        // Evict oldest if over limit
        while (queue.size() > maxRequestsPerDomain) {
            queue.pollLast();
        }

        return id;
    }

    public List<StoredRequest> getByDomain(String domain, int limit) {
        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain.get(domain.toLowerCase());
        if (queue == null) {
            return new ArrayList<>();
        }
        return queue.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    public List<StoredRequest> getByDomain(String domain, int limit, String method, Integer statusCode) {
        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain.get(domain.toLowerCase());
        if (queue == null) {
            return new ArrayList<>();
        }
        return queue.stream()
            .filter(r -> method == null || r.getMethod().equalsIgnoreCase(method))
            .filter(r -> statusCode == null || r.getStatusCode() == statusCode)
            .limit(limit)
            .collect(Collectors.toList());
    }

    public int getTotalForDomain(String domain) {
        ConcurrentLinkedDeque<StoredRequest> queue = requestsByDomain.get(domain.toLowerCase());
        return queue == null ? 0 : queue.size();
    }

    public void clearDomain(String domain) {
        requestsByDomain.remove(domain.toLowerCase());
    }

    public void clearAll() {
        requestsByDomain.clear();
    }
}
