package burpmcp.replay;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burpmcp.traffic.StoredRequest;
import burpmcp.util.JsonUtils;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReplayPackService {
    public Map<String, Object> exportPack(
        Path outputPath,
        Map<String, Object> metadata,
        List<StoredRequest> requests,
        List<Map<String, Object>> findings,
        List<String> scopeUrls
    ) throws IOException {
        Map<String, Object> pack = buildPack(metadata, requests, findings, scopeUrls);
        String canonicalJson = canonicalJson(pack);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, canonicalJson, StandardCharsets.UTF_8);

        Map<String, Object> result = new HashMap<>();
        result.put("path", outputPath.toString());
        result.put("sha256", sha256Hex(canonicalJson));
        result.put("requests", ((List<?>) pack.get("requests")).size());
        result.put("findings", ((List<?>) pack.get("findings")).size());
        return result;
    }

    public Map<String, Object> runPack(Path inputPath, MontoyaApi api) throws IOException {
        String json = Files.readString(inputPath, StandardCharsets.UTF_8);
        Map<String, Object> pack = JsonUtils.getGson().fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());

        List<Map<String, Object>> requests = safeListOfMap(pack.get("requests"));
        List<Map<String, Object>> replayResults = new ArrayList<>();

        int index = 0;
        for (Map<String, Object> requestEntry : requests) {
            index++;
            String url = asString(requestEntry.get("url"));
            String method = asString(requestEntry.get("method"));
            String body = asString(requestEntry.get("requestBody"));

            HttpRequest req = HttpRequest.httpRequestFromUrl(url).withMethod(method);
            if (body != null && !body.isEmpty()) {
                req = req.withBody(body);
            }

            var reqResp = api.http().sendRequest(req);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("index", index);
            result.put("url", url);
            result.put("statusCode", reqResp.response() == null ? null : reqResp.response().statusCode());
            result.put("responseBodySha256", reqResp.response() == null ? null : sha256Hex(reqResp.response().bodyToString()));
            replayResults.add(result);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("executedAt", Instant.now().toEpochMilli());
        out.put("packPath", inputPath.toString());
        out.put("requestCount", replayResults.size());
        out.put("results", replayResults);
        return out;
    }

    public Map<String, Object> buildPack(
        Map<String, Object> metadata,
        List<StoredRequest> requests,
        List<Map<String, Object>> findings,
        List<String> scopeUrls
    ) {
        List<Map<String, Object>> normalizedRequests = new ArrayList<>();
        requests.stream()
            .sorted(Comparator.comparingLong(StoredRequest::getTimestamp).thenComparingLong(StoredRequest::getId))
            .forEach(r -> {
                Map<String, Object> req = new LinkedHashMap<>();
                req.put("id", r.getId());
                req.put("timestamp", r.getTimestamp());
                req.put("method", r.getMethod());
                req.put("url", r.getUrl());
                req.put("host", r.getHost());
                req.put("port", r.getPort());
                req.put("https", r.isHttps());
                req.put("requestHeaders", new TreeMap<>(r.getRequestHeaders()));
                req.put("requestBody", r.getRequestBody());
                req.put("statusCode", r.getStatusCode());
                req.put("responseHeaders", new TreeMap<>(r.getResponseHeaders()));
                req.put("responseBody", r.getResponseBody());
                req.put("mimeType", r.getMimeType());
                req.put("toolSource", r.getToolSource());
                normalizedRequests.add(req);
            });

        List<Map<String, Object>> normalizedFindings = new ArrayList<>(findings);
        normalizedFindings.sort(Comparator.comparing(f -> asString(f.get("name")) + "|" + asString(f.get("baseUrl"))));

        List<String> normalizedScope = new ArrayList<>(scopeUrls);
        normalizedScope.sort(String::compareTo);

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("replayPackVersion", "1.0");
        Object generatedAt = metadata != null && metadata.containsKey("generatedAt") ? metadata.get("generatedAt") : 0;
        manifest.put("generatedAt", generatedAt);

        Map<String, Object> pack = new LinkedHashMap<>();
        pack.put("manifest", manifest);
        pack.put("metadata", metadata == null ? Map.of() : new TreeMap<>(metadata));
        pack.put("scope", normalizedScope);
        pack.put("requests", normalizedRequests);
        pack.put("findings", normalizedFindings);
        return pack;
    }

    private String canonicalJson(Object input) {
        Object normalized = normalize(input);
        return JsonUtils.getGson().toJson(normalized);
    }

    private Object normalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> out = new TreeMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), normalize(e.getValue()));
            }
            return out;
        }
        if (value instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object item : list) {
                out.add(normalize(item));
            }
            return out;
        }
        return value;
    }

    private List<Map<String, Object>> safeListOfMap(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> typed = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    typed.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                out.add(typed);
            }
        }
        return out;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
