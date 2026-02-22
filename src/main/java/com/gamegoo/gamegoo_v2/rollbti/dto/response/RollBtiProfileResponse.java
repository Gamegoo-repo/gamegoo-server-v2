package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.rollbti.domain.MemberRollBtiProfile;
import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RollBtiProfileResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long memberId;

    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updatedAt;

    public static RollBtiProfileResponse of(MemberRollBtiProfile profile) {
        return RollBtiProfileResponse.builder()
                .memberId(profile.getMember().getId())
                .type(profile.getRollBtiType())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}

