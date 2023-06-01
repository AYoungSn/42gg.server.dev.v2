package com.gg.server.domain.rank.redis;

import com.gg.server.domain.rank.data.Rank;
import com.gg.server.domain.user.User;
import com.gg.server.domain.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@RedisHash("rank")
@Getter
@Builder
@AllArgsConstructor
public class RankRedis implements Serializable {

    private Long userId;
    private String intraId;
    private int ppp;
    private int wins;
    private int losses;
    private String statusMessage;

    private String userImageUri;

    public void updateRank(int changePpp, int wins, int losses) {
        this.ppp += changePpp;
        this.wins = wins;
        this.losses = losses;
    }

    public void setStatusMessage(String msg) {
        this.statusMessage = msg;
    }

    public static RankRedis from(UserDto user, Integer ppp) {
        RankRedis rankRedis = RankRedis.builder()
                .userId(user.getId())
                .intraId(user.getIntraId())
                .ppp(ppp)
                .wins(0)
                .losses(0)
                .statusMessage("")
                .userImageUri(user.getImageUri())
                .build();
        return rankRedis;
    }

    public static RankRedis from(Rank rank){
        RankRedis rankRedis = RankRedis.builder()
                .userId(rank.getUser().getId())
                .intraId(rank.getUser().getIntraId())
                .ppp(rank.getPpp())
                .wins(rank.getWins())
                .losses(rank.getLosses())
                .statusMessage(rank.getStatusMessage())
                .userImageUri(rank.getUser().getImageUri())
                .build();
        return rankRedis;
    }

}
