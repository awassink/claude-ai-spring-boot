package nl.awassink.application.mapper;

import nl.awassink.application.dto.DriverRequest;
import nl.awassink.application.dto.DriverResponse;
import nl.awassink.domain.model.Driver;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {

    public Driver toEntity(DriverRequest request) {
        Driver driver = new Driver();
        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setEmail(request.email());
        driver.setLicenseExpiryDate(request.licenseExpiryDate());
        driver.setActive(request.active());
        return driver;
    }

    public DriverResponse toResponse(Driver driver) {
        return new DriverResponse(
            driver.getId(),
            driver.getFirstName(),
            driver.getLastName(),
            driver.getLicenseNumber(),
            driver.getPhoneNumber(),
            driver.getEmail(),
            driver.getLicenseExpiryDate(),
            driver.isActive(),
            driver.getCreatedAt(),
            driver.getUpdatedAt()
        );
    }

    public void updateEntity(Driver driver, DriverRequest request) {
        driver.setFirstName(request.firstName());
        driver.setLastName(request.lastName());
        driver.setLicenseNumber(request.licenseNumber());
        driver.setPhoneNumber(request.phoneNumber());
        driver.setEmail(request.email());
        driver.setLicenseExpiryDate(request.licenseExpiryDate());
        driver.setActive(request.active());
    }
}
