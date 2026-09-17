package com.zidio.keystone.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Best-effort address -> lat/lng lookup via OpenStreetMap's free Nominatim
 * API (no key or billing needed). Never throws — a failed or ambiguous
 * lookup just leaves the site/technician without coordinates, so the map and
 * nearest-technician features degrade gracefully instead of blocking the
 * write that triggered them.
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    private final RestClient restClient = RestClient.builder()
        .baseUrl("https://nominatim.openstreetmap.org")
        // Nominatim's usage policy requires an identifying User-Agent.
        .defaultHeader("User-Agent", "KEYSTONE-FieldService/1.0 (ops@meridianfm.com)")
        .build();

    public record GeoPoint(double latitude, double longitude) {
    }

    public Optional<GeoPoint> geocode(String address) {
        if (address == null || address.isBlank()) return Optional.empty();
        try {
            List<NominatimResult> results = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                    .queryParam("q", address)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<NominatimResult>>() {
                });

            if (results == null || results.isEmpty()) {
                log.warn("[geocoding] no match for address: {}", address);
                return Optional.empty();
            }
            NominatimResult r = results.get(0);
            return Optional.of(new GeoPoint(Double.parseDouble(r.lat()), Double.parseDouble(r.lon())));
        } catch (Exception ex) {
            log.warn("[geocoding] lookup failed for address '{}': {}", address, ex.getMessage());
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NominatimResult(String lat, String lon) {
    }
}
