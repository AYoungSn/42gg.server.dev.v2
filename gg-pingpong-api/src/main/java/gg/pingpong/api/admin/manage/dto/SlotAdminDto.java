package gg.pingpong.api.admin.manage.dto;

import java.time.LocalDateTime;

import gg.data.manage.SlotManagement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SlotAdminDto {
	private Integer pastSlotTime;

	private Integer futureSlotTime;

	private Integer interval;

	private Integer openMinute;

	private LocalDateTime startTime;

	private LocalDateTime endTime;

	public SlotAdminDto(SlotManagement slotManagement) {
		this.pastSlotTime = slotManagement.getPastSlotTime();
		this.futureSlotTime = slotManagement.getFutureSlotTime();
		this.interval = slotManagement.getGameInterval();
		this.openMinute = slotManagement.getOpenMinute();
		this.startTime = slotManagement.getStartTime();
		this.endTime = slotManagement.getEndTime();
	}
}
