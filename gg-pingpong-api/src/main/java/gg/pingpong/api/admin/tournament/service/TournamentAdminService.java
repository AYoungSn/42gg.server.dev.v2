package gg.pingpong.api.admin.tournament.service;

import static gg.data.match.type.TournamentMatchStatus.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.data.game.Game;
import gg.data.game.Team;
import gg.data.game.TeamUser;
import gg.data.game.type.StatusType;
import gg.data.manage.SlotManagement;
import gg.data.match.type.TournamentMatchStatus;
import gg.data.tournament.Tournament;
import gg.data.tournament.TournamentGame;
import gg.data.tournament.TournamentUser;
import gg.data.tournament.type.TournamentRound;
import gg.data.tournament.type.TournamentStatus;
import gg.data.user.User;
import gg.pingpong.api.admin.tournament.controller.request.TournamentAdminAddUserRequestDto;
import gg.pingpong.api.admin.tournament.controller.request.TournamentAdminCreateRequestDto;
import gg.pingpong.api.admin.tournament.controller.request.TournamentAdminUpdateRequestDto;
import gg.pingpong.api.admin.tournament.controller.request.TournamentGameUpdateRequestDto;
import gg.pingpong.api.admin.tournament.controller.response.TournamentAdminAddUserResponseDto;
import gg.pingpong.api.global.config.ConstantConfig;
import gg.pingpong.api.user.game.service.GameService;
import gg.pingpong.api.user.match.service.MatchTournamentService;
import gg.pingpong.api.user.tournament.controller.response.TournamentUserListResponseDto;
import gg.repo.game.GameRepository;
import gg.repo.manage.SlotManagementRepository;
import gg.repo.tournarment.TournamentGameRepository;
import gg.repo.tournarment.TournamentRepository;
import gg.repo.tournarment.TournamentUserRepository;
import gg.repo.user.UserRepository;
import gg.utils.exception.ErrorCode;
import gg.utils.exception.custom.InvalidParameterException;
import gg.utils.exception.game.ScoreNotInvalidException;
import gg.utils.exception.match.SlotNotFoundException;
import gg.utils.exception.tournament.TournamentConflictException;
import gg.utils.exception.tournament.TournamentGameNotFoundException;
import gg.utils.exception.tournament.TournamentNotFoundException;
import gg.utils.exception.tournament.TournamentUpdateException;
import gg.utils.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TournamentAdminService {
	private final TournamentRepository tournamentRepository;
	private final TournamentUserRepository tournamentUserRepository;
	private final UserRepository userRepository;
	private final GameRepository gameRepository;
	private final TournamentGameRepository tournamentGameRepository;
	private final SlotManagementRepository slotManagementRepository;
	private final MatchTournamentService matchTournamentService;
	private final ConstantConfig constantConfig;
	private final GameService gameService;

	/***
	 * 토너먼트 생성 Method
	 * @param tournamentAdminCreateRequestDto 토너먼트 생성에 필요한 데이터
	 * @throws TournamentConflictException 토너먼트의 제목이 겹칠 때
	 * @throws TournamentUpdateException 토너먼트 시간으로 부적합 할 때
	 * @throws TournamentConflictException 업데이트 하고자 하는 토너먼트의 시간이 겹칠 때 && 게임 존재할 때
	 * @return 새로 생성된 tournament
	 */
	@Transactional
	public void createTournament(TournamentAdminCreateRequestDto tournamentAdminCreateRequestDto) {
		checkValidTournamentTime(tournamentAdminCreateRequestDto.getStartTime(),
			tournamentAdminCreateRequestDto.getEndTime());
		checkConflictedTournament(-1L, tournamentAdminCreateRequestDto.getStartTime(),
			tournamentAdminCreateRequestDto.getEndTime());
		checkGameExistence(tournamentAdminCreateRequestDto.getStartTime(),
			tournamentAdminCreateRequestDto.getEndTime());

		Tournament tournament = Tournament.builder()
			.title(tournamentAdminCreateRequestDto.getTitle())
			.contents(tournamentAdminCreateRequestDto.getContents())
			.startTime(tournamentAdminCreateRequestDto.getStartTime())
			.endTime(tournamentAdminCreateRequestDto.getEndTime())
			.type(tournamentAdminCreateRequestDto.getType())
			.status(TournamentStatus.BEFORE).build();
		createTournamentGameList(tournament, 7);
		tournamentRepository.save(tournament);
	}

	/**
	 * <p>토너먼트 업데이트 Method</p>
	 * @param tournamentId 업데이트할 토너먼트 id
	 * @param requestDto   요청한 Dto
	 * @throws TournamentNotFoundException 찾을 수 없는 토너먼트 일 때
	 * @throws TournamentUpdateException   업데이트 할 수 없는 토너먼트 일 때, 변경할 토너먼트 시간이 부적합 할 때
	 * @throws TournamentConflictException 업데이트 하고자 하는 토너먼트의 시간이 겹칠 때 && 게임 존재할 때
	 */
	@Transactional
	public Tournament updateTournamentInfo(Long tournamentId, TournamentAdminUpdateRequestDto requestDto) {
		Tournament targetTournament = tournamentRepository.findById(tournamentId)
			.orElseThrow(TournamentNotFoundException::new);
		if (!targetTournament.getStatus().equals(TournamentStatus.BEFORE)) {
			throw new TournamentUpdateException();
		}
		checkValidTournamentTime(requestDto.getStartTime(), requestDto.getEndTime());
		checkConflictedTournament(targetTournament.getId(), requestDto.getStartTime(), requestDto.getEndTime());
		checkGameExistence(requestDto.getStartTime(), requestDto.getEndTime());

		targetTournament.update(
			requestDto.getTitle(),
			requestDto.getContents(),
			requestDto.getStartTime(),
			requestDto.getEndTime(),
			requestDto.getType(),
			TournamentStatus.BEFORE);
		return tournamentRepository.save(targetTournament);
	}

	/**
	 * <p>토너먼트 삭제 매서드</p>
	 * <p>토너먼트는 BEFORE 인 경우에만 삭제 가능하다.</p>
	 * @param tournamentId 타겟 토너먼트 id
	 * @throws TournamentNotFoundException 찾을 수 없는 토너먼트 일 때
	 * @throws TournamentUpdateException   업데이트 할 수 없는 토너먼트 일 때
	 */
	@Transactional
	public void deleteTournament(Long tournamentId) {
		Tournament targetTournament = tournamentRepository.findById(tournamentId)
			.orElseThrow(TournamentNotFoundException::new);
		if (!targetTournament.getStatus().equals(TournamentStatus.BEFORE)) {
			throw new TournamentUpdateException();
		}
		tournamentRepository.deleteById(tournamentId);
	}

	/**
	 * <p>관리자 토너먼트 참가 유저 추가 매서드</p>
	 * <p>해당 토너먼트에 이미 신청 되어 있으면 추가 불가</p>
	 * @param tournamentId 타겟 토너먼트
	 * @param requestDto   요청 dto
	 * @return TournamentAdminAddUserResponseDto dto 반환
	 * @throws TournamentNotFoundException 타겟 토너먼트 없음
	 * @throws TournamentUpdateException   이미 시작했거나 종료된 토너먼트
	 * @throws UserNotFoundException       유저 없음
	 * @throws TournamentConflictException 이미 해당 토너먼트 참가중인 유저
	 */
	@Transactional
	public TournamentAdminAddUserResponseDto addTournamentUser(Long tournamentId,
		TournamentAdminAddUserRequestDto requestDto) {
		Tournament targetTournament = tournamentRepository.findById(tournamentId)
			.orElseThrow(TournamentNotFoundException::new);
		if (!targetTournament.getStatus().equals(TournamentStatus.BEFORE)) {
			throw new TournamentUpdateException();
		}

		User targetUser = userRepository.findByIntraId(requestDto.getIntraId()).orElseThrow(UserNotFoundException::new);

		List<TournamentUser> tournamentList = targetTournament.getTournamentUsers();
		tournamentList.stream().filter(tu -> tu.getUser().getIntraId().equals(targetUser.getIntraId()))
			.findAny()
			.ifPresent(a -> {
				throw new TournamentConflictException(ErrorCode.TOURNAMENT_ALREADY_PARTICIPANT);
			});

		TournamentUser tournamentUser = new TournamentUser(targetUser, targetTournament,
			tournamentList.size() < Tournament.ALLOWED_JOINED_NUMBER, LocalDateTime.now());
		tournamentUserRepository.save(tournamentUser);

		return new TournamentAdminAddUserResponseDto(
			targetUser.getId(),
			targetUser.getIntraId(),
			tournamentUser.getIsJoined()
		);
	}

	/**
	 * <p>토너먼트 유저 삭제 매서드</p>
	 * <p>삭제하고자 하는 유저가 참가자이고, 현재 대기자가 있다면 참가신청이 빠른 대기자를 참가자로 변경해준다.</p>
	 * @param tournamentId 타겟 토너먼트 id
	 * @param userId 타겟 유저 id
	 * @throws TournamentNotFoundException 타겟 토너먼트 없음
	 * @throws TournamentUpdateException   이미 시작했거나 종료된 토너먼트
	 * @throws UserNotFoundException       유저 없음 || 토너먼트 신청자가 아님
	 */
	@Transactional
	public void deleteTournamentUser(Long tournamentId, Long userId) {
		Tournament targetTournament = tournamentRepository.findById(tournamentId)
			.orElseThrow(TournamentNotFoundException::new);
		if (!targetTournament.getStatus().equals(TournamentStatus.BEFORE)) {
			throw new TournamentUpdateException();
		}
		User targetUser = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

		List<TournamentUser> tournamentUserList = targetTournament.getTournamentUsers();
		TournamentUser targetTournamentUser = tournamentUserList.stream()
			.filter(tu -> tu.getUser().getId().equals(targetUser.getId()))
			.findAny()
			.orElseThrow(() -> new TournamentNotFoundException(ErrorCode.TOURNAMENT_NOT_PARTICIPANT));
		targetTournamentUser.deleteTournament();
		if (targetTournamentUser.getIsJoined() && tournamentUserList.size() >= Tournament.ALLOWED_JOINED_NUMBER) {
			tournamentUserList.get(Tournament.ALLOWED_JOINED_NUMBER - 1).updateIsJoined(true);
		}
		tournamentUserRepository.delete(targetTournamentUser);
	}

	/***
	 * 토너먼트 게임 테이블 생성 Method
	 * @param tournament 토너먼트 게임에 매칭될 토너먼트
	 * @param cnt 토너먼트 전체 라운드 수
	 */
	private void createTournamentGameList(Tournament tournament, int cnt) {
		TournamentRound[] rounds = TournamentRound.values();
		while (--cnt >= 0) {
			TournamentGame tournamentGame = new TournamentGame(null, tournament, rounds[cnt]);
		}
	}

	/**
	 * 토너먼트 시간 체크 :
	 * [ 현재 시간 + 최소 2일 ],
	 * [ 현재시간 보다 미래 ],
	 * [ 시작 시간이 종료시간보다 현재시에 가까움 ]
	 * [ 진행 시간 최소 2시간 ]
	 *
	 * @param startTime 업데이트할 토너먼트 시작 시간
	 * @param endTime   업데이트할 토너먼트 종료 시간
	 * @throws InvalidParameterException 토너먼트 시간으로 부적합 할 때
	 */
	private void checkValidTournamentTime(LocalDateTime startTime, LocalDateTime endTime) {
		SlotManagement slotManagement = slotManagementRepository.findCurrent(startTime)
			.orElseThrow(SlotNotFoundException::new);
		int interval = slotManagement.getGameInterval();

		if (startTime.isAfter(endTime)
			|| startTime.isEqual(endTime)
			|| LocalDate.now().plusDays(constantConfig.getAllowedMinimalStartDays()).isAfter(startTime.toLocalDate())
			|| startTime.plusHours(Tournament.MINIMUM_TOURNAMENT_DURATION).isAfter(endTime)
			|| startTime.getMinute() % interval != 0
			|| endTime.getMinute() % interval != 0) {
			throw new TournamentUpdateException(ErrorCode.TOURNAMENT_INVALID_TIME);
		}
	}

	/**
	 * <p>tournamentList 에서 targetTournament을 제외한 토너먼트 중 겹치는 시간대 존재 유무 확인</p>
	 * @param targetTournamentId 업데이트할 토너먼트 id
	 * @param startTime          업데이트할 토너먼트 시작 시간
	 * @param endTime            업데이트할 토너먼트 종료 시간
	 * @throws TournamentConflictException 업데이트 하고자 하는 토너먼트의 시간이 겹칠 때
	 */
	private void checkConflictedTournament(Long targetTournamentId, LocalDateTime startTime, LocalDateTime endTime) {
		List<Tournament> tournamentList = tournamentRepository.findAllBetween(startTime, endTime);
		for (Tournament tournament : tournamentList) {
			if (targetTournamentId.equals(tournament.getId())
				|| (!tournament.getStatus().equals(TournamentStatus.BEFORE)
				&& !tournament.getStatus()
				.equals(TournamentStatus.LIVE))) {
				continue;
			}
			throw new TournamentConflictException();
		}
	}

	/**
	 * <p>타겟 시간 내에 게임이 존재하는지 체크</p>
	 * @param startTime 시작 시간
	 * @param endTime 종료 시간
	 */
	private void checkGameExistence(LocalDateTime startTime, LocalDateTime endTime) {
		gameRepository.findAllBetweenTournament(startTime, endTime).stream()
			.findAny()
			.ifPresent(a -> {
				throw new TournamentConflictException(ErrorCode.TOURNAMENT_CONFLICT_GAME);
			});
	}

	/**
	 * <p>토너먼트 유저 리스트 조회</p>
	 * @param tournamentId 토너먼트 id
	 * @param isJoined     참가자인지 대기자인지 여부
	 * @return TournamentUserListResponseDto
	 * @throws TournamentNotFoundException 토너먼트가 존재하지 않을 때
	 */
	public TournamentUserListResponseDto getTournamentUserList(Long tournamentId, Boolean isJoined) {
		Tournament tournament = tournamentRepository.findById(tournamentId)
			.orElseThrow(TournamentNotFoundException::new);
		if (isJoined == null) {
			return new TournamentUserListResponseDto(tournamentUserRepository.findAllByTournament(tournament));
		} else {
			return new TournamentUserListResponseDto(
				tournamentUserRepository.findAllByTournamentAndIsJoined(tournament, isJoined));
		}
	}

	/**
	 * <p>토너먼트 게임 정보 수정</p>
	 * @param tournamentId 타겟 토너먼트 id
	 * @param reqDto 수정할 게임 정보
	 * @throws TournamentUpdateException 토너먼트가 시작되지 않았을 때
	 */
	@Transactional
	public void updateTournamentGame(Long tournamentId, TournamentGameUpdateRequestDto reqDto) {
		if (reqDto.getTeam1().getScore() + reqDto.getTeam2().getScore() > 3
			|| reqDto.getTeam1().getScore() + reqDto.getTeam2().getScore() < 2
			|| reqDto.getTeam1().getScore() == reqDto.getTeam2().getScore()) {
			throw new ScoreNotInvalidException();
		}
		Tournament tournament = tournamentRepository.findById(tournamentId)
			.orElseThrow(TournamentNotFoundException::new);
		if (tournament.getStatus() == TournamentStatus.BEFORE) {
			throw new TournamentUpdateException(ErrorCode.TOURNAMENT_IS_BEFORE);
		}
		TournamentGame tournamentGame = tournament.getTournamentGames().stream()
			.filter(t -> t.getId().equals(reqDto.getTournamentGameId())).findAny()
			.orElseThrow(TournamentGameNotFoundException::new);
		Game game = tournamentGame.getGame();
		if (game == null) {
			throw new TournamentGameNotFoundException();
		}
		if (!canUpdateScore(tournamentGame, reqDto)) {
			throw new TournamentUpdateException(ErrorCode.TOURNAMENT_INVALID_SCORE);
		}
		updateTeamScore(game, reqDto);
		TournamentMatchStatus matchStatus = matchTournamentService.checkTournamentGame(game);
		TournamentRound nextRound = tournamentGame.getTournamentRound().getNextRound();
		List<TeamUser> teamUsers = new ArrayList<>();
		for (Team team : game.getTeams()) {
			teamUsers.add(team.getTeamUsers().get(0));
		}
		gameService.savePChange(game, teamUsers, teamUsers.get(0).getUser().getId());
		if (REQUIRED.equals(matchStatus)) {
			matchTournamentService.matchGames(tournament, nextRound.getRoundNumber());
		} else if (ALREADY_MATCHED.equals(matchStatus)) {
			Game nextMatchedGame = tournamentGameRepository.findByTournamentIdAndTournamentRound(tournament.getId(),
					nextRound)
				.orElseThrow(TournamentGameNotFoundException::new)
				.getGame();
			matchTournamentService.updateMatchedGameUser(game, nextMatchedGame);
		}
	}

	/**
	 * <p>토너먼트 게임 점수 수정</p>
	 * @param game 수정될 게임
	 * @param reqDto 수정할 게임 정보
	 */
	private void updateTeamScore(Game game, TournamentGameUpdateRequestDto reqDto) {

		List<Team> teams = game.getTeams();
		Team team1 = teams.stream()
			.filter(t -> t.getId().equals(reqDto.getTeam1().getTeamId()))
			.findAny()
			.orElseThrow(TournamentGameNotFoundException::new);
		Team team2 = teams.stream()
			.filter(t -> t.getId().equals(reqDto.getTeam2().getTeamId()))
			.findAny()
			.orElseThrow(TournamentGameNotFoundException::new);
		team1.updateScore(reqDto.getTeam1().getScore(), reqDto.getTeam1().getScore() > reqDto.getTeam2().getScore());
		team2.updateScore(reqDto.getTeam2().getScore(), reqDto.getTeam2().getScore() > reqDto.getTeam1().getScore());
		if (game.getStatus() == StatusType.LIVE) {
			game.updateStatus();
		}
		game.updateStatus();
	}

	/**
	 * <p>토너먼트 게임 점수 수정 가능 여부</p>
	 * @param reqDto 수정할 게임 정보
	 * @return 수정 가능 여부
	 */
	private boolean canUpdateScore(TournamentGame tournamentGame, TournamentGameUpdateRequestDto reqDto) {
		if (tournamentGame.getGame().getStatus() == StatusType.BEFORE) {
			return false;
		}
		if (reqDto.getNextTournamentGameId() == null) {
			return tournamentGame.getTournamentRound() == TournamentRound.THE_FINAL;
		}
		TournamentGame nextTournamentGame = tournamentGameRepository.findById(reqDto.getNextTournamentGameId())
			.orElseThrow(TournamentGameNotFoundException::new);
		if (nextTournamentGame.getGame() == null) {
			return true;
		}
		if (nextTournamentGame.getGame().getStatus() == StatusType.BEFORE) {
			return true;
		}
		return false;
	}
}
