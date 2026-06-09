package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SwisstopoAddressClientTest {

    @Test
    void buildSearchUriShouldUseAddressAutocompleteParameters() {
        SwisstopoAddressClient client = new SwisstopoAddressClient(HttpClient.newHttpClient(), new ObjectMapper(), "https://api3.geo.admin.ch/rest/services/api/SearchServer");

        URI uri = client.buildSearchUri("Bern Bahnhofstrasse 1", 8);

        assertEquals(
            "https://api3.geo.admin.ch/rest/services/api/SearchServer?type=locations&origins=address&searchText=Bern+Bahnhofstrasse+1&limit=8",
            uri.toString()
        );
    }

    @Test
    void parseSuggestionsShouldExtractEgaidFromAddressDirectoryLink() throws IOException {
        SwisstopoAddressClient client = new SwisstopoAddressClient(HttpClient.newHttpClient(), new ObjectMapper(), "https://api3.geo.admin.ch/rest/services/api/SearchServer");

        List<AddressSuggestion> suggestions = client.parseSuggestions("""
            {
              "results": [
                {
                  "attrs": {
                    "label": "Bahnhofstrasse 2a <b>3073 G\\u00fcmligen</b>",
                    "detail": "bahnhofstrasse 2a 3073 guemligen",
                    "featureId": "190770830_0",
                    "x": 198114.796875,
                    "y": 605166.6875,
                    "lon": 7.50648307800293,
                    "lat": 46.934104919433594,
                    "links": [
                      {
                        "href": "/rest/services/ech/MapServer/ch.swisstopo.amtliches-gebaeudeadressverzeichnis/101820702",
                        "title": "ch.swisstopo.amtliches-gebaeudeadressverzeichnis"
                      },
                      {
                        "href": "/rest/services/ech/MapServer/ch.bfs.gebaeude_wohnungs_register/190770830_0",
                        "title": "ch.bfs.gebaeude_wohnungs_register"
                      }
                    ]
                  }
                }
              ]
            }
            """);

        assertEquals(1, suggestions.size());
        AddressSuggestion first = suggestions.get(0);
        assertEquals("101820702", first.egaid());
        assertEquals("bahnhofstrasse 2a 3073 guemligen", first.detail());
        assertNotNull(first.lat());
        assertNotNull(first.lon());
    }

    @Test
    void parseSuggestionsShouldFallbackToFeatureIdWhenDirectoryLinkMissing() throws IOException {
        SwisstopoAddressClient client = new SwisstopoAddressClient(HttpClient.newHttpClient(), new ObjectMapper(), "https://api3.geo.admin.ch/rest/services/api/SearchServer");

        List<AddressSuggestion> suggestions = client.parseSuggestions("""
            {
              "results": [
                {
                  "attrs": {
                    "label": "Example",
                    "featureId": "502112933_0"
                  }
                }
              ]
            }
            """);

        assertEquals(1, suggestions.size());
        assertEquals("502112933", suggestions.get(0).egaid());
    }
}
