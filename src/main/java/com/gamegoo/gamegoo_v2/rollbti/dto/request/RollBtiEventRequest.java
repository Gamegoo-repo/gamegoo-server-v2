package com.gamegoo.gamegoo_v2.rollbti.dto.request;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiEventType;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RollBtiEventRequest {

    @NotNull(message = "eventType은 필수 값입니다.")
    @Schema(ref = "#/components/schemas/RollBtiEventType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiEventType eventType;

    @Schema(description = "겜구 회원 id (선택)")
    private Long memberId;

    @Schema(ref = "#/components/schemas/RollBtiType", description = "이벤트 발생 시점의 롤BTI 타입 (선택)")
    private RollBtiType rollBtiType;

    @Size(max = 120, message = "sessionId는 120자 이하여야 합니다.")
    @Schema(description = "롤BTI 프론트 세션 식별자 (선택)")
    private String sessionId;

    @Size(max = 80, message = "eventSource는 80자 이하여야 합니다.")
    @Schema(description = "이벤트 소스 (예: WEB, APP, AD) (선택)")
    private String eventSource;
}
