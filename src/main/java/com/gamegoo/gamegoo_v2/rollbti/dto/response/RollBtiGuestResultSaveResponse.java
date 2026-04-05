package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiGuestResult;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RollBtiGuestResultSaveResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String resultId;

    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createdAt;

    public static RollBtiGuestResultSaveResponse of(RollBtiGuestResult result) {
        return RollBtiGuestResultSaveResponse.builder()
                .resultId(result.getResultId())
                .type(result.getRollBtiType())
                .createdAt(result.getCreatedAt())
                .build();
    }
}

