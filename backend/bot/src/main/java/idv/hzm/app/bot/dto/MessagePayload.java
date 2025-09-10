package idv.hzm.app.bot.dto;

public class MessagePayload extends BasePayload {
	private String content;

	public MessagePayload() {
		this("");
	}

	public MessagePayload(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
