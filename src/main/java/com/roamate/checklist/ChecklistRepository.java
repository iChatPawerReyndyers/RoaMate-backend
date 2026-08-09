package com.roamate.checklist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChecklistRepository extends JpaRepository<ChecklistItem, UUID> {
    List<ChecklistItem> findByTripIdAndCategoryAndDeletedFalse(UUID tripId, ChecklistCategory category);
}
