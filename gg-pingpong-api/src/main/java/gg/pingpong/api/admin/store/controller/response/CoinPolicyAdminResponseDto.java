package gg.pingpong.api.admin.store.controller.response;

import java.time.LocalDateTime;

import gg.data.store.CoinPolicy;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoinPolicyAdminResponseDto {
	private Long coinPolicyId;
	private String createUserId;
	private int attendance;
	private int normal;
	private int rankWin;
	private int rankLose;
	private LocalDateTime createdAt;

	public CoinPolicyAdminResponseDto(CoinPolicy coinPolicyAdmin) {
		this.coinPolicyId = coinPolicyAdmin.getId();
		this.createUserId = coinPolicyAdmin.getUser().getIntraId();
		this.attendance = coinPolicyAdmin.getAttendance();
		this.normal = coinPolicyAdmin.getNormal();
		this.rankWin = coinPolicyAdmin.getRankWin();
		this.rankLose = coinPolicyAdmin.getRankLose();
		this.createdAt = coinPolicyAdmin.getCreatedAt();
	}
}
