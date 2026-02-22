package com.gamegoo.gamegoo_v2.core.exception;

import com.gamegoo.gamegoo_v2.core.exception.common.ErrorCode;
import com.gamegoo.gamegoo_v2.core.exception.common.GlobalException;

public class RollBtiException extends GlobalException {

    public RollBtiException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RollBtiException(ErrorCode errorCode, String customMessage) {
        super(errorCode, customMessage);
    }
}

