package gg.pingpong.api.user.rank.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.data.rank.redis.RankRedis;
import gg.data.season.Season;
import gg.data.user.User;
import gg.pingpong.api.user.rank.controller.response.ExpRankPageResponseDto;
import gg.pingpong.api.user.rank.controller.response.RankPageResponseDto;
import gg.pingpong.api.user.rank.dto.ExpRankDto;
import gg.pingpong.api.user.rank.dto.RankDto;
import gg.pingpong.api.user.season.service.SeasonFindService;
import gg.pingpong.api.user.user.dto.UserDto;
import gg.repo.rank.RankRepository;
import gg.repo.rank.redis.RankRedisRepository;
import gg.repo.user.ExpRankV2Dto;
import gg.repo.user.UserRepository;
import gg.utils.RedisKeyManager;
import gg.utils.exception.ErrorCode;
import gg.utils.exception.custom.PageNotFoundException;
import gg.utils.exception.rank.RedisDataNotFoundException;
import gg.utils.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankService {
	private final UserRepository userRepository;
	private final RankRedisRepository redisRepository;
	private final SeasonFindService seasonFindService;
	private final RankRepository rankRepository;

	@Transactional(readOnly = true)
	public ExpRankPageResponseDto getExpRankPageByRedis(PageRequest pageRequest, UserDto curUser) {

		Long myRank = curUser.getTotalExp() == 0 ? -1 : userRepository.findExpRankingByIntraId(curUser.getIntraId());
		Page<User> users = userRepository.findAllByTotalExpGreaterThan(pageRequest, 0);
		if (pageRequest.getPageNumber() + 1 > users.getTotalPages()) {
			throw new PageNotFoundException("페이지가 존재하지 않습니다.", ErrorCode.PAGE_NOT_FOUND);
		}

		List<Long> userIds = users.getContent().stream().map(user -> user.getId()).collect(Collectors.toList());
		Season curSeason = seasonFindService.findCurrentSeason(LocalDateTime.now());

		String hashKey = RedisKeyManager.getHashKey(curSeason.getId());
		List<RankRedis> ranks = redisRepository.findRanksByUserIds(hashKey, userIds);

		Integer startRank = pageRequest.getPageSize() * pageRequest.getPageNumber() + 1;
		List<ExpRankDto> expRankDtos = new ArrayList<>();
		for (int i = 0; i < ranks.size(); i++) {
			RankRedis rank = ranks.get(i);
			User user = users.getContent().get(i);
			expRankDtos.add(ExpRankDto.from(user, startRank + i, rank.getStatusMessage()));
		}

		return new ExpRankPageResponseDto(myRank.intValue(), pageRequest.getPageNumber() + 1, users.getTotalPages(),
			expRankDtos);
	}

	/**
	 * ExpRankPage v2 (redis 조회 제거 - db 조회로만 하는 기능)
	 * @param pageRequest
	 * @param curUser
	 * @return
	 */
	@Transactional(readOnly = true)
	public ExpRankPageResponseDto getExpRankPage(PageRequest pageRequest, UserDto curUser) {

		Long myRank = curUser.getTotalExp() == 0 ? -1 : userRepository.findExpRankingByIntraId(curUser.getIntraId());
		Page<User> users = userRepository.findAllByTotalExpGreaterThan(pageRequest, 0);
		if (pageRequest.getPageNumber() + 1 > users.getTotalPages()) {
			throw new PageNotFoundException();
		}
		List<ExpRankDto> expRankDtos = getExpRankList(pageRequest);

		return new ExpRankPageResponseDto(myRank.intValue(),
			pageRequest.getPageNumber() + 1,
			users.getTotalPages(),
			expRankDtos);
	}

	@Cacheable(value = "expRanking", cacheManager = "gameCacheManager",
		key = "#pageRequest.pageNumber + #pageRequest.pageSize.toString()")
	public List<ExpRankDto> getExpRankList(PageRequest pageRequest) {
		Season curSeason = seasonFindService.findCurrentSeason(LocalDateTime.now());
		int pageOffset = pageRequest.getPageNumber() * pageRequest.getPageSize();
		List<ExpRankV2Dto> expRankV2Dtos = userRepository.findExpRank(pageOffset, pageRequest.getPageSize(),
			curSeason.getId());
		return expRankV2Dtos.stream().map(ExpRankDto::from).collect(Collectors.toList());
	}

	/**
	 * rank 페이지 조회 v2
	 * @param pageRequest
	 * @param curUser
	 * @param seasonId
	 * @return
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = "ranking", cacheManager = "gameCacheManager",
		key = "#pageRequest.pageSize.toString() + #pageRequest.pageNumber + #curUser.id + #seasonId")
	public RankPageResponseDto getRankPageV2(PageRequest pageRequest, UserDto curUser, Long seasonId) {
		Season season;
		if (seasonId == null || seasonId == 0) {
			season = seasonFindService.findCurrentSeason(LocalDateTime.now());
		} else {
			season = seasonFindService.findSeasonById(seasonId);
		}
		int totalPage = calcTotalPageV2(season, pageRequest.getPageSize());
		if (totalPage == 0) {
			return returnEmptyRankPage();
		}
		if (pageRequest.getPageNumber() + 1 > totalPage) {
			throw new PageNotFoundException();
		}

		int myRank = rankRepository.findRankByUserIdAndSeasonId(curUser.getId(), season.getId())
			.orElse(-1);
		int pageOffset = pageRequest.getPageNumber() * pageRequest.getPageSize();
		List<RankDto> rankList = rankRepository.findPppRankBySeasonId(pageOffset, pageRequest.getPageSize(),
				season.getId())
			.stream().map(RankDto::from).collect(Collectors.toList());
		return new RankPageResponseDto(myRank, pageRequest.getPageNumber() + 1, totalPage, rankList);
	}

	@Transactional(readOnly = true)
	public RankPageResponseDto getRankPage(PageRequest pageRequest, UserDto curUser, Long seasonId) {
		Season season;
		if (seasonId == null || seasonId == 0) {
			season = seasonFindService.findCurrentSeason(LocalDateTime.now());
		} else {
			season = seasonFindService.findSeasonById(seasonId);
		}
		int totalPage = calcTotalPage(season, pageRequest.getPageSize());
		if (totalPage == 0) {
			return returnEmptyRankPage();
		}
		if (pageRequest.getPageNumber() + 1 > totalPage) {
			throw new PageNotFoundException("페이지가 존재하지 않습니다.", ErrorCode.PAGE_NOT_FOUND);
		}

		int myRank = findMyRank(curUser, season);
		int startRank = pageRequest.getPageNumber() * pageRequest.getPageSize();
		int endRank = startRank + pageRequest.getPageSize() - 1;
		List<RankDto> rankList = createRankList(startRank, endRank, season);
		return new RankPageResponseDto(myRank, pageRequest.getPageNumber() + 1, totalPage, rankList);
	}

	private RankPageResponseDto returnEmptyRankPage() {
		return new RankPageResponseDto(-1, 1, 1, new ArrayList<>());
	}

	private int findMyRank(UserDto curUser, Season season) {
		String zSetKey = RedisKeyManager.getZSetKey(season.getId());
		try {
			Long myRank = redisRepository.getRankInZSet(zSetKey, curUser.getId());
			return myRank.intValue() + 1;
		} catch (RedisDataNotFoundException e) {
			return -1;
		}
	}

	private int calcTotalPage(Season season, int pageSize) {
		String zSetKey = RedisKeyManager.getZSetKey(season.getId());
		try {
			Long totalUserCount = redisRepository.countTotalRank(zSetKey);
			return (int)Math.ceil((double)totalUserCount / pageSize);
		} catch (RedisDataNotFoundException e) {
			return 0;
		}
	}

	private int calcTotalPageV2(Season season, int pageSize) {
		try {
			Integer totalUserCount = rankRepository.countRankUserBySeasonId(season.getId());
			return (int)Math.ceil((double)totalUserCount / pageSize);
		} catch (RedisDataNotFoundException e) {
			return 0;
		}
	}

	private List<RankDto> createRankList(int startRank, int endRank, Season season) {
		String zSetKey = RedisKeyManager.getZSetKey(season.getId());
		String hashKey = RedisKeyManager.getHashKey(season.getId());

		List<Long> userIds = redisRepository.getUserIdsByRangeFromZSet(zSetKey, startRank, endRank);
		List<RankRedis> userRanks = redisRepository.findRanksByUserIds(hashKey, userIds);
		List<RankDto> rankList = new ArrayList<>();

		for (int i = 0; i < userRanks.size(); i++) {
			User user = userRepository.findById(userIds.get(i)).orElseThrow(UserNotFoundException::new);
			rankList.add(RankDto.from(user, userRanks.get(i), ++startRank));
		}
		return rankList;
	}
}
