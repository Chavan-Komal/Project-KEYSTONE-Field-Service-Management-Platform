package com.zidio.keystone.service;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.dto.UserDto;
import com.zidio.keystone.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Backs the technician picker on the dispatcher/manager assign flow (F4).
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public List<UserDto> listTechnicians() {
        return userRepository.findByRoleOrderByNameAsc(Role.TECHNICIAN).stream()
            .map(UserDto::from)
            .toList();
    }
}
