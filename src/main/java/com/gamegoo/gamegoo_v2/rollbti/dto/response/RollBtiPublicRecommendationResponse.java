package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiPublicRecommendationResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int page;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int requestedSize;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int count;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @ArraySchema(schema = @Schema(implementation = RollBtiMemberCardResponse.class))
    private List<RollBtiMemberCardResponse> recommendations;

    public static RollBtiPublicRecommendationResponse of(
            int page,
            int requestedSize,
            List<RollBtiMemberCardResponse> recommendations,
            boolean hasNext) {
        return RollBtiPublicRecommendationResponse.builder()
                .page(page)
                .requestedSize(requestedSize)
                .count(recommendations.size())
                .hasNext(hasNext)
                .recommendations(recommendations)
                .build();
    }
}
