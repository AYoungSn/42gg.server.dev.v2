package gg.pingpong.api.admin.tournament.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gg.data.game.Game;
import gg.data.manage.SlotManagement;
import gg.data.tournament.Tournament;
import gg.data.tournament.TournamentGame;
import gg.data.tournament.TournamentUser;
import gg.data.tournament.type.TournamentRound;
import gg.data.tournament.type.TournamentStatus;
import gg.data.tournament.type.TournamentType;
import gg.data.user.User;
import gg.data.user.type.RacketType;
import gg.data.user.type.RoleType;
import gg.data.user.type.SnsType;
import gg.pingpong.api.admin.tournament.controller.request.TournamentAdminAddUserRequestDto;
import gg.pingpong.api.admin.tournament.controller.request.TournamentAdminCreateRequestDto;
import gg.pingpong.api.admin.tournament.controller.request.TournamentAdminUpdateRequestDto;
import gg.pingpong.api.global.config.ConstantConfig;
import gg.pingpong.api.utils.ReflectionUtilsForUnitTest;
import gg.repo.game.GameRepository;
import gg.repo.manage.SlotManagementRepository;
import gg.repo.tournarment.TournamentGameRepository;
import gg.repo.tournarment.TournamentRepository;
import gg.repo.tournarment.TournamentUserRepository;
import gg.repo.user.UserRepository;
import gg.utils.annotation.UnitTest;
import gg.utils.exception.tournament.TournamentConflictException;
import gg.utils.exception.tournament.TournamentNotFoundException;
import gg.utils.exception.tournament.TournamentUpdateException;
import gg.utils.exception.user.UserNotFoundException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TournamentAdminServiceTest {
	@Mock
	TournamentRepository tournamentRepository;
	@Mock
	TournamentGameRepository tournamentGameRepository;
	@Mock
	TournamentUserRepository tournamentUserRepository;
	@Mock
	SlotManagementRepository slotManagementRepository;
	@Mock
	UserRepository userRepository;
	@Mock
	GameRepository gameRepository;
	@Mock
	ConstantConfig constantConfig;
	@InjectMocks
	TournamentAdminService tournamentAdminService;

	// 토너먼트 생성 서비스 테스트
	@Nested
	@DisplayName("토너먼트 관리자 생성 서비스 테스트")
	class TournamentAdminServiceCreateTest {
		@Test
		@DisplayName("토너먼트 생성 성공")
		void success() {
			// given
			TournamentAdminCreateRequestDto requestDto = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(3, 14, 0), getTargetTime(3, 16, 0));
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 10, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentGame tournamentGame = createTournamentGame(tournament, TournamentRound.THE_FINAL);

			given(slotManagementRepository.findCurrent(requestDto.getStartTime())).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findAllBetween(requestDto.getStartTime(), requestDto.getEndTime()))
				.willReturn(new ArrayList<>());
			given(gameRepository.findAllBetweenTournament(requestDto.getStartTime(), requestDto.getEndTime()))
				.willReturn(new ArrayList<>());
			given(tournamentRepository.save(any(Tournament.class))).willReturn(tournament);

			// when
			tournamentAdminService.createTournament(requestDto);
		}

		@Test
		@DisplayName("유효하지 않은 시간 입력")
		public void invalidTime() {
			when(constantConfig.getAllowedMinimalStartDays()).thenReturn(2);
			//given
			Tournament tournament = createTournament(1L, TournamentStatus.BEFORE,
				getTargetTime(0, 0, 0), getTargetTime(0, 1, 0));

			TournamentAdminCreateRequestDto requestDto1 = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(1, 1, 0), getTargetTime(1, 3, 0));
			TournamentAdminCreateRequestDto requestDto2 = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(3, 3, 0), getTargetTime(3, 1, 0));
			TournamentAdminCreateRequestDto requestDto3 = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(3, 3, 0), getTargetTime(3, 3, 0));
			TournamentAdminCreateRequestDto requestDto4 = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(3, 1, 10), getTargetTime(3, 2, 10));
			given(slotManagementRepository.findCurrent(any(LocalDateTime.class))).willReturn(
				Optional.of(createSlot(15)));
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.createTournament(requestDto1))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(() -> tournamentAdminService.createTournament(requestDto2))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(() -> tournamentAdminService.createTournament(requestDto3))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(() -> tournamentAdminService.createTournament(requestDto4))
				.isInstanceOf(TournamentUpdateException.class);
		}

		@Test
		@DisplayName("기존에 있는 토너먼트와 겹치는 토너먼트 시간")
		public void tournamentTimeConflict() {
			// given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(3, 10, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentAdminCreateRequestDto createRequestDto = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(3, 11, 0), getTargetTime(3, 13, 0));
			given(slotManagementRepository.findCurrent(createRequestDto.getStartTime())).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findAllBetween(createRequestDto.getStartTime(), createRequestDto.getEndTime()))
				.willReturn(tournamentList);

			// when, then
			assertThatThrownBy(() -> tournamentAdminService.createTournament(createRequestDto))
				.isInstanceOf(TournamentConflictException.class);
		}

		@Test
		@DisplayName("기존에 있는 게임과 겹치는 토너먼트 시간")
		void gameAlreadyExist() {
			// given
			TournamentAdminCreateRequestDto tournamentAdminCreateRequestDto = createTournamentCreateRequestDto(
				"1st tournament",
				getTargetTime(3, 10, 0), getTargetTime(3, 12, 0));
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 10, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentGame tournamentGame = createTournamentGame(tournament, TournamentRound.THE_FINAL);
			List<Game> gameList = new ArrayList<>();
			gameList.add(new Game());
			given(slotManagementRepository.findCurrent(tournamentAdminCreateRequestDto.getStartTime())).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findAllBetween(tournamentAdminCreateRequestDto.getStartTime(),
				tournamentAdminCreateRequestDto.getEndTime()))
				.willReturn(new ArrayList<>());
			given(gameRepository.findAllBetweenTournament(tournamentAdminCreateRequestDto.getStartTime(),
				tournamentAdminCreateRequestDto.getEndTime()))
				.willReturn(gameList);

			// when
			assertThatThrownBy(() -> tournamentAdminService.createTournament(tournamentAdminCreateRequestDto))
				.isInstanceOf(TournamentConflictException.class);
			;
		}
	}

	// 토너먼트 수정 서비스 테스트
	@Nested
	@DisplayName("토너먼트 관리자 서비스 수정 테스트")
	class TournamentAdminServiceUpdateTest {
		@Test
		@DisplayName("토너먼트_업데이트_성공")
		public void success() {
			// given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 10, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentAdminUpdateRequestDto updateRequestDto = createTournamentAdminUpdateRequestDto(
				getTargetTime(3, 14, 0), getTargetTime(3, 16, 0));
			given(slotManagementRepository.findCurrent(updateRequestDto.getStartTime())).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			given(tournamentRepository.findAllBetween(updateRequestDto.getStartTime(), updateRequestDto.getEndTime()))
				.willReturn(new ArrayList<>());
			given(
				gameRepository.findAllBetweenTournament(updateRequestDto.getStartTime(), updateRequestDto.getEndTime()))
				.willReturn(new ArrayList<>());
			given(tournamentRepository.save(any(Tournament.class))).willReturn(tournament);
			// when
			Tournament changedTournament = tournamentAdminService.updateTournamentInfo(1L, updateRequestDto);
			// then
			assertThat(changedTournament.getId()).isEqualTo(tournament.getId());
			assertThat(changedTournament.getTitle()).isEqualTo(updateRequestDto.getTitle());
			assertThat(changedTournament.getContents()).isEqualTo(updateRequestDto.getContents());
			assertThat(changedTournament.getStartTime()).isEqualTo(updateRequestDto.getStartTime());
			assertThat(changedTournament.getEndTime()).isEqualTo(updateRequestDto.getEndTime());
			assertThat(changedTournament.getType()).isEqualTo(updateRequestDto.getType());
			assertThat(changedTournament.getStatus()).isEqualTo(tournament.getStatus());
		}

		@Test
		@DisplayName("타겟_토너먼트_없음")
		public void tournamentNotFound() {
			// given
			Tournament tournament = createTournament(1234L, TournamentStatus.BEFORE,
				getTargetTime(2, 1, 0), getTargetTime(2, 3, 0));
			TournamentAdminUpdateRequestDto updateRequestDto = createTournamentAdminUpdateRequestDto(
				getTargetTime(2, 1, 0), getTargetTime(2, 3, 0));

			given(tournamentRepository.findById(tournament.getId())).willReturn(Optional.empty());
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), updateRequestDto))
				.isInstanceOf(TournamentNotFoundException.class);
		}

		@Test
		@DisplayName("토너먼트_업데이트_불가_상태")
		public void canNotUpdate() {
			// given
			Tournament tournamentLive = createTournament(1L, TournamentStatus.LIVE,
				LocalDateTime.now().plusHours(-1).withMinute(0), LocalDateTime.now().plusHours(1).withMinute(0));
			Tournament tournamentEnd = createTournament(2L, TournamentStatus.END,
				LocalDateTime.now().plusHours(-4).withMinute(0), LocalDateTime.now().plusHours(-2).withMinute(0));
			TournamentAdminUpdateRequestDto updateRequestDto = createTournamentAdminUpdateRequestDto(
				getTargetTime(3, 10, 0), getTargetTime(3, 12, 0));
			given(tournamentRepository.findById(tournamentLive.getId())).willReturn(Optional.of(tournamentLive));
			given(tournamentRepository.findById(tournamentEnd.getId())).willReturn(Optional.of(tournamentEnd));
			// when, then
			assertThatThrownBy(
				() -> tournamentAdminService.updateTournamentInfo(tournamentLive.getId(), updateRequestDto))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(
				() -> tournamentAdminService.updateTournamentInfo(tournamentEnd.getId(), updateRequestDto))
				.isInstanceOf(TournamentUpdateException.class);
		}

		@Test
		@DisplayName("업데이트_토너먼트_Dto_Invalid_Time")
		public void dto_Invalid_Time() {
			when(constantConfig.getAllowedMinimalStartDays()).thenReturn(2);
			// given
			Tournament tournament = createTournament(1L, TournamentStatus.BEFORE,
				getTargetTime(2, 1, 0), getTargetTime(2, 3, 0));
			TournamentAdminUpdateRequestDto invalidRequestDto1 = createTournamentAdminUpdateRequestDto(
				getTargetTime(2, 3, 0), getTargetTime(2, 1, 0));
			TournamentAdminUpdateRequestDto invalidRequestDto2 = createTournamentAdminUpdateRequestDto(
				invalidRequestDto1.getStartTime(), invalidRequestDto1.getStartTime());
			TournamentAdminUpdateRequestDto invalidRequestDto3 = createTournamentAdminUpdateRequestDto(
				getTargetTime(1, 23, 0), getTargetTime(2, 1, 0));
			TournamentAdminUpdateRequestDto invalidRequestDto4 = createTournamentAdminUpdateRequestDto(
				getTargetTime(2, 20, 1), getTargetTime(2, 23, 0));

			given(slotManagementRepository.findCurrent(any(LocalDateTime.class))).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			// when then
			assertThatThrownBy(
				() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), invalidRequestDto1))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(
				() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), invalidRequestDto2))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(
				() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), invalidRequestDto3))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(
				() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), invalidRequestDto4))
				.isInstanceOf(TournamentUpdateException.class);
		}

		@Test
		@DisplayName("Dto_기간_토너먼트_기간_겹침")
		public void tournamentTimeConflict() {
			// given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(3, 10, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentAdminUpdateRequestDto updateRequestDto = createTournamentAdminUpdateRequestDto(
				getTargetTime(3, 12, 0), getTargetTime(3, 14, 0));
			given(slotManagementRepository.findCurrent(any(LocalDateTime.class))).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			given(tournamentRepository.findAllBetween(updateRequestDto.getStartTime(), updateRequestDto.getEndTime()))
				.willReturn(tournamentList);
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), updateRequestDto))
				.isInstanceOf(TournamentConflictException.class);
		}

		@Test
		@DisplayName("Dto_기간_게임_겹침")
		public void gameTimeConflict() {
			// given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 10, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentAdminUpdateRequestDto updateRequestDto = createTournamentAdminUpdateRequestDto(
				getTargetTime(3, 1, 0), getTargetTime(3, 3, 0));
			List<Game> gameList = new ArrayList<>();
			gameList.add(new Game());
			given(slotManagementRepository.findCurrent(any(LocalDateTime.class))).willReturn(
				Optional.of(createSlot(15)));
			given(tournamentRepository.findById(tournament.getId())).willReturn(Optional.of(tournament));
			given(tournamentRepository.findAllBetween(updateRequestDto.getStartTime(), updateRequestDto.getEndTime()))
				.willReturn(new ArrayList<>());
			given(gameRepository.findAllBetweenTournament(updateRequestDto.getStartTime(),
				updateRequestDto.getEndTime())).willReturn(gameList);
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.updateTournamentInfo(tournament.getId(), updateRequestDto))
				.isInstanceOf(TournamentConflictException.class);
		}
	}

	// 토너먼트 삭제 서비스 테스트
	@Nested
	@DisplayName("토너먼트 관리자 서비스 삭제 테스트")
	class TournamentAdminServiceDeleteTest {
		@Test
		@DisplayName("토너먼트_삭제_성공")
		void success() {
			// given
			int tournamentGameCnt = 7;
			Tournament tournament = createTournament(1L, TournamentStatus.BEFORE,
				getTargetTime(2, 1, 0), getTargetTime(2, 3, 0));
			List<TournamentGame> tournamentGameList = createTournamentGames(1L, tournament, tournamentGameCnt);
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			// when, then
			tournamentAdminService.deleteTournament(tournament.getId());
		}

		@Test
		@DisplayName("타겟_토너먼트_없음")
		public void tournamentNotFound() {
			// given
			Tournament tournament = createTournament(1L, TournamentStatus.BEFORE,
				getTargetTime(2, 1, 0), getTargetTime(2, 3, 0));
			given(tournamentRepository.findById(1L)).willReturn(Optional.empty());
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.deleteTournament(tournament.getId()))
				.isInstanceOf(TournamentNotFoundException.class);
		}

		@Test
		@DisplayName("토너먼트_삭제_불가_상태")
		public void canNotDelete() {
			// given
			Tournament liveTournament = createTournament(1L, TournamentStatus.LIVE,
				LocalDateTime.now().plusHours(-1).withMinute(0), LocalDateTime.now().plusHours(1).withMinute(0));
			Tournament endTournament = createTournament(1L, TournamentStatus.END,
				LocalDateTime.now().plusHours(-4).withMinute(0), LocalDateTime.now().plusHours(-2).withMinute(0));
			given(tournamentRepository.findById(liveTournament.getId())).willReturn(Optional.of(liveTournament));
			given(tournamentRepository.findById(endTournament.getId())).willReturn(Optional.of(endTournament));
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.deleteTournament(liveTournament.getId()))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(() -> tournamentAdminService.deleteTournament(endTournament.getId()))
				.isInstanceOf(TournamentUpdateException.class);
		}

	}

	@Nested
	@DisplayName("관리자_토너먼트_유저_추가_테스트")
	class TournamentAdminServiceAddUserTest {
		@Test
		@DisplayName("유저_추가_성공")
		public void success() {
			// given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 1, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentAdminAddUserRequestDto requestDto = new TournamentAdminAddUserRequestDto("testUser");
			User user = createUser("testUser");
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			given(userRepository.findByIntraId("testUser")).willReturn(Optional.of(user));
			given(tournamentUserRepository.save(any(TournamentUser.class))).willReturn(null);
			// when, then
			tournamentAdminService.addTournamentUser(1L, requestDto);
		}

		@Test
		@DisplayName("타겟_토너먼트_없음")
		public void tournamentNotFound() {
			// given
			TournamentAdminAddUserRequestDto requestDto = new TournamentAdminAddUserRequestDto("test");

			given(tournamentRepository.findById(any(Long.class))).willReturn(Optional.empty());
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.addTournamentUser(1L, requestDto))
				.isInstanceOf(TournamentNotFoundException.class);
		}

		@Test
		@DisplayName("토너먼트_업데이트_불가_상태")
		public void canNotAdd() {
			// given
			Tournament tournamentLive = createTournament(1L, TournamentStatus.LIVE,
				LocalDateTime.now().plusHours(-1).withMinute(0), LocalDateTime.now().plusHours(1).withMinute(0));
			Tournament tournamentEnd = createTournament(2L, TournamentStatus.END,
				LocalDateTime.now().plusHours(-4).withMinute(0), LocalDateTime.now().plusHours(-2).withMinute(0));
			TournamentAdminAddUserRequestDto requestDto = new TournamentAdminAddUserRequestDto("test");
			given(tournamentRepository.findById(tournamentLive.getId())).willReturn(Optional.of(tournamentLive));
			given(tournamentRepository.findById(tournamentEnd.getId())).willReturn(Optional.of(tournamentEnd));
			// when, then
			assertThatThrownBy(() -> tournamentAdminService.addTournamentUser(tournamentLive.getId(), requestDto))
				.isInstanceOf(TournamentUpdateException.class);
			assertThatThrownBy(() -> tournamentAdminService.addTournamentUser(tournamentEnd.getId(), requestDto))
				.isInstanceOf(TournamentUpdateException.class);
		}

		@Test
		@DisplayName("찾을_수_없는_유저")
		public void userNotFound() {
			// given
			Tournament tournament = createTournament(1L, TournamentStatus.BEFORE,
				getTargetTime(0, 14, 0), getTargetTime(0, 16, 0));
			TournamentAdminAddUserRequestDto requestDto = new TournamentAdminAddUserRequestDto("test");
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			given(userRepository.findByIntraId("test")).willReturn(Optional.empty());

			// when then
			assertThatThrownBy(() -> tournamentAdminService.addTournamentUser(tournament.getId(), requestDto))
				.isInstanceOf(UserNotFoundException.class);
		}

		@Test
		@DisplayName("이미_해당_토너먼트_참가중인_유저")
		public void alreadyTournamentParticipant() {
			// given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 1, 0));
			Tournament tournament = tournamentList.get(0);
			TournamentAdminAddUserRequestDto requestDto = new TournamentAdminAddUserRequestDto("testUser");
			User user = createUser("testUser");
			TournamentUser tournamentUser = new TournamentUser(user, tournament, true, LocalDateTime.now());
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			given(userRepository.findByIntraId("testUser")).willReturn(Optional.of(user));

			// when, then
			assertThatThrownBy(() -> tournamentAdminService.addTournamentUser(tournament.getId(), requestDto))
				.isInstanceOf(TournamentConflictException.class);
		}
	}

	@Nested
	@DisplayName("관리자_토너먼트_유저_삭제_테스트")
	class TournamentAdminServiceDeleteUserTest {
		@Test
		@DisplayName("찾을_수_없는_유저")
		public void userNotFound() {
			//given
			List<Tournament> tournamentList = createTournaments(1L, 2, getTargetTime(2, 1, 0));
			Tournament tournament = tournamentList.get(0);
			User user = createUser("user");
			given(tournamentRepository.findById(1L)).willReturn(Optional.of(tournament));
			given(userRepository.findById(null)).willReturn(Optional.empty());

			// when, then
			assertThatThrownBy(() -> tournamentAdminService.deleteTournamentUser(tournament.getId(), user.getId()))
				.isInstanceOf(UserNotFoundException.class);
		}
	}

	/**
	 * 토너먼트 생성 requestDto
	 * @param startTime 토너먼트 시작 시간
	 * @param endTime 토너먼트 종료 시간
	 * @return
	 */
	private TournamentAdminCreateRequestDto createTournamentCreateRequestDto(String title, LocalDateTime startTime,
		LocalDateTime endTime) {
		return new TournamentAdminCreateRequestDto(
			title,
			"제 1회 루키전 많관부!!",
			startTime,
			endTime,
			TournamentType.ROOKIE
		);
	}

	/**
	 * 토너먼트 게임 테이블 생성
	 * @param tournament 토너먼트
	 * @param round 몇 번째 게임인지에 대한 정보
	 * @return 새로 생성된 토너먼트 게임
	 */
	private TournamentGame createTournamentGame(Tournament tournament, TournamentRound round) {
		TournamentGame tournamentGame = new TournamentGame(null, tournament, round);
		return tournamentGameRepository.save(tournamentGame);
	}

	/**
	 * 현재 시간에서 days hours, 만큼 차이나는 시간을 구한다.
	 * @param days
	 * @param hours
	 * @return
	 */
	private LocalDateTime getTargetTime(int days, int hours, int minutes) {
		return LocalDateTime.now().plusDays(days).withHour(hours).withMinute(minutes);
	}

	/**
	 * 각 매개변수로 초기화 된 토너먼트를 반환
	 * @param id
	 * @param status
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private Tournament createTournament(Long id, TournamentStatus status, LocalDateTime startTime,
		LocalDateTime endTime) {
		Tournament tournament = Tournament.builder()
			.title(id + "st tournament")
			.contents("")
			.startTime(startTime)
			.endTime(endTime)
			.type(TournamentType.ROOKIE)
			.status(status)
			.build();
		ReflectionUtilsForUnitTest.setFieldWithReflection(tournament, "id", id);
		return tournament;
	}

	/**
	 * <div>id 부터 cnt개 만큼의 토너먼트 리스트를 반환해준다.<div/>
	 * 각 토너먼트는 1시간 길이이며, 토너먼트간 1시간의 간격이 있다.
	 * @param id
	 * @param cnt
	 * @param startTime
	 * @return
	 */
	private List<Tournament> createTournaments(Long id, long cnt, LocalDateTime startTime) {
		List<Tournament> tournamentList = new ArrayList<>();
		for (long i = 0; i < cnt; i++) {
			tournamentList.add(createTournament(id++, TournamentStatus.BEFORE,
				startTime.plusHours(i * 2), startTime.plusHours((i * 2 + 2))));
		}
		return tournamentList;
	}

	/**
	 * 각 매개변수로 초기화된 TournamentAdminUpdateRequestDto를 반환
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private TournamentAdminUpdateRequestDto createTournamentAdminUpdateRequestDto(LocalDateTime startTime,
		LocalDateTime endTime) {
		return new TournamentAdminUpdateRequestDto(
			"tournament changed",
			"changed",
			startTime,
			endTime,
			TournamentType.ROOKIE
		);
	}

	/**
	 * 유저 생성 매서드 - intraId로만 초기화
	 * @param intraId
	 * @return
	 */
	private User createUser(String intraId) {
		return User.builder()
			.eMail("email")
			.intraId(intraId)
			.racketType(RacketType.PENHOLDER)
			.snsNotiOpt(SnsType.NONE)
			.roleType(RoleType.USER)
			.totalExp(1000)
			.build();
	}

	/**
	 * cnt 사이즈의 토너먼트 게임 리스트 생성
	 * @param id 토너먼트 게임 id
	 * @param tournament 해당 토너먼트
	 * @param cnt 토너먼트 게임 수
	 * @return
	 */
	private List<TournamentGame> createTournamentGames(Long id, Tournament tournament, int cnt) {
		List<TournamentGame> tournamentGameList = new ArrayList<>();
		TournamentRound[] values = TournamentRound.values();
		while (--cnt >= 0) {
			tournamentGameList.add(new TournamentGame(null, tournament, values[cnt]));
		}
		return tournamentGameList;
	}

	private SlotManagement createSlot(int gameInterval) {
		return SlotManagement.builder()
			.pastSlotTime(0)
			.futureSlotTime(0)
			.openMinute(0)
			.gameInterval(gameInterval)
			.startTime(LocalDateTime.now().minusHours(1))
			.build();
	}
}
