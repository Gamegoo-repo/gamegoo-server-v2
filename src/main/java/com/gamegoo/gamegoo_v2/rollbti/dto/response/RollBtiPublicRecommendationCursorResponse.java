package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiPublicRecommendationCursorResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int requestedSize;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int count;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema
    private Long nextCursorMemberId;

    @ArraySchema(schema = @Schema(implementation = RollBtiMemberCardResponse.class))
    private List<RollBtiMemberCardResponse> recommendations;

    public static RollBtiPublicRecommendationCursorResponse of(
            int requestedSize,
            List<RollBtiMemberCardResponse> recommendations,
            boolean hasNext,
            Long nextCursorMemberId) {
        return RollBtiPublicRecommendationCursorResponse.builder()
                .requestedSize(requestedSize)
                .count(recommendations.size())
                .hasNext(hasNext)
                .nextCursorMemberId(nextCursorMemberId)
                .recommendations(recommendations)
                .build();
    }
}
