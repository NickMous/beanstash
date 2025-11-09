package com.nickmous.beanstash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public final class BeanstashApplication {

	private BeanstashApplication() {
		// Private constructor to prevent instantiation
	}

	public static void main(String[] args) {
		SpringApplication.run(BeanstashApplication.class, args);
	}

}
