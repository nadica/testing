package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwisstopoAddressClient {
    private static final String DEFAULT_SEARCH_URL = "https://api3.geo.admin.ch/rest/services/api/SearchServer";
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("(\\d+)");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String searchUrl;

    public SwisstopoAddressClient() {
        this(HttpClient.newHttpClient(), new ObjectMapper(), DEFAULT_SEARCH_URL);
    }

    SwisstopoAddressClient(HttpClient httpClient, ObjectMapper objectMapper, String searchUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.searchUrl = searchUrl;
    }

    public List<AddressSuggestion> autocomplete(String searchText, int limit) throws IOException, InterruptedException {
        if (searchText == null || searchText.isBlank()) {
            return List.of();
        }

        URI uri = buildSearchUri(searchText, limit);
        HttpRequest request = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Swisstopo search failed with status " + response.statusCode());
        }

        return parseSuggestions(response.body());
    }

    URI buildSearchUri(String searchText, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String encodedText = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
        String query = "type=locations&origins=address&searchText=" + encodedText + "&limit=" + safeLimit;
        return URI.create(searchUrl + "?" + query);
    }

    List<AddressSuggestion> parseSuggestions(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            return List.of();
        }

        List<AddressSuggestion> suggestions = new ArrayList<>();
        for (JsonNode result : results) {
            JsonNode attrs = result.path("attrs");
            String label = attrs.path("label").asText("");
            String detail = attrs.path("detail").asText("");
            String egaid = extractEgaid(attrs);
            Double east = getNullableDouble(attrs, "x");
            Double north = getNullableDouble(attrs, "y");
            Double lon = getNullableDouble(attrs, "lon");
            Double lat = getNullableDouble(attrs, "lat");

            suggestions.add(new AddressSuggestion(label, detail, egaid, east, north, lon, lat));
        }
        return suggestions;
    }

    private String extractEgaid(JsonNode attrs) {
        JsonNode links = attrs.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                String title = link.path("title").asText("");
                String href = link.path("href").asText("");
                if ("ch.swisstopo.amtliches-gebaeudeadressverzeichnis".equals(title)) {
                    String idFromHref = lastPathSegment(href);
                    if (idFromHref != null) {
                        return idFromHref;
                    }
                }
            }
        }

        String featureId = attrs.path("featureId").asText("");
        Matcher matcher = NUMERIC_ID_PATTERN.matcher(featureId);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String lastPathSegment(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        int index = href.lastIndexOf('/');
        if (index < 0 || index == href.length() - 1) {
            return null;
        }
        String segment = href.substring(index + 1);
        Matcher matcher = NUMERIC_ID_PATTERN.matcher(segment);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Double getNullableDouble(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }
}
