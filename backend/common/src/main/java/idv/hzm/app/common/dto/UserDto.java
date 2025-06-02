package idv.hzm.app.common.dto;

public class UserDto {
	private String userId;
	private UserRole role; // 使用 enum 存此人於此房間中的角色

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

}
