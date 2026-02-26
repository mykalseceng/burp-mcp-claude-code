package burpmcp.replay;

import burpmcp.traffic.StoredRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReplayPackServiceTest {

    @Test
    void exportProducesDeterministicHashForSameData() throws Exception {
        ReplayPackService service = new ReplayPackService();

        StoredRequest requestA = new StoredRequest.Builder()
            .id(2)
            .timestamp(200)
            .method("POST")
            .url("https://example.com/b")
            .host("example.com")
            .port(443)
            .isHttps(true)
            .requestBody("b")
            .statusCode(200)
            .responseBody("ok")
            .build();

        StoredRequest requestB = new StoredRequest.Builder()
            .id(1)
            .timestamp(100)
            .method("GET")
            .url("https://example.com/a")
            .host("example.com")
            .port(443)
            .isHttps(true)
            .requestBody("")
            .statusCode(404)
            .responseBody("no")
            .build();

        Map<String, Object> metadata = Map.of(
            "domain", "example.com",
            "generatedAt", 0
        );

        List<Map<String, Object>> findings = List.of(
            Map.of("name", "Issue B", "baseUrl", "https://example.com/b"),
            Map.of("name", "Issue A", "baseUrl", "https://example.com/a")
        );

        Path out1 = Files.createTempFile("replay-1", ".json");
        Path out2 = Files.createTempFile("replay-2", ".json");
        try {
            Map<String, Object> r1 = service.exportPack(out1, metadata, List.of(requestA, requestB), findings, List.of("https://example.com"));
            Map<String, Object> r2 = service.exportPack(out2, metadata, List.of(requestA, requestB), findings, List.of("https://example.com"));

            assertEquals(r1.get("sha256"), r2.get("sha256"));
            assertEquals(Files.readString(out1), Files.readString(out2));
        } finally {
            Files.deleteIfExists(out1);
            Files.deleteIfExists(out2);
        }
    }
}
