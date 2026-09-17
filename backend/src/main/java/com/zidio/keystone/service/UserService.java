package com.zidio.keystone.service;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.dto.CreateTechnicianRequest;
import com.zidio.keystone.dto.TechnicianDto;
import com.zidio.keystone.dto.UpdateTechnicianBaseRequest;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.service.GeocodingService.GeoPoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GeocodingService geocodingService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, GeocodingService geocodingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.geocodingService = geocodingService;
    }

    // Backs the technician picker on the dispatcher/manager assign flow (F4),
    // and the manager's technician roster screen.
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public List<TechnicianDto> listTechnicians() {
        return userRepository.findByRoleOrderByNameAsc(Role.TECHNICIAN).stream()
            .map(TechnicianDto::from)
            .toList();
    }

    // Manager provisions a technician account (Section 03: staff accounts are
    // manager-provisioned, never self-registered). baseAddress is geocoded
    // best-effort so the technician shows up on the tracking map and in
    // nearest-technician suggestions right away.
    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public TechnicianDto createTechnician(CreateTechnicianRequest request) {
        if (userRepository.findByEmail(request.email().trim()).isPresent()) {
            throw new IllegalArgumentException("An account with that email already exists.");
        }

        User.Builder builder = User.builder()
            .name(request.name())
            .email(request.email().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(Role.TECHNICIAN);

        if (request.baseAddress() != null && !request.baseAddress().isBlank()) {
            builder.baseAddress(request.baseAddress());
            geocodingService.geocode(request.baseAddress()).ifPresent(p -> {
                builder.baseLatitude(p.latitude());
                builder.baseLongitude(p.longitude());
            });
        }

        User saved = userRepository.save(builder.build());
        return TechnicianDto.from(saved);
    }

    // Sets or changes a technician's home-base address after the fact.
    @PreAuthorize("hasRole('MANAGER')")
    @Transactional
    public TechnicianDto updateTechnicianBase(UUID technicianId, UpdateTechnicianBaseRequest request) {
        User technician = userRepository.findById(technicianId)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("That user is not a technician.");
        }

        technician.setBaseAddress(request.baseAddress());
        GeoPoint point = geocodingService.geocode(request.baseAddress()).orElse(null);
        technician.setBaseLatitude(point != null ? point.latitude() : null);
        technician.setBaseLongitude(point != null ? point.longitude() : null);

        return TechnicianDto.from(userRepository.save(technician));
    }
}
