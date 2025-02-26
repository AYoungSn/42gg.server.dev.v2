package gg.pingpong.api.user.tournament.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.data.tournament.Tournament;
import gg.data.tournament.TournamentUser;
import gg.data.tournament.type.TournamentStatus;
import gg.data.tournament.type.TournamentType;
import gg.data.user.User;
import gg.data.user.type.RacketType;
import gg.data.user.type.RoleType;
import gg.data.user.type.SnsType;
import gg.pingpong.api.global.security.jwt.utils.AuthTokenProvider;
import gg.pingpong.api.user.tournament.controller.response.TournamentListResponseDto;
import gg.pingpong.api.user.tournament.controller.response.TournamentResponseDto;
import gg.repo.tournarment.TournamentUserRepository;
import gg.utils.TestDataUtils;
import gg.utils.annotation.IntegrationTest;
import gg.utils.exception.ErrorCode;
import gg.utils.exception.custom.CustomRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor
@Slf4j
public class TournamentFindControllerTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	TestDataUtils testDataUtils;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	AuthTokenProvider tokenProvider;
	@Autowired
	TournamentUserRepository tournamentUserRepository;

	List<TournamentResponseDto> tournamentList;
	String accessToken;

	User tester;

	@Nested
	@DisplayName("토너먼트_리스트_조회")
	class FindTournamentListTest {
		@BeforeEach
		void beforeEach() {
			tester = testDataUtils.createNewUser("findControllerTester", "findControllerTester", RacketType.DUAL,
				SnsType.SLACK, RoleType.ADMIN);
			accessToken = tokenProvider.createToken(tester.getId());
			tournamentList = testDataUtils.makeTournamentList();
		}

		@Test
		@DisplayName("전체_조회")
		public void getTournamentList() throws Exception {
			// given
			int page = 1;
			int size = 20;
			String url = "/pingpong/tournaments/?page=" + page + "&size=" + size;

			// when
			String contentAsString = mockMvc.perform(
					get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
			TournamentListResponseDto resp = objectMapper.readValue(contentAsString, TournamentListResponseDto.class);

			// then
			List<TournamentResponseDto> tournamentInfoList = resp.getTournaments();
			for (int i = 0; i < tournamentInfoList.size(); i++) {
				Long tournamentId = tournamentInfoList.get(i).getTournamentId();
				TournamentResponseDto tournamentResponseDto = tournamentList.stream()
					.filter(t -> t.getTournamentId().equals(tournamentId))
					.findFirst()
					.orElse(null);
				if (tournamentResponseDto != null) {
					assertThat(tournamentInfoList.get(i).getTitle()).isEqualTo(tournamentResponseDto.getTitle());
					assertThat(tournamentInfoList.get(i).getContents()).isEqualTo(tournamentResponseDto.getContents());
					assertThat(tournamentInfoList.get(i).getType()).isEqualTo(tournamentResponseDto.getType());
					assertThat(tournamentInfoList.get(i).getStatus()).isEqualTo(tournamentResponseDto.getStatus());
					assertThat(tournamentInfoList.get(i).getWinnerIntraId()).isEqualTo(
						tournamentResponseDto.getWinnerIntraId());
					assertThat(tournamentInfoList.get(i).getWinnerImageUrl()).isEqualTo(
						tournamentResponseDto.getWinnerImageUrl());
					assertThat(tournamentInfoList.get(i).getPlayerCnt()).isEqualTo(
						tournamentResponseDto.getPlayerCnt());
				}
				if (i > 0) {
					assertThat(tournamentInfoList.get(i).getStartTime()).isAfter(
						tournamentInfoList.get(i - 1).getEndTime());
				}
			}
		}

		@Test
		@DisplayName("status별_조회")
		public void getTournamentListByStatus() throws Exception {

			// given
			int page = 1;
			int size = 10;
			String url = "/pingpong/tournaments/?page=" + page + "&size=" + size + "&status=" + TournamentStatus.BEFORE;

			// when
			String contentAsString = mockMvc.perform(
					get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
			TournamentListResponseDto resp = objectMapper.readValue(contentAsString, TournamentListResponseDto.class);

			// then
			List<TournamentResponseDto> tournamentInfoList = resp.getTournaments();
			for (TournamentResponseDto responseDto : tournamentInfoList) {
				assertThat(responseDto.getStatus()).isEqualTo(TournamentStatus.BEFORE);
			}
		}

		@Test
		@DisplayName("type별_조회")
		public void getTournamentListByType() throws Exception {

			// given
			int page = 1;
			int size = 10;
			String url = "/pingpong/tournaments/?page=" + page + "&size=" + size + "&type=" + TournamentType.ROOKIE;

			// when
			String contentAsString = mockMvc.perform(
					get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
			TournamentListResponseDto resp = objectMapper.readValue(contentAsString, TournamentListResponseDto.class);

			// then
			List<TournamentResponseDto> tournamentInfoList = resp.getTournaments();
			for (TournamentResponseDto responseDto : tournamentInfoList) {
				assertThat(responseDto.getType()).isEqualTo(TournamentType.ROOKIE);
			}
		}

		@Test
		@DisplayName("type과 status 별 조회")
		public void getTournamentListByTypeAndStatus() throws Exception {
			// given
			int page = 1;
			int size = 10;
			String url =
				"/pingpong/tournaments/?page=" + page + "&size=" + size + "&type=" + TournamentType.ROOKIE + "&status="
					+ TournamentStatus.BEFORE;

			// when
			String contentAsString = mockMvc.perform(
					get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
			TournamentListResponseDto resp = objectMapper.readValue(contentAsString, TournamentListResponseDto.class);

			// then
			List<TournamentResponseDto> tournamentInfoList = resp.getTournaments();
			for (TournamentResponseDto responseDto : tournamentInfoList) {
				assertThat(responseDto.getType()).isEqualTo(TournamentType.ROOKIE);
				assertThat(responseDto.getStatus()).isEqualTo(TournamentStatus.BEFORE);
			}
		}

		@Test
		@DisplayName("잘못된 type")
		public void wrongType() throws Exception {
			// given
			int page = 1;
			int size = 10;
			String url = "/pingpong/tournaments/?page=" + page + "&size=" + size + "&type=" + "rookie123" + "&status="
				+ TournamentStatus.BEFORE.getCode();

			// when
			String contentAsString = mockMvc.perform(
					get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isBadRequest())
				.andReturn().getResponse().getContentAsString();

			// then
			log.info(contentAsString);
		}

		@Test
		@DisplayName("잘못된 status")
		public void wrongStatus() throws Exception {
			// given
			int page = 1;
			int size = 10;
			String url =
				"/pingpong/tournaments/?page=" + page + "&size=" + size + "&type=" + TournamentType.ROOKIE.getCode()
					+ "&status=" + "wrongStatus";

			// when
			String contentAsString = mockMvc.perform(
					get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isBadRequest())
				.andReturn().getResponse().getContentAsString();

			// then
			log.info(contentAsString);
		}
	}

	@Nested
	@DisplayName("토너먼트_유저_신청_취소_테스트")
	class CancelTournamentUserRegistrationTest {
		@BeforeEach
		void beforeEach() {
			tester = testDataUtils.createNewUser("findControllerTester", "findControllerTester", RacketType.DUAL,
				SnsType.SLACK, RoleType.ADMIN);
			accessToken = tokenProvider.createToken(tester.getId());
		}

		@Test
		@DisplayName("유저_신청_취소_성공")
		void success() throws Exception {
			// given
			int maxTournamentUser = 8;
			Tournament tournament = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			for (int i = 0; i < maxTournamentUser - 1; i++) {
				testDataUtils.createTournamentUser(testDataUtils.createNewUser("testUser" + i), tournament, true);
			}
			testDataUtils.createTournamentUser(tester, tournament, true);
			for (int i = maxTournamentUser; i < maxTournamentUser + 4; i++) {
				testDataUtils.createTournamentUser(testDataUtils.createNewUser("testUser" + i), tournament, false);
			}
			String url = "/pingpong/tournaments/" + tournament.getId() + "/users";
			String expected = "{\"status\":\"BEFORE\"}";

			// when
			String contentAsString = mockMvc.perform(delete(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			// then
			System.out.println(contentAsString);
			if (expected.compareTo(contentAsString) != 0) {
				throw new CustomRuntimeException("상태 오류", ErrorCode.BAD_REQUEST);
			}
			tournamentUserRepository.findByTournamentIdAndUserId(tournament.getId(), tester.getId()).ifPresent(
				a -> {
					throw new CustomRuntimeException("", ErrorCode.BAD_REQUEST);
				});
			List<TournamentUser> tournamentUserList = tournament.getTournamentUsers();
			for (int i = 0; i < maxTournamentUser; i++) {
				if (!tournamentUserList.get(i).getIsJoined()) {
					throw new CustomRuntimeException("참가자 오류", ErrorCode.BAD_REQUEST);
				}
			}
			for (int i = maxTournamentUser; i < tournamentUserList.size(); i++) {
				if (tournamentUserList.get(i).getIsJoined()) {
					throw new CustomRuntimeException("대기자 오류", ErrorCode.BAD_REQUEST);
				}
			}
		}

		@Test
		@DisplayName("유저_없음")
		void userNotFound() throws Exception {
			// given
			Tournament tournament = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			String url = "/pingpong/tournaments/" + tournament.getId() + "/users";

			// when, then
			String contentAsString = mockMvc.perform(delete(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

			System.out.println(contentAsString);
		}

		@Test
		@DisplayName("토너먼트_없음")
		void tournamentNotFound() throws Exception {
			// given
			String url = "/pingpong/tournaments/" + 9999 + "/users";

			// when, then
			String contentAsString = mockMvc.perform(delete(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

			System.out.println(contentAsString);
		}

	}

	@Nested
	@DisplayName("토너먼트_단일_조회")
	class FindTournamentTest {
		@BeforeEach
		void beforeEach() {
			tester = testDataUtils.createNewUser("findControllerTester", "findControllerTester", RacketType.DUAL,
				SnsType.SLACK, RoleType.ADMIN);
			accessToken = tokenProvider.createToken(tester.getId());
		}

		@Test
		@DisplayName("조회_성공")
		public void success() throws Exception {
			//given
			Tournament tournament = testDataUtils.createTournament("string1", "string",
				LocalDateTime.now().plusDays(2).plusHours(1), LocalDateTime.now().plusDays(2).plusHours(3),
				TournamentType.ROOKIE, TournamentStatus.BEFORE);
			User user = testDataUtils.createNewUser("test");
			testDataUtils.createTournamentUser(user, tournament, true);
			tournament.updateWinner(user);

			Long tournamentId = tournament.getId();
			String url = "/pingpong/tournaments/" + tournamentId;

			//when
			String contentAsString = mockMvc.perform(get(url)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			TournamentResponseDto responseDto = objectMapper.readValue(contentAsString, TournamentResponseDto.class);

			//then
			assertThat(tournament.getTitle()).isEqualTo(responseDto.getTitle());
			assertThat(tournament.getContents()).isEqualTo(responseDto.getContents());
			assertThat(tournament.getType()).isEqualTo(responseDto.getType());
			assertThat(tournament.getStatus()).isEqualTo(responseDto.getStatus());
			if (tournament.getWinner() == null) {
				assertThat(responseDto.getWinnerIntraId()).isEqualTo(null);
				assertThat(responseDto.getWinnerImageUrl()).isEqualTo(null);
			} else {
				assertThat(tournament.getWinner().getIntraId()).isEqualTo(responseDto.getWinnerIntraId());
				assertThat(tournament.getWinner().getImageUri()).isEqualTo(responseDto.getWinnerImageUrl());
			}
			assertThat(tournament.getTournamentUsers().size()).isEqualTo(responseDto.getPlayerCnt());
		}

		@Test
		@DisplayName("잘못된_토너먼트_ID")
		public void tournamentNotExist() throws Exception {
			//given
			Long tournamentId = 1L;
			String url = "/pingpong/tournaments/" + tournamentId;

			//when
			String contentAsString = mockMvc.perform(get(url)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

			//then
			System.out.println(contentAsString);
		}
	}

	@Nested
	@DisplayName("토너먼트_유저_참가_상태_조회")
	class UserStatusInTournamentTest {
		@BeforeEach
		void beforeEach() {
			tester = testDataUtils.createNewUser("findControllerTester", "findControllerTester", RacketType.DUAL,
				SnsType.SLACK, RoleType.ADMIN);
			accessToken = tokenProvider.createToken(tester.getId());
		}

		@Test
		@DisplayName("유저_상태_조회_성공")
		void success() throws Exception {
			// given 1
			Tournament tournament = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			String url = "/pingpong/tournaments/" + tournament.getId() + "/users";
			String expected1 = "{\"status\":\"BEFORE\"}";

			// when 1
			String contentAsString = mockMvc.perform(get(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			// then 1
			if (expected1.compareTo(contentAsString) != 0) {
				throw new CustomRuntimeException("상태 오류", ErrorCode.BAD_REQUEST);
			}

			// given 2
			testDataUtils.createTournamentUser(tester, tournament, false);
			String expected2 = "{\"status\":\"WAIT\"}";

			// when 2
			contentAsString = mockMvc.perform(get(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			// then 2
			if (expected2.compareTo(contentAsString) != 0) {
				throw new CustomRuntimeException("상태 오류", ErrorCode.BAD_REQUEST);
			}

			// given 3
			tournamentUserRepository.findByTournamentIdAndUserId(tournament.getId(), tester.getId())
				.get().updateIsJoined(true);
			String expected3 = "{\"status\":\"PLAYER\"}";

			// when 3
			contentAsString = mockMvc.perform(get(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

			// then 3
			if (expected3.compareTo(contentAsString) != 0) {
				throw new CustomRuntimeException("상태 오류", ErrorCode.BAD_REQUEST);
			}
		}

		@Test
		@DisplayName("토너먼트_없음")
		void tournamentNotFound() throws Exception {
			// given
			String url = "/pingpong/tournaments/" + 9999 + "/users";

			// when, then
			String contentAsString = mockMvc.perform(get(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

			System.out.println(contentAsString);
		}

	}

	@Nested
	@DisplayName("토너먼트_유저_참가_신청")
	class RegisterTournamentUserTest {
		@BeforeEach
		void beforeEach() {
			tester = testDataUtils.createNewUser("findControllerTester", "findControllerTester", RacketType.DUAL,
				SnsType.SLACK, RoleType.ADMIN);
			accessToken = tokenProvider.createToken(tester.getId());
		}

		@Test
		@DisplayName("유저_참가_신청(참가자)_성공")
		void successPlayer() throws Exception {
			// given
			Tournament tournament = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			String url = "/pingpong/tournaments/" + tournament.getId() + "/users";
			String expected = "{\"status\":\"PLAYER\"}";

			// when
			String contentAsString = mockMvc.perform(post(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

			// then
			if (expected.compareTo(contentAsString) != 0) {
				throw new CustomRuntimeException("상태 오류", ErrorCode.BAD_REQUEST);
			}
		}

		@Test
		@DisplayName("유저_참가_신청(대기자)_성공")
		void successWait() throws Exception {
			// given
			int maxTournamentUser = 8;
			Tournament tournament = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			for (int i = 0; i < maxTournamentUser; i++) {
				testDataUtils.createTournamentUser(testDataUtils.createNewUser("testUser" + i), tournament, true);
			}
			String url = "/pingpong/tournaments/" + tournament.getId() + "/users";
			String expected = "{\"status\":\"WAIT\"}";

			// when
			String contentAsString = mockMvc.perform(post(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

			// then
			if (expected.compareTo(contentAsString) != 0) {
				throw new CustomRuntimeException("상태 오류", ErrorCode.BAD_REQUEST);
			}
		}

		@Test
		@DisplayName("토너먼트_없음")
		void tournamentNotFound() throws Exception {
			// given
			String url = "/pingpong/tournaments/" + 9999 + "/users";

			// when, then
			String contentAsString = mockMvc.perform(post(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

			System.out.println(contentAsString);
		}

		@Test
		@DisplayName("이미_신청한_토너먼트_존재")
		void conflictRegisteration() throws Exception {
			// given
			Tournament tournament1 = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			Tournament tournament2 = testDataUtils.createTournament(LocalDateTime.now(), LocalDateTime.now(),
				TournamentStatus.BEFORE);
			testDataUtils.createTournamentUser(tester, tournament1, false);
			String url = "/pingpong/tournaments/" + tournament2.getId() + "/users";

			// when, then
			String contentAsString = mockMvc.perform(post(url)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isConflict())
				.andReturn().getResponse().getContentAsString();

			System.out.println(contentAsString);
		}

	}
}
