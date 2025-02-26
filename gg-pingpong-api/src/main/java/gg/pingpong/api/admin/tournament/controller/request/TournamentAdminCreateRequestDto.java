package gg.pingpong.api.admin.tournament.controller.request;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import gg.data.tournament.type.TournamentType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TournamentAdminCreateRequestDto {
	@NotNull(message = "제목이 필요합니다.")
	@Length(max = 30, message = "제목은 30자 이내로 작성해주세요.")
	private String title;

	@NotNull(message = "내용이 필요합니다.")
	@Length(max = 3000, message = "내용은 3000자 이내로 작성해주세요.")
	private String contents;

	@NotNull(message = "시작 시간이 필요합니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime startTime;

	@NotNull(message = "종료 시간이 필요합니다.")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private LocalDateTime endTime;

	@NotNull(message = "토너먼트 종류가 필요합니다.")
	private TournamentType type;
}
