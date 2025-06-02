package idv.hzm.app.portal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
        // 允許所有來源
        registry.addMapping("/**")  // 所有路由
                .allowedOrigins("http://localhost")  // 設定允許的來源
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // 設定允許的 HTTP 方法
                .allowedHeaders("*")  // 設定允許的標頭
                .allowCredentials(true);  // 設定是否允許傳遞憑證（cookies等）
	}
}
