package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.TurningPointResponse;
import com.example.aidatingagentbackend.entity.TurningPoint;
import com.example.aidatingagentbackend.repository.TurningPointRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TurningPointService {

    private final TurningPointRepository turningPointRepository;

    public TurningPointService(TurningPointRepository turningPointRepository) {
        this.turningPointRepository = turningPointRepository;
    }

    @Transactional(readOnly = true)
    public List<TurningPointResponse> findAll() {
        return turningPointRepository.findAll()
                .stream()
                .map(TurningPointResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TurningPointResponse findById(Long id) {
        TurningPoint turningPoint = turningPointRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turning point not found. id=" + id));

        return TurningPointResponse.from(turningPoint);
    }
}
