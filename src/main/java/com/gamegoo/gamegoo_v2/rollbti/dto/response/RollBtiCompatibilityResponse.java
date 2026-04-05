package com.gamegoo.gamegoo_v2.rollbti.dto.response;

import com.gamegoo.gamegoo_v2.rollbti.domain.RollBtiType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RollBtiCompatibilityResponse {

    @Schema(ref = "#/components/schemas/RollBtiType", requiredMode = Schema.RequiredMode.REQUIRED)
    private RollBtiType type;

    @ArraySchema(schema = @Schema(ref = "#/components/schemas/RollBtiType"))
    private List<RollBtiType> goodMatches;

    @ArraySchema(schema = @Schema(ref = "#/components/schemas/RollBtiType"))
    private List<RollBtiType> badMatches;

    public static RollBtiCompatibilityResponse of(RollBtiType type, List<RollBtiType> goodMatches,
                                                  List<RollBtiType> badMatches) {
        return RollBtiCompatibilityResponse.builder()
                .type(type)
                .goodMatches(goodMatches)
                .badMatches(badMatches)
                .build();
    }
}

