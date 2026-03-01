package com.gamegoo.gamegoo_v2.rollbti.service;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiGuestResult;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import com.gamegoo.gamegoo_v2.rollbti.repository.RollBtiGuestResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RollBtiGuestResultSaver {

    private final RollBtiGuestResultRepository rollBtiGuestResultRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<RollBtiGuestResult> trySave(String resultId, RollBtiType type, String payload, String sessionId) {
        try {
            RollBtiGuestResult guestResult = RollBtiGuestResult.create(resultId, type, payload, sessionId);
            return Optional.of(rollBtiGuestResultRepository.saveAndFlush(guestResult));
        } catch (DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

}