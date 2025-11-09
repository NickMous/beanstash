package com.nickmous.beanstash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class BeanstashApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeanstashApplication.class, args);
	}

}
