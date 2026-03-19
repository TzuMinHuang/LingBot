package idv.hzm.app.bot.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.cors-allowed-origins}")
	private String[] corsAllowedOrigins;

	@Value("${app.auth.enabled:false}")
	private boolean authEnabled;

	@Autowired
	private JwtAuthInterceptor jwtAuthInterceptor;

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedOrigins(corsAllowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("Content-Type", "Authorization", "Accept", "Cache-Control")
				.allowCredentials(true);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		if (authEnabled) {
			registry.addInterceptor(jwtAuthInterceptor)
					.addPathPatterns("/chat/**")
					.excludePathPatterns("/auth/**", "/actuator/**");
		}
	}
}
