package com.gamegoo.gamegoo_v2.rollbti.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "RollBTI event type")
public enum RollBtiEventType {
    START_TEST,
    COMPLETE_TEST,
    GO_TO_GAMEGOO,
    SIGNUP_COMPLETE,
    RESULT_CARD_SAVE,
    RESULT_CARD_SHARE
}
