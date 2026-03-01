package com.gamegoo.gamegoo_v2.rollbti.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RollBtiGuestResultSaveRequest {

    @NotNull(message = "type은 필수 값입니다.")
    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType type;

    @NotNull(message = "resultPayload는 필수 값입니다.")
    @Schema(description = "공유할 롤BTI 결과 원본 payload")
    private JsonNode resultPayload;

    @Size(max = 120, message = "sessionId는 120자 이하여야 합니다.")
    @Schema(description = "비회원 세션 식별자 (선택)")
    private String sessionId;
}

