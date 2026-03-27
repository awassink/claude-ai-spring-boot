package nl.awassink.integration;

import nl.awassink.application.dto.DriverRequest;
import nl.awassink.application.dto.DriverResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestSecurityConfig.class)
class DriverIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() {
        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth("test-token");
    }

    private DriverRequest sampleRequest(String licenseNumber, String email) {
        return new DriverRequest(
            "John", "Doe", licenseNumber, "+31612345678",
            email, LocalDate.of(2027, 1, 1), true
        );
    }

    @Test
    void createDriver_validRequest_returns201AndCanBeRetrieved() {
        DriverRequest request = sampleRequest("LIC-IT-001", "it001@example.com");
        HttpEntity<DriverRequest> entity = new HttpEntity<>(request, authHeaders);

        ResponseEntity<DriverResponse> response = restTemplate.postForEntity("/api/drivers", entity, DriverResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().licenseNumber()).isEqualTo("LIC-IT-001");
        assertThat(response.getBody().id()).isNotNull();

        UUID id = response.getBody().id();
        ResponseEntity<DriverResponse> getResponse = restTemplate.exchange(
            "/api/drivers/" + id, HttpMethod.GET,
            new HttpEntity<>(authHeaders), DriverResponse.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().email()).isEqualTo("it001@example.com");
    }

    @Test
    void createDriver_duplicateLicense_returns409() {
        DriverRequest request = sampleRequest("LIC-IT-DUP", "dup1@example.com");
        HttpEntity<DriverRequest> entity = new HttpEntity<>(request, authHeaders);
        restTemplate.postForEntity("/api/drivers", entity, DriverResponse.class);

        DriverRequest duplicate = sampleRequest("LIC-IT-DUP", "dup2@example.com");
        ResponseEntity<Object> response = restTemplate.postForEntity(
            "/api/drivers", new HttpEntity<>(duplicate, authHeaders), Object.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getDriver_nonExistent_returns404() {
        ResponseEntity<Object> response = restTemplate.exchange(
            "/api/drivers/" + UUID.randomUUID(), HttpMethod.GET,
            new HttpEntity<>(authHeaders), Object.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateDriver_existingDriver_returns200WithUpdatedData() {
        DriverRequest create = sampleRequest("LIC-IT-UPD", "upd@example.com");
        ResponseEntity<DriverResponse> created = restTemplate.postForEntity(
            "/api/drivers", new HttpEntity<>(create, authHeaders), DriverResponse.class
        );
        UUID id = created.getBody().id();

        DriverRequest update = new DriverRequest(
            "Jane", "Smith", "LIC-IT-UPD", "+31698765432",
            "upd@example.com", LocalDate.of(2028, 6, 1), false
        );
        ResponseEntity<DriverResponse> response = restTemplate.exchange(
            "/api/drivers/" + id, HttpMethod.PUT,
            new HttpEntity<>(update, authHeaders), DriverResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().firstName()).isEqualTo("Jane");
        assertThat(response.getBody().active()).isFalse();
    }

    @Test
    void updateDriver_nonExistentDriver_returns404() {
        DriverRequest update = sampleRequest("LIC-NONE", "none@example.com");
        ResponseEntity<Object> response = restTemplate.exchange(
            "/api/drivers/" + UUID.randomUUID(), HttpMethod.PUT,
            new HttpEntity<>(update, authHeaders), Object.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteDriver_existingDriver_returns204AndNotFoundAfter() {
        DriverRequest create = sampleRequest("LIC-IT-DEL", "del@example.com");
        ResponseEntity<DriverResponse> created = restTemplate.postForEntity(
            "/api/drivers", new HttpEntity<>(create, authHeaders), DriverResponse.class
        );
        UUID id = created.getBody().id();

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
            "/api/drivers/" + id, HttpMethod.DELETE,
            new HttpEntity<>(authHeaders), Void.class
        );
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Object> getResponse = restTemplate.exchange(
            "/api/drivers/" + id, HttpMethod.GET,
            new HttpEntity<>(authHeaders), Object.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteDriver_nonExistentDriver_returns404() {
        ResponseEntity<Object> response = restTemplate.exchange(
            "/api/drivers/" + UUID.randomUUID(), HttpMethod.DELETE,
            new HttpEntity<>(authHeaders), Object.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAll_afterInsertingTwo_returnsBoth() {
        restTemplate.postForEntity("/api/drivers",
            new HttpEntity<>(sampleRequest("LIC-IT-A1", "a1@example.com"), authHeaders), DriverResponse.class);
        restTemplate.postForEntity("/api/drivers",
            new HttpEntity<>(sampleRequest("LIC-IT-A2", "a2@example.com"), authHeaders), DriverResponse.class);

        ResponseEntity<DriverResponse[]> response = restTemplate.exchange(
            "/api/drivers", HttpMethod.GET,
            new HttpEntity<>(authHeaders), DriverResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void create_withoutToken_returns401() {
        DriverRequest request = sampleRequest("LIC-IT-NOAUTH", "noauth@example.com");
        ResponseEntity<Object> response = restTemplate.postForEntity(
            "/api/drivers", new HttpEntity<>(request), Object.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
