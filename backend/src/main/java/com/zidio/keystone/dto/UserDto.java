package com.zidio.keystone.dto;

import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;

import java.util.UUID;

public record UserDto(UUID id, String name, String email, Role role) {
    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
