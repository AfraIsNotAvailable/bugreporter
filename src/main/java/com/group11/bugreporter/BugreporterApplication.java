package com.group11.bugreporter;

import com.group11.bugreporter.config.DemoData;
import com.group11.bugreporter.config.DockerComposeBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class BugreporterApplication {

	public static void main(String[] args) {
		DockerComposeBootstrap.bootstrapIfNecessary();
		ConfigurableApplicationContext context = SpringApplication.run(BugreporterApplication.class, args);
		context.getBean(DemoData.class).loadDemoData();
	}

}
