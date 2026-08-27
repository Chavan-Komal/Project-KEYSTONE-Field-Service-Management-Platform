package com.zidio.keystone.controller;

import com.zidio.keystone.dto.*;
import com.zidio.keystone.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers & Sites")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public Page<CustomerDto> list(@RequestParam(required = false) String q, Pageable pageable) {
        return customerService.listCustomers(q, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@Valid @RequestBody CreateCustomerRequest request) {
        return customerService.createCustomer(request);
    }

    @GetMapping("/{id}/sites")
    public List<SiteDto> sites(@PathVariable UUID id) {
        return customerService.listSitesForCustomer(id);
    }

    @PostMapping("/{id}/sites")
    @ResponseStatus(HttpStatus.CREATED)
    public SiteDto createSite(@PathVariable UUID id, @Valid @RequestBody CreateSiteRequest request) {
        return customerService.createSite(id, request);
    }
}
