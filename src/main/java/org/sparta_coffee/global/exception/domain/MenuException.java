package org.sparta_coffee.global.exception.domain;

import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.common.ServiceException;

public class MenuException extends ServiceException {
    public MenuException(ErrorCode errorCode) {
        super(errorCode);
    }
}
