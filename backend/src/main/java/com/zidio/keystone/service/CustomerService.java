package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Site;
import com.zidio.keystone.dto.CreateCustomerRequest;
import com.zidio.keystone.dto.CreateSiteRequest;
import com.zidio.keystone.dto.CustomerDto;
import com.zidio.keystone.dto.SiteDto;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.SiteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;

    public CustomerService(CustomerRepository customerRepository, SiteRepository siteRepository) {
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
    }

    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Transactional
    public CustomerDto createCustomer(CreateCustomerRequest request) {
        Customer customer = Customer.builder()
            .name(request.name())
            .contactEmail(request.contactEmail())
            .build();
        return CustomerDto.from(customerRepository.save(customer));
    }

    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public Page<CustomerDto> listCustomers(String search, Pageable pageable) {
        Page<Customer> page = (search == null || search.isBlank())
            ? customerRepository.findAll(pageable)
            : customerRepository.findByNameContainingIgnoreCase(search, pageable);
        return page.map(CustomerDto::from);
    }

    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    @Transactional
    public SiteDto createSite(UUID customerId, CreateSiteRequest request) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));

        Site site = Site.builder()
            .customer(customer)
            .name(request.name())
            .address(request.address())
            .build();

        return SiteDto.from(siteRepository.save(site));
    }

    @PreAuthorize("isAuthenticated()")
    public List<SiteDto> listSitesForCustomer(UUID customerId) {
        return siteRepository.findByCustomerId(customerId).stream().map(SiteDto::from).toList();
    }
}
