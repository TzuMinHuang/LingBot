package idv.hzm.app.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "idv.hzm.app.portal", "idv.hzm.app.common" })
public class PortalApplication {

	public static void main(String[] args) {
		System.out.println("DATASOURCE_URL: " + System.getenv("DATASOURCE_URL"));
		SpringApplication.run(PortalApplication.class, args);
	}

}
