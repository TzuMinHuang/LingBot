package idv.hzm.app.admin;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import idv.hzm.app.admin.mapper.CityMapper;

@SpringBootApplication(scanBasePackages = { "idv.hzm.app.admin", "idv.hzm.app.common" })
public class AdminApplication implements CommandLineRunner {

	private final CityMapper cityMapper;

	public AdminApplication(CityMapper cityMapper) {
		this.cityMapper = cityMapper;
	}

	public static void main(String[] args) {
		SpringApplication.run(AdminApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		System.out.println(this.cityMapper.findBySid("01"));
		System.out.println(this.cityMapper.findByNotSids(List.of("01", "02")));
		System.out.println(this.cityMapper.findByNotSidsAndNotSnames(List.of("01", "02"), List.of("孫風", "李雲")));
	}

}
