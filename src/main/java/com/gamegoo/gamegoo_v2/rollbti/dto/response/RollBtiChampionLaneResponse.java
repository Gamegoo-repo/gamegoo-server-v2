package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiChampionLaneResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Lane code: TOP, JUNGLE, MID, ADC, SUP")
    private String lane;

    @ArraySchema(schema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED))
    private List<String> champions;

    public static RollBtiChampionLaneResponse of(String lane, List<String> champions) {
        return RollBtiChampionLaneResponse.builder()
                .lane(lane)
                .champions(champions)
                .build();
    }
}

