package idv.hzm.app.bot.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import idv.hzm.app.bot.config.JwtTokenProvider;

/**
 * Mock SSO 端點 — 模擬企業 SSO 回調，直接根據提供的員工資訊發放 JWT。
 * 正式環境應替換為真實 SSO provider（OIDC/SAML）整合。
 */
@RestController
public class MockSsoController {

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@PostMapping("/auth/mock-sso")
	public ResponseEntity<Map<String, String>> mockSsoCallback(@RequestBody Map<String, String> body) {
		String userId = body.get("userId");
		String name = body.getOrDefault("name", "Employee");

		if (userId == null || userId.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
		}

		String token = jwtTokenProvider.generateToken(userId, name);
		return ResponseEntity.ok(Map.of("token", token));
	}
}
