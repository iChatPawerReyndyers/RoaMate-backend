package com.roamate.finance.repo;

import com.roamate.finance.domain.KittyDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KittyDepositRepository extends JpaRepository<KittyDeposit, UUID> {
    List<KittyDeposit> findByTripIdAndDeletedFalse(UUID tripId);
}
