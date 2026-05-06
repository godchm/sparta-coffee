package org.sparta_coffee.global.exception.domain;

import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.common.ServiceException;

public class PointException extends ServiceException {
    public PointException(ErrorCode errorCode) {
        super(errorCode);
    }
}
