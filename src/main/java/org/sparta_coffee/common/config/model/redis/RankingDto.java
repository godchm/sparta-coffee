package org.sparta_coffee.common.config.model.redis;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingDto {

    private String keyword;
    private double score;
    private long price;
}