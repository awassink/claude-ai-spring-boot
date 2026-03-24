package nl.awassink.application.service;

import nl.awassink.application.dto.DriverRequest;
import nl.awassink.application.dto.DriverResponse;
import nl.awassink.application.mapper.DriverMapper;
import nl.awassink.domain.model.Driver;
import nl.awassink.domain.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    private DriverMapper driverMapper;

    private DriverService driverService;

    private Driver driver;
    private DriverRequest request;
    private UUID driverId;

    @BeforeEach
    void setUp() {
        driverMapper = new DriverMapper();
        driverService = new DriverService(driverRepository, driverMapper);
        driverId = UUID.randomUUID();
        driver = new Driver();
        driver.setId(driverId);
        driver.setFirstName("John");
        driver.setLastName("Doe");
        driver.setLicenseNumber("LIC-001");
        driver.setPhoneNumber("+31612345678");
        driver.setEmail("john.doe@example.com");
        driver.setLicenseExpiryDate(LocalDate.of(2027, 1, 1));
        driver.setActive(true);

        request = new DriverRequest(
            "John", "Doe", "LIC-001", "+31612345678",
            "john.doe@example.com", LocalDate.of(2027, 1, 1), true
        );
    }

    @Test
    void findAll_returnsAllDrivers() {
        when(driverRepository.findAll()).thenReturn(List.of(driver));

        List<DriverResponse> result = driverService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("John");
    }

    @Test
    void findById_existingId_returnsDriver() {
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));

        DriverResponse result = driverService.findById(driverId);

        assertThat(result.id()).isEqualTo(driverId);
        assertThat(result.lastName()).isEqualTo("Doe");
    }

    @Test
    void findById_nonExistentId_throwsNoSuchElementException() {
        UUID unknownId = UUID.randomUUID();
        when(driverRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.findById(unknownId))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Driver not found");
    }

    @Test
    void create_validRequest_createsAndReturnsDriver() {
        when(driverRepository.existsByLicenseNumber(request.licenseNumber())).thenReturn(false);
        when(driverRepository.existsByEmail(request.email())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        DriverResponse result = driverService.create(request);

        assertThat(result.firstName()).isEqualTo("John");
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    void create_duplicateLicenseNumber_throwsIllegalStateException() {
        when(driverRepository.existsByLicenseNumber(request.licenseNumber())).thenReturn(true);

        assertThatThrownBy(() -> driverService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("License number already registered");

        verify(driverRepository, never()).save(any());
    }

    @Test
    void create_duplicateEmail_throwsIllegalStateException() {
        when(driverRepository.existsByLicenseNumber(request.licenseNumber())).thenReturn(false);
        when(driverRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> driverService.create(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Email already registered");

        verify(driverRepository, never()).save(any());
    }

    @Test
    void update_existingDriver_updatesFields() {
        DriverRequest updateRequest = new DriverRequest(
            "Jane", "Smith", "LIC-002", "+31698765432",
            "jane.smith@example.com", LocalDate.of(2028, 6, 1), false
        );
        when(driverRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        driverService.update(driverId, updateRequest);

        verify(driverRepository).save(driver);
        assertThat(driver.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void update_nonExistentDriver_throwsNoSuchElementException() {
        UUID unknownId = UUID.randomUUID();
        when(driverRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.update(unknownId, request))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Driver not found");
    }

    @Test
    void delete_existingDriver_deletesSuccessfully() {
        when(driverRepository.existsById(driverId)).thenReturn(true);

        driverService.delete(driverId);

        verify(driverRepository).deleteById(driverId);
    }

    @Test
    void delete_nonExistentDriver_throwsNoSuchElementException() {
        UUID unknownId = UUID.randomUUID();
        when(driverRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> driverService.delete(unknownId))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Driver not found");

        verify(driverRepository, never()).deleteById(any());
    }
}
