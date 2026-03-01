package com.gamegoo.gamegoo_v2.rollbti.repository;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiGuestResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RollBtiGuestResultRepository extends JpaRepository<RollBtiGuestResult, Long> {

    boolean existsByResultId(String resultId);

    Optional<RollBtiGuestResult> findByResultId(String resultId);
}

