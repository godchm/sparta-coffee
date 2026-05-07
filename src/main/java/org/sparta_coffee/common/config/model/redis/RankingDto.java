package org.sparta_coffee.common.config.model.redis;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankingDto {

    private double score;
    private String keyword;
    private long price;
    private String rankingDateTime;
}