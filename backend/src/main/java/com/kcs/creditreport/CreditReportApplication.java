package com.kcs.creditreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// @ConfigurationPropertiesScan : JwtProperties·CookieProperties(record 기반) 자동 등록
@SpringBootApplication
@ConfigurationPropertiesScan
public class CreditReportApplication {

	public static void main(String[] args) {
		SpringApplication.run(CreditReportApplication.class, args);
	}

}
