package idv.hzm.app.admin.entity;

import java.util.List;

import idv.hzm.app.common.dto.UserDto;

public class ChatRoom {

	private String roomId;
	private List<UserDto> participants;

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public List<UserDto> getParticipants() {
		return participants;
	}

	public void setParticipants(List<UserDto> participants) {
		this.participants = participants;
	}
}
