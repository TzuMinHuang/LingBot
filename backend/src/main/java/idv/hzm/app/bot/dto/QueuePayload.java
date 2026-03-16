package idv.hzm.app.bot.dto;

public class QueuePayload extends BasePayload {
	private int position;

	public QueuePayload() {
	}

	public QueuePayload(int position) {
		this.position = position;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}
