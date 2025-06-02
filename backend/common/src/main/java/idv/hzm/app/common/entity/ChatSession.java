package idv.hzm.app.common.entity;

import java.time.LocalDateTime;
import java.util.List;

import idv.hzm.app.common.dto.EventDto;

public class ChatSession {
	private String sessionId; // 會話的唯一 ID
	private List<String> participants; // 參與者列表（客戶與客服）
	private String status;// 會話狀態（例如：進行中、結束）
	private LocalDateTime startTime; // 會話開始時間
	private LocalDateTime lastActivityTime; // 最後活動時間
	private LocalDateTime endTime; // 會話結束時間
	private List<EventDto> chatMessages; // 訊息列表
	private String language; // 使用語言

	// Getter and Setter methods
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public List<String> getParticipants() {
		return participants;
	}

	public void setParticipants(List<String> participants) {
		this.participants = participants;
	}

	public List<EventDto> getChatMessages() {
		return chatMessages;
	}

	public void setChatMessages(List<EventDto> chatMessages) {
		this.chatMessages = chatMessages;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public LocalDateTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public LocalDateTime getLastActivityTime() {
		return lastActivityTime;
	}

	public void setLastActivityTime(LocalDateTime lastActivityTime) {
		this.lastActivityTime = lastActivityTime;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}
