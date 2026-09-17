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
import com.zidio.keystone.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final GeocodingService geocodingService;

    public CustomerService(CustomerRepository customerRepository, SiteRepository siteRepository, GeocodingService geocodingService) {
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.geocodingService = geocodingService;
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

        Site.Builder siteBuilder = Site.builder()
            .customer(customer)
            .name(request.name())
            .address(request.address());

        // Best-effort — a failed lookup just leaves the site without
        // coordinates rather than blocking creation (see GeocodingService).
        geocodingService.geocode(request.address()).ifPresent(p -> {
            siteBuilder.latitude(p.latitude());
            siteBuilder.longitude(p.longitude());
        });

        return SiteDto.from(siteRepository.save(siteBuilder.build()));
    }

    // Staff-only: picking a customer's sites when raising a work order on
    // their behalf. Customers use listMySites() below instead — narrower,
    // and doesn't let one customer enumerate another's sites by guessing IDs.
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public List<SiteDto> listSitesForCustomer(UUID customerId) {
        return siteRepository.findByCustomerId(customerId).stream().map(SiteDto::from).toList();
    }

    // The "which site?" picker on a customer's own "Raise a Request" form —
    // scoped to their own organisation via the JWT, never a client-supplied ID.
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<SiteDto> listMySites() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return siteRepository.findByCustomerId(principal.getCustomerId()).stream().map(SiteDto::from).toList();
    }
}
