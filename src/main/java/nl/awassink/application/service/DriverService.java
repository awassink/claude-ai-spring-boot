package nl.awassink.application.service;

import nl.awassink.application.dto.DriverRequest;
import nl.awassink.application.dto.DriverResponse;
import nl.awassink.application.mapper.DriverMapper;
import nl.awassink.domain.model.Driver;
import nl.awassink.domain.repository.DriverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DriverService {

    private static final String DRIVER_NOT_FOUND = "Driver not found: ";

    private final DriverRepository driverRepository;
    private final DriverMapper driverMapper;

    public DriverService(DriverRepository driverRepository, DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.driverMapper = driverMapper;
    }

    public List<DriverResponse> findAll() {
        return driverRepository.findAll().stream()
            .map(driverMapper::toResponse)
            .toList();
    }

    public DriverResponse findById(UUID id) {
        return driverRepository.findById(id)
            .map(driverMapper::toResponse)
            .orElseThrow(() -> new NoSuchElementException(DRIVER_NOT_FOUND + id));
    }

    @Transactional
    public DriverResponse create(DriverRequest request) {
        if (driverRepository.existsByLicenseNumber(request.licenseNumber())) {
            throw new IllegalStateException("License number already registered: " + request.licenseNumber());
        }
        if (driverRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already registered: " + request.email());
        }
        Driver driver = driverMapper.toEntity(request);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse update(UUID id, DriverRequest request) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException(DRIVER_NOT_FOUND + id));
        driverMapper.updateEntity(driver, request);
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Transactional
    public void delete(UUID id) {
        if (!driverRepository.existsById(id)) {
            throw new NoSuchElementException(DRIVER_NOT_FOUND + id);
        }
        driverRepository.deleteById(id);
    }
}
