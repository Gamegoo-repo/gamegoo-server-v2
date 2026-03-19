package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiRecommendationCursorResponse {

    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType requesterType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int requestedSize;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int count;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema
    private Long nextCursorMemberId;

    @ArraySchema(schema = @Schema(implementation = RollBtiRecommendedMemberResponse.class))
    private List<RollBtiRecommendedMemberResponse> recommendations;

    public static RollBtiRecommendationCursorResponse of(
            RollBtiType requesterType,
            int requestedSize,
            List<RollBtiRecommendedMemberResponse> recommendations,
            boolean hasNext,
            Long nextCursorMemberId) {
        return RollBtiRecommendationCursorResponse.builder()
                .requesterType(requesterType)
                .requestedSize(requestedSize)
                .count(recommendations.size())
                .hasNext(hasNext)
                .nextCursorMemberId(nextCursorMemberId)
                .recommendations(recommendations)
                .build();
    }
}
