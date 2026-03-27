package nl.awassink.presentation.rest;

import tools.jackson.databind.ObjectMapper;
import nl.awassink.application.dto.DriverRequest;
import nl.awassink.application.dto.DriverResponse;
import nl.awassink.application.service.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DriverService driverService;

    private static final UUID DRIVER_ID = UUID.randomUUID();

    private DriverResponse sampleResponse() {
        return new DriverResponse(
            DRIVER_ID, "John", "Doe", "LIC-001", "+31612345678",
            "john.doe@example.com", LocalDate.of(2027, 1, 1), true,
            Instant.now(), Instant.now()
        );
    }

    private DriverRequest sampleRequest() {
        return new DriverRequest(
            "John", "Doe", "LIC-001", "+31612345678",
            "john.doe@example.com", LocalDate.of(2027, 1, 1), true
        );
    }

    @Test
    void findAll_authenticated_returns200() throws Exception {
        when(driverService.findAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/drivers").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void findAll_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/drivers"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void findById_existingId_returns200() throws Exception {
        when(driverService.findById(DRIVER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/drivers/" + DRIVER_ID).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(DRIVER_ID.toString()));
    }

    @Test
    void findById_nonExistentId_returns404() throws Exception {
        when(driverService.findById(DRIVER_ID)).thenThrow(new NoSuchElementException("Driver not found: " + DRIVER_ID));

        mockMvc.perform(get("/api/drivers/" + DRIVER_ID).with(jwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void findById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/drivers/not-a-uuid").with(jwt()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(driverService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/drivers")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.licenseNumber").value("LIC-001"));
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        DriverRequest invalid = new DriverRequest(
            "", "Doe", "LIC-001", null,
            "john.doe@example.com", LocalDate.of(2027, 1, 1), true
        );

        mockMvc.perform(post("/api/drivers")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateLicense_returns409() throws Exception {
        when(driverService.create(any())).thenThrow(new IllegalStateException("License number already registered: LIC-001"));

        mockMvc.perform(post("/api/drivers")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
            .andExpect(status().isConflict());
    }

    @Test
    void update_existingDriver_returns200() throws Exception {
        when(driverService.update(eq(DRIVER_ID), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/drivers/" + DRIVER_ID)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void update_nonExistentDriver_returns404() throws Exception {
        when(driverService.update(eq(DRIVER_ID), any())).thenThrow(new NoSuchElementException("Driver not found: " + DRIVER_ID));

        mockMvc.perform(put("/api/drivers/" + DRIVER_ID)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest())))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingDriver_returns204() throws Exception {
        mockMvc.perform(delete("/api/drivers/" + DRIVER_ID).with(jwt()))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentDriver_returns404() throws Exception {
        doThrow(new NoSuchElementException("Driver not found: " + DRIVER_ID))
            .when(driverService).delete(DRIVER_ID);

        mockMvc.perform(delete("/api/drivers/" + DRIVER_ID).with(jwt()))
            .andExpect(status().isNotFound());
    }
}
