package gg.pingpong.api.user.season.controller.response;

import gg.data.season.Season;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SeasonResDto {
	private Long id;
	private String name;

	public SeasonResDto(Season season) {
		this.id = season.getId();
		this.name = season.getSeasonName();
	}

	@Override
	public String toString() {
		return "SeasonResDto{"
			+ "id=" + id
			+ ", name='" + name + '\''
			+ '}';
	}
}
