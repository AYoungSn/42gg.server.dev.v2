package gg.pingpong.api.user.megaphone.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalTime;

import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.data.store.Item;
import gg.data.store.Megaphone;
import gg.data.store.Receipt;
import gg.data.store.type.ItemStatus;
import gg.data.store.type.ItemType;
import gg.data.user.User;
import gg.data.user.type.RacketType;
import gg.data.user.type.RoleType;
import gg.data.user.type.SnsType;
import gg.pingpong.api.admin.store.controller.request.ItemUpdateRequestDto;
import gg.pingpong.api.global.security.jwt.utils.AuthTokenProvider;
import gg.pingpong.api.user.store.controller.request.MegaphoneUseRequestDto;
import gg.repo.store.MegaphoneRepository;
import gg.repo.store.ReceiptRepository;
import gg.utils.ItemTestUtils;
import gg.utils.TestDataUtils;
import gg.utils.annotation.IntegrationTest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@IntegrationTest
@AutoConfigureMockMvc
class MegaphoneControllerTest {
	@Autowired
	TestDataUtils testDataUtils;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	AuthTokenProvider tokenProvider;

	@Autowired
	MegaphoneRepository megaphoneRepository;

	@Autowired
	ReceiptRepository receiptRepository;

	@Autowired
	ItemTestUtils itemTestUtils;

	@Test
	@Transactional
	@DisplayName("[Post] /pingpong/megaphones")
	void useMegaphoneTest() throws Exception {
		// 해당 테스트는 시스템상 23:55 ~ 00:05 사이에 테스트 불가능
		if (LocalTime.now().isAfter(LocalTime.of(23, 54))
			|| LocalTime.now().isBefore(LocalTime.of(0, 6))) {
			return;
		}
		String intraId = "intra";
		String email = "email";
		User newUser = testDataUtils.createNewUser(intraId, email, RacketType.PENHOLDER,
			SnsType.BOTH, RoleType.ADMIN);
		String accessToken = tokenProvider.createToken(newUser.getId());
		// db에 저장해두고 테스트
		ItemUpdateRequestDto dto = new ItemUpdateRequestDto("확성기", "default",
			"default", 40, 50, ItemType.MEGAPHONE);
		Item item = itemTestUtils.createItem(newUser, dto);
		Receipt receipt = itemTestUtils.purchaseItem(newUser, newUser, item);
		MegaphoneUseRequestDto megaphoneUseRequestDto = new MegaphoneUseRequestDto(receipt.getId(), "test");
		String content = objectMapper.writeValueAsString(megaphoneUseRequestDto);
		String url = "/pingpong/megaphones";

		mockMvc.perform(post(url)
				.content(content)
				.contentType(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().is2xxSuccessful())
			.andReturn().getResponse().getContentAsString();

		Megaphone result = megaphoneRepository.findFirstByOrderByIdDesc();
		assertThat(result.getContent()).isEqualTo(megaphoneUseRequestDto.getContent());
	}

	@Test
	@Transactional
	@DisplayName("DELETE /pingpong/megaphones/{megaphoneId}")
	public void deleteMegaphoneTest() throws Exception {
		//given
		String intraId = "intra2";
		String email = "email";
		User newUser = testDataUtils.createNewUser(intraId, email, RacketType.PENHOLDER,
			SnsType.BOTH, RoleType.ADMIN);
		String accessToken = tokenProvider.createToken(newUser.getId());
		ItemUpdateRequestDto dto = new ItemUpdateRequestDto("확성기", "default",
			"default", 40, 50, ItemType.MEGAPHONE);
		Item item = itemTestUtils.createItem(newUser, dto);
		Receipt receipt = itemTestUtils.purchaseItem(newUser, newUser, item);
		Receipt receipt2 = itemTestUtils.purchaseItem(newUser, newUser, item);
		Megaphone megaphone = itemTestUtils.createMegaPhone(newUser, receipt, "test");
		Megaphone megaphone2 = itemTestUtils.createMegaPhone(newUser, receipt2, "test");
		String url = "/pingpong/megaphones/" + megaphone2.getId();

		//when
		mockMvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().is2xxSuccessful())
			.andReturn().getResponse();

		//then
		assertThat(receipt2.getStatus()).isEqualTo(ItemStatus.DELETED);
	}

	@Test
	@Transactional
	@DisplayName("[GET] /pingpong/megaphones/receipt/{receiptId}")
	void getMegaphoneDetailTest() throws Exception {
		String intraId = "intra";
		String email = "email";
		User newUser = testDataUtils.createNewUser(intraId, email, RacketType.PENHOLDER,
			SnsType.BOTH, RoleType.ADMIN);
		String accessToken = tokenProvider.createToken(newUser.getId());
		ItemUpdateRequestDto dto = new ItemUpdateRequestDto("확성기", "default",
			"default", 40, 50, ItemType.MEGAPHONE);
		Item item = itemTestUtils.createItem(newUser, dto);
		Receipt receipt = itemTestUtils.purchaseItem(newUser, newUser, item);
		Megaphone megaphone = itemTestUtils.createMegaPhone(newUser, receipt, "test");
		String url = "/pingpong/megaphones/receipt/" + receipt.getId();

		String contentAsString = mockMvc.perform(get(url)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();

		System.out.println(contentAsString);
	}

	@Test
	@Transactional
	@DisplayName("[GET] /pingpong/megaphones")
	void getMegaphoneTodayListTest() throws Exception {
		String accessToken = testDataUtils.getLoginAccessToken();
		String url = "/pingpong/megaphones";
		mockMvc.perform(get(url)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().is2xxSuccessful())
			.andReturn().getResponse().getContentAsString();
	}
}
