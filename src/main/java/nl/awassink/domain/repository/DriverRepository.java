package nl.awassink.domain.repository;

import nl.awassink.domain.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    Optional<Driver> findByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByEmail(String email);
}
