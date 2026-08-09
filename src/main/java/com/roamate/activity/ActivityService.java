package com.roamate.activity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivitySessionRepository repository;

    public ActivityService(ActivitySessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ActivitySession recordBatch(ActivitySession session) {
        return repository.save(session);
    }

    public List<ActivitySession> history(UUID tripId, String userId) {
        return repository.findByTripIdAndUserId(tripId, userId);
    }
}
