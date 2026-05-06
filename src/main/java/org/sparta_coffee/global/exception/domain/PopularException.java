package org.sparta_coffee.global.exception.domain;

import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.common.ServiceException;

public class PopularException extends ServiceException {
    public PopularException(ErrorCode errorCode) {
        super(errorCode);
    }
}
