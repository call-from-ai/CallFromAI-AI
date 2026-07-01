package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.MemoryRequest;
import com.example.aidatingagentbackend.dto.MemoryResponse;
import com.example.aidatingagentbackend.entity.Memory;
import com.example.aidatingagentbackend.repository.MemoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Transactional
    public MemoryResponse create(MemoryRequest request) {
        Memory memory = new Memory();
        applyRequest(memory, request);
        return MemoryResponse.from(memoryRepository.save(memory));
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> findAll() {
        return memoryRepository.findAll()
                .stream()
                .map(MemoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemoryResponse findById(Long id) {
        return MemoryResponse.from(findMemory(id));
    }

    @Transactional
    public MemoryResponse update(Long id, MemoryRequest request) {
        Memory memory = findMemory(id);
        applyRequest(memory, request);
        return MemoryResponse.from(memoryRepository.save(memory));
    }

    @Transactional
    public void delete(Long id) {
        Memory memory = findMemory(id);
        memoryRepository.delete(memory);
    }

    private Memory findMemory(Long id) {
        return memoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory not found. id=" + id));
    }

    private void applyRequest(Memory memory, MemoryRequest request) {
        memory.setType(request.getType());
        memory.setSummary(request.getSummary());
        memory.setImportance(request.getImportance());
    }
}
