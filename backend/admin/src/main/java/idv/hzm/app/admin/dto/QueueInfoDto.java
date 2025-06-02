package idv.hzm.app.admin.dto;

public class QueueInfoDto {

	private String sessionId;
	private boolean isAlreadyInQueue;
	private int position;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public boolean isAlreadyInQueue() {
		return isAlreadyInQueue;
	}

	public void setAlreadyInQueue(boolean isAlreadyInQueue) {
		this.isAlreadyInQueue = isAlreadyInQueue;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public String toString() {
		return "QueueInfoDto [sessionId=" + sessionId + ", isAlreadyInQueue=" + isAlreadyInQueue + ", position="
				+ position + "]";
	}

}
