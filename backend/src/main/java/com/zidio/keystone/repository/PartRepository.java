package com.zidio.keystone.repository;

import com.zidio.keystone.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID> {
}
