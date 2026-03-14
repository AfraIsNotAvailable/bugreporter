package com.group11.bugreporter;

import com.group11.bugreporter.config.DockerComposeBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BugreporterApplication {

	public static void main(String[] args) {
		DockerComposeBootstrap.bootstrapIfNecessary();
		SpringApplication.run(BugreporterApplication.class, args);
	}

}
