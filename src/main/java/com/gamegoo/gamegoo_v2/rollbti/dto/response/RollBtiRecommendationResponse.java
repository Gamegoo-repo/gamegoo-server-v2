package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiRecommendationResponse {

    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType requesterType;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int page;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int requestedSize;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private int count;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @ArraySchema(schema = @Schema(implementation = RollBtiRecommendedMemberResponse.class))
    private List<RollBtiRecommendedMemberResponse> recommendations;

    public static RollBtiRecommendationResponse of(
            RollBtiType requesterType,
            int page,
            int requestedSize,
            List<RollBtiRecommendedMemberResponse> recommendations,
            boolean hasNext) {
        return RollBtiRecommendationResponse.builder()
                .requesterType(requesterType)
                .page(page)
                .requestedSize(requestedSize)
                .count(recommendations.size())
                .hasNext(hasNext)
                .recommendations(recommendations)
                .build();
    }
}
