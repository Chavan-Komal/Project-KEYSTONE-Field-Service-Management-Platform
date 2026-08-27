package com.zidio.keystone.dto;

public record LoginResponse(String token, UserDto user) {}
