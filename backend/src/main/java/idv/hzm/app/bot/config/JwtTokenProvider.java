package idv.hzm.app.bot.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 輕量 JWT 工具（HMAC-SHA256），供 Mock SSO 使用。
 * 不引入外部 JWT 庫，僅用 Java 內建 API。
 */
@Component
public class JwtTokenProvider {

	private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
	private static final String ALGORITHM = "HmacSHA256";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Value("${app.jwt.secret:default-dev-secret-change-in-production}")
	private String secret;

	@Value("${app.jwt.expiration-seconds:86400}")
	private long expirationSeconds;

	public String generateToken(String userId, String employeeName) {
		try {
			long now = System.currentTimeMillis() / 1000;
			long exp = now + expirationSeconds;

			String header = base64Url(MAPPER.writeValueAsString(Map.of("alg", "HS256", "typ", "JWT")));
			String payload = base64Url(MAPPER.writeValueAsString(Map.of(
					"sub", userId,
					"name", employeeName,
					"iat", now,
					"exp", exp)));

			String signature = sign(header + "." + payload);
			return header + "." + payload + "." + signature;
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate JWT", e);
		}
	}

	/**
	 * 驗證 token 並回傳 userId (sub claim)。無效或過期回傳 null。
	 */
	public String validateAndGetUserId(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) return null;

			String expectedSig = sign(parts[0] + "." + parts[1]);
			// timing-safe comparison to prevent side-channel attacks
			if (!MessageDigest.isEqual(
					expectedSig.getBytes(StandardCharsets.UTF_8),
					parts[2].getBytes(StandardCharsets.UTF_8))) return null;

			String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
			@SuppressWarnings("unchecked")
			Map<String, Object> claims = MAPPER.readValue(payloadJson, Map.class);

			long exp = ((Number) claims.get("exp")).longValue();
			if (System.currentTimeMillis() / 1000 > exp) {
				logger.debug("JWT expired for sub={}", claims.get("sub"));
				return null;
			}

			return (String) claims.get("sub");
		} catch (Exception e) {
			logger.debug("JWT validation failed: {}", e.getMessage());
			return null;
		}
	}

	private String sign(String data) throws Exception {
		Mac mac = Mac.getInstance(ALGORITHM);
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
		byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}

	private String base64Url(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}
}
