package com.back.atlas.client.places;

import com.back.atlas.config.PlacesConfig;
import com.back.atlas.config.PlacesProperties;
import com.back.atlas.dto.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Against a mocked HTTP server, with answers in the real Overpass and Nominatim formats. */
class OsmActivityProviderTest {

    // Trimmed Overpass answer around the Colosseum: a node, a way (with "center"), a duplicate
    // name, an element without a name, and a paid place without a Wikipedia entry.
    private static final String OVERPASS = """
            {"version": 0.6, "generator": "Overpass API", "elements": [
              {"type": "node", "id": 1, "lat": 41.8925, "lon": 12.4853,
               "tags": {"tourism": "attraction", "name": "Small fountain"}},
              {"type": "way", "id": 2, "center": {"lat": 41.8902, "lon": 12.4922},
               "tags": {"tourism": "attraction", "historic": "archaeological_site", "name": "Colosseo",
                        "wikipedia": "it:Colosseo", "wikidata": "Q10285", "fee": "yes",
                        "wikimedia_commons": "File:Colosseo 2020.jpg", "opening_hours": "09:00-19:00",
                        "website": "https://parcocolosseo.it"}},
              {"type": "node", "id": 3, "lat": 41.8955, "lon": 12.4823,
               "tags": {"historic": "monument", "name": "Altare della Patria", "wikidata": "Q190208",
                        "wikipedia": "it:Vittoriano", "fee": "no"}},
              {"type": "node", "id": 4, "lat": 41.8930, "lon": 12.4850, "tags": {"tourism": "viewpoint"}},
              {"type": "node", "id": 5, "lat": 41.9000, "lon": 12.5000,
               "tags": {"tourism": "attraction", "name": "colosseo"}},
              {"type": "node", "id": 6, "lat": 41.8910, "lon": 12.4870,
               "tags": {"tourism": "museum", "name": "Museo del Foro", "fee": "yes"}}
            ]}""";

    private MockRestServiceServer server;
    private OsmActivityProvider provider;

    @BeforeEach
    void setUp() {
        PlacesProperties properties = new PlacesProperties(
                "https://overpass.test", "https://nominatim.test", "Atlas-Test", Duration.ofSeconds(5));
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OsmActivityProvider(PlacesConfig.configure(builder, properties).build(), properties);
    }

    @Test
    void nearbyPlacesAreRankedNotableFirstThenByDistance() {
        server.expect(once(), requestTo(containsString("https://overpass.test/api/interpreter?data=")))
                .andExpect(header("User-Agent", "Atlas-Test"))
                .andExpect(decodedQuery("around:1000,41.890200,12.492200"))
                .andRespond(withSuccess(OVERPASS, MediaType.APPLICATION_JSON));

        List<Place> places = provider.nearby(41.8902, 12.4922, 1000, 10);

        server.verify();
        assertThat(places).extracting(Place::getName)
                // Wikipedia entries first (closest first), then the others by distance;
                // the unnamed viewpoint and the duplicate "colosseo" are dropped.
                .containsExactly("Colosseo", "Altare della Patria", "Museo del Foro", "Small fountain");

        Place colosseo = places.getFirst();
        assertThat(colosseo.getId()).isEqualTo("way/2");
        assertThat(colosseo.getCategory()).isEqualTo("Attraction");
        assertThat(colosseo.getDistanceMeters()).isZero();
        assertThat(colosseo.getFree()).isFalse();
        assertThat(colosseo.getWikipediaUrl()).isEqualTo("https://it.wikipedia.org/wiki/Colosseo");
        assertThat(colosseo.getImageUrl())
                .isEqualTo("https://commons.wikimedia.org/wiki/Special:FilePath/Colosseo_2020.jpg?width=640");
        assertThat(colosseo.getOpeningHours()).isEqualTo("09:00-19:00");
        assertThat(colosseo.getMapsUrl()).contains("query=41.890200%2C12.492200");
        assertThat(places.get(1).getFree()).isTrue();
        assertThat(places.get(1).getDistanceMeters()).isBetween(1000, 1200);
    }

    @Test
    void sameAreaIsServedFromTheCache() {
        server.expect(once(), requestTo(containsString("/api/interpreter")))
                .andRespond(withSuccess(OVERPASS, MediaType.APPLICATION_JSON));

        provider.nearby(41.89020, 12.49220, 1000, 10);
        List<Place> again = provider.nearby(41.89024, 12.49218, 1000, 2); // a few metres away

        server.verify(); // one HTTP call only
        assertThat(again).hasSize(2);
    }

    @Test
    void cityIsGeocodedThenOnlyNotablePlacesAreRequested() {
        server.expect(once(), requestTo(containsString("https://nominatim.test/search?q=Rome")))
                .andExpect(decodedQuery("featureType=settlement"))
                .andRespond(withSuccess("""
                        [{"place_id": 1, "lat": "41.8933203", "lon": "12.4829321", "display_name": "Roma, Lazio, Italia"}]""",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(containsString("/api/interpreter")))
                .andExpect(decodedQuery("around:6000,41.893320,12.482932"))
                .andExpect(decodedQuery("[wikidata]"))
                .andRespond(withSuccess(OVERPASS, MediaType.APPLICATION_JSON));

        List<Place> places = provider.inCity("Rome", 2);

        server.verify();
        assertThat(places).extracting(Place::getName).containsExactly("Colosseo", "Altare della Patria");
    }

    @Test
    void unknownCityGivesNoPlaces() {
        server.expect(once(), requestTo(containsString("nominatim.test")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(provider.inCity("Atlantis", 5)).isEmpty();
        server.verify();
    }

    @Test
    void providerErrorsBecomeProviderException() {
        server.expect(requestTo(containsString("/api/interpreter"))).andRespond(withServerError());

        assertThatThrownBy(() -> provider.nearby(41.9, 12.5, 1000, 10))
                .isInstanceOf(ActivityProvider.ProviderException.class);
    }

    /** Matches text in the query string once URL-decoded. */
    private static RequestMatcher decodedQuery(String expected) {
        return request -> assertThat(URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8))
                .contains(expected);
    }

    @Test
    void wikipediaTagsBecomeLinks() {
        assertThat(OsmActivityProvider.wikipediaUrl("en:St. Peter's Basilica"))
                .isEqualTo("https://en.wikipedia.org/wiki/St._Peter%27s_Basilica");
        assertThat(OsmActivityProvider.wikipediaUrl("Colosseo")).isNull();
        assertThat(OsmActivityProvider.wikipediaUrl(null)).isNull();
    }
}
