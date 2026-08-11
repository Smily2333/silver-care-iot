package com.silvercare.iot.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.silvercare.iot.config.GeocodingProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class NominatimClient {

    private final RestClient restClient;
    private final GeocodingProperties properties;

    public NominatimClient(GeocodingProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .build();
    }

    NominatimClient(GeocodingProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public Optional<AddressResult> reverse(BigDecimal latitude, BigDecimal longitude) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/reverse")
                        .queryParam("lat", latitude.toPlainString())
                        .queryParam("lon", longitude.toPlainString())
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .queryParam("accept-language", "zh-CN,zh")
                        .build())
                .retrieve()
                .body(JsonNode.class);
        if (response == null || response.has("error")) {
            return Optional.empty();
        }
        JsonNode address = response.path("address");
        return Optional.of(new AddressResult(
                text(response, "display_name"),
                firstText(address, "road", "pedestrian", "residential", "footway"),
                firstText(address, "neighbourhood", "quarter", "suburb", "town"),
                firstText(address, "city_district", "district", "county"),
                firstText(address, "city", "municipality", "state")
        ));
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    public record AddressResult(String displayName, String road, String neighbourhood,
                                String district, String city) {
    }
}
