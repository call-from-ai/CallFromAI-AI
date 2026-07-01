package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
}
