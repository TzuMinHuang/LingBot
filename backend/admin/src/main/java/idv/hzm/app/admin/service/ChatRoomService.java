package idv.hzm.app.admin.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.admin.entity.ChatRoom;
import idv.hzm.app.common.dto.UserDto;
import idv.hzm.app.common.util.CodeGeneratorUtil;

@Service
public class ChatRoomService {

	@Autowired
	private CodeGeneratorUtil codeGeneratorUtil;

	public ChatRoom createChatRoom(List<UserDto> participants) {

		ChatRoom chatRoom = new ChatRoom();
		chatRoom.setRoomId(this.codeGeneratorUtil.nextRoomCode());
		chatRoom.setParticipants(participants);
		return chatRoom;
	}

}
