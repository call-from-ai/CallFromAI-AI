package com.example.aidatingagentbackend.repository;

import com.example.aidatingagentbackend.entity.Reflection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReflectionRepository extends JpaRepository<Reflection, Long> {

    List<Reflection> findTop10ByUserIdOrderByImportanceDescCreatedAtDesc(Long userId);
}
