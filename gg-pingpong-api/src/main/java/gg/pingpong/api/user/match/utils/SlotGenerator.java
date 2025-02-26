package gg.pingpong.api.user.match.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import gg.data.game.Game;
import gg.data.game.type.Mode;
import gg.data.manage.SlotManagement;
import gg.data.match.RedisMatchTime;
import gg.data.match.RedisMatchUser;
import gg.data.match.type.Option;
import gg.data.match.type.SlotStatus;
import gg.data.rank.redis.RankRedis;
import gg.data.season.Season;
import gg.pingpong.api.user.match.controller.response.SlotStatusResponseListDto;
import gg.pingpong.api.user.match.dto.SlotStatusDto;
import lombok.Getter;

@Getter
public class SlotGenerator {
	/**
	 * minTime ~ maxTime : 슬롯이 보여지는 시간 범위
	 * matchUser : 현재 유저 정보
	 * matchCalculator : 매칭 관련 슬롯 상태 결정
	 * option : 유저가 현재 선택한 mode(random, normal, both)
	 */
	private final HashMap<LocalDateTime, SlotStatusDto> slots;
	private final Integer interval;
	private final LocalDateTime minTime;
	private final LocalDateTime now;
	private final LocalDateTime maxTime;
	private final RedisMatchUser matchUser;
	private final MatchCalculator matchCalculator;
	private final Option option;

	public SlotGenerator(RankRedis user, SlotManagement slotManagement, Season season, Option option) {
		this.interval = slotManagement.getGameInterval();
		this.now = LocalDateTime.now();
		this.minTime = LocalDateTime.of(
				now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(), 0)
			.minusHours(slotManagement.getPastSlotTime());
		this.maxTime = setMaxTime(slotManagement);
		this.option = option;
		this.slots = new HashMap<LocalDateTime, SlotStatusDto>();
		this.matchUser = new RedisMatchUser(user.getUserId(), user.getPpp(), option);
		this.matchCalculator = new MatchCalculator(season.getPppGap(), matchUser);
	}

	public void addPastSlots() {
		for (LocalDateTime time = minTime; time.isBefore(now); time = time.plusMinutes(interval)) {
			slots.put(time, new SlotStatusDto(time, SlotStatus.CLOSE, interval));
		}
	}

	public void addMatchedSlots(List<Game> games) {
		games.stream().forEach(e -> slots.put(e.getStartTime(),
			new SlotStatusDto(e.getStartTime(), SlotStatus.CLOSE, interval)));
	}

	public void addMySlots(Game myGame) {
		slots.put(myGame.getStartTime(),
			new SlotStatusDto(myGame.getStartTime(), myGame.getEndTime(),
				getMySlotStatus(myGame.getMode(), option)));
	}

	public void addMySlots(Set<RedisMatchTime> allMatchTime) {
		allMatchTime.stream().forEach(match -> slots.put(match.getStartTime(),
			new SlotStatusDto(match.getStartTime(),
				getMySlotStatus(match.getOption(), option), interval)));
	}

	private SlotStatus getMySlotStatus(Option myOption, Option viewOption) {
		if (myOption.equals(viewOption)) {
			return SlotStatus.MYTABLE;
		}
		return SlotStatus.CLOSE;
	}

	private SlotStatus getMySlotStatus(Mode myMode, Option viewOption) {
		if (myMode.getCode().equals(viewOption.getCode())) {
			return SlotStatus.MYTABLE;
		}
		return SlotStatus.CLOSE;
	}

	public void groupEnrolledSlot(LocalDateTime startTime, List<RedisMatchUser> players) {
		slots.put(startTime, new SlotStatusDto(startTime, matchCalculator.findEnemyStatus(players), interval));
	}

	public SlotStatusResponseListDto getResponseListDto() {
		long slotCountPerHour = 60 / interval;
		List<List<SlotStatusDto>> matchBoards = new ArrayList<List<SlotStatusDto>>();
		for (LocalDateTime time = minTime; time.isBefore(maxTime); time = time.plusHours(1)) {
			List<SlotStatusDto> hourBoard = new ArrayList<SlotStatusDto>();
			for (long i = 0; i < slotCountPerHour; i++) {
				SlotStatusDto dto = slots.getOrDefault(time.plusMinutes(i * interval),
					new SlotStatusDto(time.plusMinutes(i * interval), SlotStatus.OPEN, interval));
				hourBoard.add(dto);
			}
			matchBoards.add(hourBoard);
		}
		return new SlotStatusResponseListDto(matchBoards);
	}

	private LocalDateTime setMaxTime(SlotManagement slotManagement) {
		LocalDateTime compared = LocalDateTime.of(
				now.getYear(), now.getMonth(), now.getDayOfMonth(), now.getHour(), 0)
			.plusHours(slotManagement.getFutureSlotTime());
		if (slotManagement.getEndTime() != null && slotManagement.getEndTime().isBefore(compared)) {
			return slotManagement.getEndTime();
		}
		return compared;
	}
}
