package com.zidio.keystone;

import com.zidio.keystone.dto.CreateTechnicianRequest;
import com.zidio.keystone.dto.LoginRequest;
import com.zidio.keystone.dto.LoginResponse;
import com.zidio.keystone.dto.MapPinDto;
import com.zidio.keystone.dto.TechnicianDto;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end verification of the manager-provisioned technician roster,
 * nearest-technician dispatch suggestion, and the manager/customer tracking
 * map — driven over real HTTP against a real (embedded) Postgres, the same
 * way a browser client would call the deployed API.
 *
 * provider = ZONKY forces the native (no-Docker) embedded-postgres binary
 * instead of the default Testcontainers/Docker-backed provider — this
 * machine has no working Docker engine.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(type = POSTGRES, provider = ZONKY)
class MapAndTechnicianIntegrationTest {

    @LocalServerPort
    int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    private String loginAs(String email) {
        LoginResponse resp = rest.postForObject(baseUrl() + "/auth/login", new LoginRequest(email, "Password123!"), LoginResponse.class);
        assertNotNull(resp, "login should succeed for seeded user " + email);
        return resp.token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void managerCanCreateTechnicianWithGeocodedBase() {
        String token = loginAs("manager@keystone.dev");
        CreateTechnicianRequest req = new CreateTechnicianRequest(
            "Test Tech", "test.tech." + System.currentTimeMillis() + "@keystone.dev", "Password123!",
            "College Road, Nashik, MH"
        );

        ResponseEntity<TechnicianDto> resp = rest.exchange(
            baseUrl() + "/users/technicians", HttpMethod.POST, new HttpEntity<>(req, authHeaders(token)), TechnicianDto.class
        );

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        TechnicianDto tech = resp.getBody();
        assertNotNull(tech);
        assertNotNull(tech.baseLatitude(), "expected geocoding to resolve a real address");
        assertNotNull(tech.baseLongitude());
    }

    @Test
    void nearestTechniciansSortsSeededTechnicianByDistance() {
        String token = loginAs("dispatcher@keystone.dev");
        // WO-1002 is unassigned, sited at Meridian Warehouse 3 (MIDC Pune).
        ResponseEntity<TechnicianDto[]> resp = rest.exchange(
            baseUrl() + "/work-orders/c2222222-cccc-2222-cccc-222222222222/nearest-technicians",
            HttpMethod.GET, new HttpEntity<>(authHeaders(token)), TechnicianDto[].class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<TechnicianDto> techs = List.of(resp.getBody());
        assertFalse(techs.isEmpty());

        TechnicianDto rahul = techs.stream().filter(t -> t.email().equals("technician@keystone.dev")).findFirst().orElseThrow();
        assertNotNull(rahul.distanceKm(), "seeded technician has a base location — expected a real distance, not null");
        assertTrue(rahul.distanceKm() < 50, "Pune MIDC to Pune Shivaji Nagar should be well under 50km, was " + rahul.distanceKm());
    }

    @Test
    void managerMapOverviewIncludesTechnicianAndOpenWorkOrderPins() {
        String token = loginAs("manager@keystone.dev");
        ResponseEntity<MapPinDto[]> resp = rest.exchange(
            baseUrl() + "/map/overview", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), MapPinDto[].class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<MapPinDto> pins = List.of(resp.getBody());
        assertTrue(pins.stream().anyMatch(p -> p.kind().equals("TECHNICIAN")), "expected the seeded technician's base pin");
        assertTrue(pins.stream().anyMatch(p -> p.kind().equals("WORK_ORDER")), "expected at least one open work order pin");
    }

    @Test
    void customerMapOverviewShowsOwnSitesAndAssignedTechnician() {
        String token = loginAs("customer@keystone.dev");
        ResponseEntity<MapPinDto[]> resp = rest.exchange(
            baseUrl() + "/map/my-requests", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), MapPinDto[].class
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<MapPinDto> pins = List.of(resp.getBody());
        assertTrue(pins.stream().anyMatch(p -> p.kind().equals("SITE")), "expected the customer's own sites");
        assertTrue(pins.stream().anyMatch(p -> p.kind().equals("TECHNICIAN")), "WO-1001 is assigned to Rahul, who has a base location");
    }

    @Test
    void technicianCannotCreateTechniciansOrSeeManagerMap() {
        String token = loginAs("technician@keystone.dev");

        ResponseEntity<String> createResp = rest.exchange(
            baseUrl() + "/users/technicians", HttpMethod.POST,
            new HttpEntity<>(new CreateTechnicianRequest("X", "x@keystone.dev", "Password123!", null), authHeaders(token)),
            String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, createResp.getStatusCode());

        ResponseEntity<String> mapResp = rest.exchange(
            baseUrl() + "/map/overview", HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, mapResp.getStatusCode());
    }
}
