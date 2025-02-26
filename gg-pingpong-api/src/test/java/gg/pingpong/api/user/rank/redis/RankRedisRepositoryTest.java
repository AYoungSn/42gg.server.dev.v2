package gg.pingpong.api.user.rank.redis;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gg.data.rank.redis.RankRedis;
import gg.repo.rank.redis.RankRedisRepository;
import gg.utils.annotation.IntegrationTest;
import gg.utils.exception.rank.RedisDataNotFoundException;

@IntegrationTest
class RankRedisRepositoryTest {

	@Autowired
	RankRedisRepository redisRepository;

	static String zSetKey = "test:ZSet";
	static String hashKey = "test:hash";

	@AfterEach
	void deleteAll() {
		redisRepository.deleteZSetKey(zSetKey);
		redisRepository.deleteHashKey(hashKey);
	}

	@Test
	void addToZSet() {
		//given
		Long userId = 1L;
		int ppp = 100;

		//when
		redisRepository.addToZSet(zSetKey, userId, ppp);

		//then
		Long scoreInZSet = redisRepository.getScoreInZSet(zSetKey, userId);
		Assertions.assertThat(scoreInZSet).isEqualTo(ppp);
	}

	@Test
	void incrementScoreInZSet() {
		//given
		Long userId = 1L;
		int ppp = 100;
		int incrementPpp = 50;
		redisRepository.addToZSet(zSetKey, userId, ppp);

		//when
		redisRepository.incrementScoreInZSet(zSetKey, userId, incrementPpp);

		//then
		Long scoreInZSet = redisRepository.getScoreInZSet(zSetKey, userId);
		Assertions.assertThat(scoreInZSet).isEqualTo(ppp + incrementPpp);
	}

	@Test
	void decrementScoreInZSet() {
		//given
		Long userId = 1L;
		int ppp = 100;
		int decrementPpp = 50;
		redisRepository.addToZSet(zSetKey, userId, ppp);

		//when
		redisRepository.decrementScoreInZSet(zSetKey, userId, decrementPpp);

		//then
		Long scoreInZSet = redisRepository.getScoreInZSet(zSetKey, userId);
		Assertions.assertThat(scoreInZSet).isEqualTo(ppp - decrementPpp);
	}

	@Test
	void add() {
		Long userId = 1L;
		Long userId2 = 2L;
		Long userId3 = 3L;
		int ppp = 100;
		int ppp2 = 200;
		int ppp3 = 300;
		redisRepository.addToZSet(zSetKey, userId, ppp);
		redisRepository.addToZSet(zSetKey, userId2, ppp2);
		redisRepository.addToZSet(zSetKey, userId3, ppp3);
	}

	@Test
	void getRankInZSet() {
		//        given
		Long userId = 1L;
		Long userId2 = 2L;
		Long userId3 = 3L;
		int ppp = 100;
		int ppp2 = 200;
		int ppp3 = 300;
		redisRepository.addToZSet(zSetKey, userId, ppp);
		redisRepository.addToZSet(zSetKey, userId2, ppp2);
		redisRepository.addToZSet(zSetKey, userId3, ppp3);

		//when
		Long rankInZSet = redisRepository.getRankInZSet(zSetKey, userId);

		//then
		Assertions.assertThat(rankInZSet).isEqualTo(2);
	}

	@Test
	void getScoreInZSet() {
		//given
		Long userId = 1L;
		int ppp = 100;
		redisRepository.addToZSet(zSetKey, userId, ppp);

		//when
		Long scoreInZSet = redisRepository.getScoreInZSet(zSetKey, userId);

		//then
		Assertions.assertThat(scoreInZSet).isEqualTo(ppp);
	}

	@Test
	public void getUserIdsByRange() throws Exception {
		//given
		Long userId = 1L;
		Long userId2 = 2L;
		Long userId3 = 3L;
		int ppp = 100;
		int ppp2 = 200;
		int ppp3 = 300;
		redisRepository.addToZSet(zSetKey, userId, ppp);
		redisRepository.addToZSet(zSetKey, userId2, ppp2);
		redisRepository.addToZSet(zSetKey, userId3, ppp3);

		//when
		List<Long> userIdsSorted = redisRepository.getUserIdsByRangeFromZSet(zSetKey, 0, 1);

		//then
		Assertions.assertThat(userIdsSorted).containsExactly(userId3, userId2);
		Assertions.assertThat(userIdsSorted.get(0)).isEqualTo(userId3);
		Assertions.assertThat(userIdsSorted.get(1)).isEqualTo(userId2);
	}

	@Test
	@DisplayName("ZSet에서 유저 삭제")
	public void deleteZSetTest() throws Exception {
		//given
		Long userId = 1L;
		int ppp = 100;
		redisRepository.addToZSet(zSetKey, userId, ppp);

		//when
		redisRepository.deleteFromZSet(zSetKey, userId);

		//then
		Assertions.assertThatThrownBy(() -> {
			redisRepository.getRankInZSet(zSetKey, userId);
		}).isInstanceOf(RedisDataNotFoundException.class);
	}

	@Test
	@DisplayName("Redis에 랭킹 데이터를 추가, 검색한다.")
	public void rankCreateSearch() throws Exception {
		//given
		Long userId = 1L;
		int ppp = 100;
		int win = 3;
		int lose = 4;
		String statusMessage = "statusMessage";
		RankRedis ranking = new RankRedis(userId, "aa", ppp, win, lose, statusMessage,
			"https://42gg-public-test-image.s3.ap-northeast-2.amazonaws.com/tier/hand.svg", "#000000");

		//when
		redisRepository.addRankData(hashKey, userId, ranking);
		RankRedis findRanking = redisRepository.findRankByUserId(hashKey, userId);

		//then
		Assertions.assertThat(findRanking.getUserId()).isEqualTo(userId);
		Assertions.assertThat(findRanking.getPpp()).isEqualTo(ppp);
		Assertions.assertThat(findRanking.getWins()).isEqualTo(win);
		Assertions.assertThat(findRanking.getLosses()).isEqualTo(lose);
		Assertions.assertThat(findRanking.getStatusMessage()).isEqualTo(statusMessage);
	}

	@Test
	@DisplayName("user rank정보 업데이트")
	public void updateRank() throws Exception {
		//given
		Long userId = 1L;
		int ppp = 100;
		int win = 3;
		int lose = 4;
		String statusMessage = "statusMessage";
		RankRedis ranking = new RankRedis(userId, "aa", ppp, win, lose, statusMessage,
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");
		redisRepository.addRankData(hashKey, userId, ranking);

		//when
		int newPpp = 200;
		int newWin = 4;
		int newLose = 5;
		String newStatusMessage = "newStatusMessage";
		RankRedis newRanking = new RankRedis(userId, "aa", newPpp, newWin, newLose, newStatusMessage,
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");

		redisRepository.updateRankData(hashKey, userId, newRanking);
		//then
		RankRedis findRanking = redisRepository.findRankByUserId(hashKey, userId);
		Assertions.assertThat(findRanking.getUserId()).isEqualTo(userId);
		Assertions.assertThat(findRanking.getPpp()).isEqualTo(newPpp);
		Assertions.assertThat(findRanking.getWins()).isEqualTo(newWin);
		Assertions.assertThat(findRanking.getLosses()).isEqualTo(newLose);
		Assertions.assertThat(findRanking.getStatusMessage()).isEqualTo(newStatusMessage);
	}

	@Test
	@DisplayName("유저 랭크를 삭제한다")
	public void deleteUserRank() throws Exception {
		//given
		Long userId = 3L;
		int ppp = 100;
		int win = 3;
		int lose = 4;
		String statusMessage = "statusMessage";
		RankRedis ranking = new RankRedis(userId, "aa", ppp, win, lose, statusMessage,
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");
		redisRepository.addRankData(hashKey, userId, ranking);

		//when
		redisRepository.deleteRankData(hashKey, userId);

		//then
		Assertions.assertThatThrownBy(() -> {
			redisRepository.findRankByUserId(hashKey, userId);
		}).isInstanceOf(RedisDataNotFoundException.class);
	}

	@Test
	void findRanksByUserIds() {
		//given
		Long userId = 1L;
		Long userId2 = 2L;
		Long userId3 = 3L;
		Long userId4 = 4L;
		int ppp = 100;
		int ppp2 = 200;
		int ppp3 = 300;
		int ppp4 = 400;

		RankRedis rank1 = new RankRedis(userId, "aa", ppp, 0, 0, "statusMessage",
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");
		RankRedis rank2 = new RankRedis(userId2, "aa", ppp2, 0, 0, "statusMessage",
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");
		RankRedis rank3 = new RankRedis(userId3, "aa", ppp3, 0, 0, "statusMessage",
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");
		RankRedis rank4 = new RankRedis(userId4, "aa", ppp4, 0, 0, "statusMessage",
			"https://42gg-public-image.s3.ap-northeast-2.amazonaws.com/images/nheo.jpeg", "#000000");
		redisRepository.addRankData(hashKey, userId, rank1);
		redisRepository.addRankData(hashKey, userId2, rank2);
		redisRepository.addRankData(hashKey, userId3, rank3);
		redisRepository.addRankData(hashKey, userId4, rank4);
		redisRepository.addToZSet(zSetKey, userId, ppp);
		redisRepository.addToZSet(zSetKey, userId2, ppp2);
		redisRepository.addToZSet(zSetKey, userId3, ppp3);
		redisRepository.addToZSet(zSetKey, userId4, ppp4);

		//when
		List<Long> sortedUserIds = redisRepository.getUserIdsByRangeFromZSet(zSetKey, 0, 3);
		List<RankRedis> ranks = redisRepository.findRanksByUserIds(hashKey, sortedUserIds);

		//then -> 랭크 순서도 알맞게 반환되는지 확인
		Assertions.assertThat(ranks)
			.usingElementComparatorIgnoringFields("userId")
			.containsExactly(rank4, rank3, rank2, rank1);
	}

}
