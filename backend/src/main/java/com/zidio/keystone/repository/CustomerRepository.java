package com.zidio.keystone.repository;

import com.zidio.keystone.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
