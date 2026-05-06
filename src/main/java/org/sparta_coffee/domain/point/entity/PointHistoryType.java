package org.sparta_coffee.domain.point.entity;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointHistoryType {
    CHARGE,
    USE,
    REFUND
}
