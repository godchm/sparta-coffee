package org.sparta_coffee.global.exception.domain;

import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.common.ServiceException;

public class UserException extends ServiceException {
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }
}
