package org.sparta_coffee.global.exception.domain;

import org.sparta_coffee.global.exception.common.ErrorCode;
import org.sparta_coffee.global.exception.common.ServiceException;

public class OrderException extends ServiceException {
    public OrderException(ErrorCode errorCode) {
        super(errorCode);
    }
}
