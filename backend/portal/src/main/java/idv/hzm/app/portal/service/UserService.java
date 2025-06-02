package idv.hzm.app.portal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.common.dto.UserDto;
import idv.hzm.app.common.dto.UserRole;
import idv.hzm.app.common.util.CodeGeneratorUtil;

@Service
public class UserService {

	@Autowired
	private ChatRedisService chatRedisService;
	@Autowired
	private CodeGeneratorUtil codeGeneratorUtil;

	public UserDto createCustomerUser() {
		final String userId = this.codeGeneratorUtil.nextUserCode();

		UserDto user = new UserDto();
		user.setUserId(userId);
		user.setRole(UserRole.CUSTOMER);

		this.chatRedisService.saveUserInfo(userId, user);
		return user;
	}

}
