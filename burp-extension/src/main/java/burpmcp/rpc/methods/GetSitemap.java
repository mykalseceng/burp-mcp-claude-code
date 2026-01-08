package burpmcp.rpc.methods;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.sitemap.SiteMapFilter;
import burp.api.montoya.http.message.HttpRequestResponse;
import burpmcp.rpc.*;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

public class GetSitemap implements RpcMethod {
    private final MontoyaApi api;

    public GetSitemap(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public String getName() {
        return "get_sitemap";
    }

    @Override
    public Object execute(JsonObject params) throws RpcException {
        String domain = params.has("domain") ? params.get("domain").getAsString() : null;
        boolean includeParams = !params.has("includeParams") || params.get("includeParams").getAsBoolean();

        List<HttpRequestResponse> items;
        if (domain != null && !domain.isEmpty()) {
            items = new ArrayList<>(api.siteMap().requestResponses(
                SiteMapFilter.prefixFilter("https://" + domain)
            ));
            items.addAll(api.siteMap().requestResponses(
                SiteMapFilter.prefixFilter("http://" + domain)
            ));
        } else {
            items = api.siteMap().requestResponses();
        }

        List<Map<String, Object>> entries = items.stream()
            .map(item -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("url", item.request().url());
                entry.put("method", item.request().method());
                entry.put("statusCode", item.response() != null ? item.response().statusCode() : 0);
                entry.put("mimeType", item.response() != null ? item.response().mimeType().toString() : "");
                if (includeParams) {
                    entry.put("parameters", item.request().parameters().stream()
                        .map(p -> p.name())
                        .collect(Collectors.toList()));
                }
                return entry;
            })
            .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("entries", entries);
        result.put("count", entries.size());

        return result;
    }
}
