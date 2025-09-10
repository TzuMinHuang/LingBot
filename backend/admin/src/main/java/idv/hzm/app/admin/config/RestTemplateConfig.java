package idv.hzm.app.admin.config;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add((request, body, execution) -> {
			long start = System.currentTimeMillis();
			ClientHttpResponse response = execution.execute(request, body);
			long end = System.currentTimeMillis();
			long time = end - start;
			LoggerFactory.getLogger("RestTemplateMonitor").info("RestTemplate call [{} {}] executed in {} ms",
					request.getMethod(), request.getURI(), time);
			return response;
		});
		return restTemplate;
	}

}
