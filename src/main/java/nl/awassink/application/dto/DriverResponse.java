package nl.awassink.application.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DriverResponse(
    UUID id,
    String firstName,
    String lastName,
    String licenseNumber,
    String phoneNumber,
    String email,
    LocalDate licenseExpiryDate,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
