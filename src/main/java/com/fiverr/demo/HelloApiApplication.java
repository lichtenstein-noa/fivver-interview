package com.fiverr.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

@SpringBootApplication
@RestController
public class HelloApiApplication {

	private final DataSource dataSource;

	// This automatically injects the database connection Spring configured for us
	public HelloApiApplication(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public static void main(String[] args) {
		SpringApplication.run(HelloApiApplication.class, args);
	}

	// Health check endpoint — just returns "Hello World"
	@GetMapping("/hello")
	public Map<String, String> hello() {
		return Map.of("message", "Hello World");
	}

	// Database check endpoint — confirms the DB connection is working
	@GetMapping("/db-check")
	public Map<String, String> dbCheck() {
		try (Connection conn = dataSource.getConnection();
			 Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery("SELECT NOW() as current_time")) {

			if (rs.next()) {
				return Map.of("status", "ok", "db_time", rs.getString("current_time"));
			}
			return Map.of("status", "error", "message", "No result from database");

		} catch (Exception e) {
			return Map.of("status", "error", "message", e.getMessage());
		}
	}
}