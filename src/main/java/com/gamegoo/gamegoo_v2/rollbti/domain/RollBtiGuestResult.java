package com.gamegoo.gamegoo_v2.rollbti.domain;

import com.gamegoo.gamegoo_v2.core.common.BaseDateTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "roll_bti_guest_result")
public class RollBtiGuestResult extends BaseDateTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roll_bti_guest_result_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String resultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RollBtiType rollBtiType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultPayload;

    @Column(length = 120)
    private String sessionId;

    @Builder
    private RollBtiGuestResult(String resultId, RollBtiType rollBtiType, String resultPayload, String sessionId) {
        this.resultId = resultId;
        this.rollBtiType = rollBtiType;
        this.resultPayload = resultPayload;
        this.sessionId = sessionId;
    }

    public static RollBtiGuestResult create(String resultId, RollBtiType rollBtiType, String resultPayload,
                                            String sessionId) {
        return RollBtiGuestResult.builder()
                .resultId(resultId)
                .rollBtiType(rollBtiType)
                .resultPayload(resultPayload)
                .sessionId(sessionId)
                .build();
    }
}

