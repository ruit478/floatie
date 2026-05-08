package com.floatie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.Map;

@SpringBootApplication
@RestController
public class FloatieApplication {

	public static void main(String[] args) {
		SpringApplication.run(FloatieApplication.class, args);
	}

	@GetMapping("/api/hello")
	public Map<String, String> sayHello() {
		return Map.of(
				"message", "Hello from Spring Boot! 🐾",
				"timestamp", LocalDateTime.now().toString()
		);
	}

	@GetMapping("/api/status")
	public Map<String, Object> status() {
		return Map.of(
				"status", "online",
				"service", "Floatie Backend",
				"version", "1.0.0",
				"buildSystem", "Gradle",
				"javaVersion", System.getProperty("java.version")
		);
	}
}