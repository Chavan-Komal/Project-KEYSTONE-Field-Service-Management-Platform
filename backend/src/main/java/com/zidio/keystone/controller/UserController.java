package com.zidio.keystone.controller;

import com.zidio.keystone.dto.CreateTechnicianRequest;
import com.zidio.keystone.dto.TechnicianDto;
import com.zidio.keystone.dto.UpdateTechnicianBaseRequest;
import com.zidio.keystone.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/technicians")
    public List<TechnicianDto> technicians() {
        return userService.listTechnicians();
    }

    @PostMapping("/technicians")
    @ResponseStatus(HttpStatus.CREATED)
    public TechnicianDto createTechnician(@Valid @RequestBody CreateTechnicianRequest request) {
        return userService.createTechnician(request);
    }

    @PatchMapping("/technicians/{id}/base")
    public TechnicianDto updateTechnicianBase(@PathVariable UUID id, @Valid @RequestBody UpdateTechnicianBaseRequest request) {
        return userService.updateTechnicianBase(id, request);
    }
}
