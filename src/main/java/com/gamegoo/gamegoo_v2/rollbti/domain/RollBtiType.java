package com.gamegoo.gamegoo_v2.rollbti.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(enumAsRef = true, description = "RollBTI 16 type")
public enum RollBtiType {
    ADCI,
    ADCB,
    ADTI,
    ADTB,
    ASCI,
    ASCB,
    ASTI,
    ASTB,
    FDCI,
    FDCB,
    FDTI,
    FDTB,
    FSCI,
    FSCB,
    FSTI,
    FSTB
}
