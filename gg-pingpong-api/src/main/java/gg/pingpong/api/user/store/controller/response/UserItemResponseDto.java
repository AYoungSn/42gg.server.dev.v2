package gg.pingpong.api.user.store.controller.response;

import gg.data.store.Item;
import gg.data.store.Receipt;
import gg.data.store.type.ItemStatus;
import gg.data.store.type.ItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserItemResponseDto {
	private Long receiptId;
	private String itemName;
	private String imageUri;
	private String purchaserIntra;
	private ItemStatus itemStatus;
	private ItemType itemType;

	public UserItemResponseDto(Receipt receipt) {
		Item item = receipt.getItem();
		this.receiptId = receipt.getId();
		this.itemName = item.getName();
		this.imageUri = item.getImageUri();
		this.purchaserIntra = receipt.getPurchaserIntraId();
		this.itemStatus = receipt.getStatus();
		this.itemType = item.getType();
	}

}
