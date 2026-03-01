package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RollBtiParticipationCountResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalParticipants;

    public static RollBtiParticipationCountResponse of(long totalParticipants) {
        return RollBtiParticipationCountResponse.builder()
                .totalParticipants(totalParticipants)
                .build();
    }
}

