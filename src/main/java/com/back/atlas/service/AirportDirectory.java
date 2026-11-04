package com.back.atlas.service;

import com.back.atlas.dto.Airport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Airport search for the flight form, from a static file: the ~3,200 airports with
 * scheduled flights and an IATA code, from OurAirports (public domain, ourairports.com).
 * Airports do not change often, so a file loaded in memory beats a paid API:
 * instant answers, no key, no network.
 */
@Service
public class AirportDirectory {

    /** Italian city names that the English dataset does not contain (keys normalized below). */
    private static final Map<String, String> CITY_ALIASES = normalizeKeys(Map.ofEntries(
            Map.entry("roma", "rome"), Map.entry("milano", "milan"), Map.entry("venezia", "venice"),
            Map.entry("firenze", "florence"), Map.entry("napoli", "naples"), Map.entry("torino", "turin"),
            Map.entry("genova", "genoa"), Map.entry("parigi", "paris"), Map.entry("londra", "london"),
            Map.entry("barcellona", "barcelona"), Map.entry("siviglia", "seville"), Map.entry("lisbona", "lisbon"),
            Map.entry("berlino", "berlin"), Map.entry("monaco di baviera", "munich"),
            Map.entry("praga", "prague"), Map.entry("atene", "athens"), Map.entry("bruxelles", "brussels"),
            Map.entry("ginevra", "geneva"), Map.entry("zurigo", "zurich"), Map.entry("varsavia", "warsaw"),
            Map.entry("mosca", "moscow"), Map.entry("copenaghen", "copenhagen"), Map.entry("edimburgo", "edinburgh"),
            Map.entry("dublino", "dublin"), Map.entry("il cairo", "cairo"), Map.entry("pechino", "beijing"),
            Map.entry("tokio", "tokyo"), Map.entry("nuova york", "new york"), Map.entry("città del messico", "mexico city")));

    private final List<Entry> airports;

    public AirportDirectory() {
        this.airports = load("data/airports.csv");
    }

    /** Best matches first: IATA code, then city, then airport name. Big airports win ties. */
    public List<Airport> search(String keyword, int limit) {
        String query = normalize(keyword);
        if (query.length() < 2) {
            return List.of();
        }
        String alias = CITY_ALIASES.get(query);
        return airports.stream()
                .map(entry -> new Scored(entry, Math.max(entry.score(query), alias == null ? 0 : entry.score(alias))))
                .filter(scored -> scored.score > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing(scored -> !scored.entry.large)
                        .thenComparing(scored -> scored.entry.airport.getIataCode()))
                .limit(limit)
                .map(scored -> scored.entry.airport)
                .toList();
    }

    public int size() {
        return airports.size();
    }

    private record Scored(Entry entry, int score) {
    }

    private record Entry(Airport airport, boolean large, String iata, String city, String name, String keywords) {

        int score(String query) {
            if (iata.equals(query)) return 100;
            if (city.equals(query)) return 80;
            if (city.startsWith(query)) return 60;
            if (name.startsWith(query)) return 50;
            if (containsWord(city, query) || containsWord(name, query)) return 40;
            if (containsWord(keywords, query)) return 30;
            if (query.length() >= 4 && (city.contains(query) || name.contains(query))) return 10;
            return 0;
        }

        private static boolean containsWord(String text, String query) {
            return text.startsWith(query) || text.contains(" " + query);
        }
    }

    private static Map<String, String> normalizeKeys(Map<String, String> aliases) {
        Map<String, String> normalized = new java.util.HashMap<>();
        aliases.forEach((key, value) -> normalized.put(normalize(key), value));
        return Map.copyOf(normalized);
    }

    static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static List<Entry> load(String path) {
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine(); // header
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> f = parseCsvLine(line);
                // iata,name,city,country,country_code,size,latitude,longitude,keywords
                Airport airport = new Airport(f.get(0), f.get(1), f.get(2), f.get(3), f.get(4),
                        Double.parseDouble(f.get(6)), Double.parseDouble(f.get(7)));
                entries.add(new Entry(airport, "large".equals(f.get(5)), normalize(f.get(0)),
                        normalize(f.get(2)), normalize(f.get(1)), normalize(f.get(8))));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + path, e);
        }
        return List.copyOf(entries);
    }

    /** RFC 4180 fields: commas inside double quotes, "" for a literal quote. */
    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields;
    }
}
