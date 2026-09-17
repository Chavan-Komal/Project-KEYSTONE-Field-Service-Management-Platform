package com.zidio.keystone.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTechnicianBaseRequest(@NotBlank String baseAddress) {}
