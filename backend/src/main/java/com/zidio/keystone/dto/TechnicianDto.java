package com.zidio.keystone.dto;

import com.zidio.keystone.domain.User;

import java.util.UUID;

// distanceKm is only populated by the nearest-technicians lookup — null
// everywhere else (e.g. the plain roster list).
public record TechnicianDto(
    UUID id,
    String name,
    String email,
    String baseAddress,
    Double baseLatitude,
    Double baseLongitude,
    Double distanceKm
) {
    public static TechnicianDto from(User u) {
        return new TechnicianDto(u.getId(), u.getName(), u.getEmail(), u.getBaseAddress(), u.getBaseLatitude(), u.getBaseLongitude(), null);
    }

    public TechnicianDto withDistanceKm(Double distanceKm) {
        return new TechnicianDto(id, name, email, baseAddress, baseLatitude, baseLongitude, distanceKm);
    }
}
