package idv.hzm.app.bot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 認證攔截器 — 從 Authorization header 解析 Bearer token，
 * 驗證後將 userId 設為 request attribute 供下游使用。
 * 未通過驗證時回傳 401。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthInterceptor.class);
	public static final String USER_ID_ATTR = "authenticatedUserId";

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
			return false;
		}

		String token = authHeader.substring(7);
		String userId = jwtTokenProvider.validateAndGetUserId(token);
		if (userId == null) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
			return false;
		}

		request.setAttribute(USER_ID_ATTR, userId);
		logger.debug("[AUTH] Authenticated userId={}", userId);
		return true;
	}
}
