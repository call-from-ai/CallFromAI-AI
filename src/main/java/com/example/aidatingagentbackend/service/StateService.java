package com.example.aidatingagentbackend.service;

import com.example.aidatingagentbackend.dto.StateRequest;
import com.example.aidatingagentbackend.dto.StateResponse;
import com.example.aidatingagentbackend.entity.State;
import com.example.aidatingagentbackend.repository.StateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class StateService {

    private final StateRepository stateRepository;

    public StateService(StateRepository stateRepository) {
        this.stateRepository = stateRepository;
    }

    @Transactional
    public StateResponse create(StateRequest request) {
        State state = new State();
        applyRequest(state, request);
        return StateResponse.from(stateRepository.save(state));
    }

    @Transactional(readOnly = true)
    public List<StateResponse> findAll() {
        return stateRepository.findAll()
                .stream()
                .map(StateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StateResponse findById(Long id) {
        return StateResponse.from(findState(id));
    }

    @Transactional
    public StateResponse update(Long id, StateRequest request) {
        State state = findState(id);
        applyRequest(state, request);
        return StateResponse.from(stateRepository.save(state));
    }

    @Transactional
    public void delete(Long id) {
        State state = findState(id);
        stateRepository.delete(state);
    }

    private State findState(Long id) {
        return stateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "State not found. id=" + id));
    }

    private void applyRequest(State state, StateRequest request) {
        state.setEmotion(request.getEmotion());
        state.setEmotionIntensity(request.getEmotionIntensity());
        state.setEnergy(request.getEnergy());
        state.setStress(request.getStress());
        state.setThinking(request.getThinking());
        state.setGoal(request.getGoal());
        state.refreshUpdatedAt();
    }
}
