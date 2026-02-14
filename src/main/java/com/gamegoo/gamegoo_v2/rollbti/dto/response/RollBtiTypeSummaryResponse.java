package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiTypeSummaryResponse {

    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType type;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String alias;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    @ArraySchema(schema = @Schema(ref = "#/components/schemas/RollBtiType"))
    private List<RollBtiType> goodMatches;

    @ArraySchema(schema = @Schema(ref = "#/components/schemas/RollBtiType"))
    private List<RollBtiType> badMatches;

    @ArraySchema(schema = @Schema(implementation = RollBtiChampionLaneResponse.class))
    private List<RollBtiChampionLaneResponse> laneRecommendations;

    public static RollBtiTypeSummaryResponse of(
            RollBtiType type,
            String alias,
            String description,
            List<RollBtiType> goodMatches,
            List<RollBtiType> badMatches,
            List<RollBtiChampionLaneResponse> laneRecommendations) {
        return RollBtiTypeSummaryResponse.builder()
                .type(type)
                .alias(alias)
                .description(description)
                .goodMatches(goodMatches)
                .badMatches(badMatches)
                .laneRecommendations(laneRecommendations)
                .build();
    }
}

