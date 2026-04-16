package com.daol.concierge.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

@Component
@Profile("dev")
public class InvSeedRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(InvSeedRunner.class);
	private final DataSource ds;

	public InvSeedRunner(DataSource ds) { this.ds = ds; }

	@Override
	public void run(String... args) throws Exception {
		ClassPathResource res = new ClassPathResource("seed/INV_SEED.sql");
		if (!res.exists()) { log.info("[SEED] INV_SEED.sql not found, skip"); return; }

		String sql;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
			sql = br.lines()
					.filter(line -> !line.trim().startsWith("--"))
					.collect(Collectors.joining("\n"));
		}

		try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
			int total = 0;
			for (String stmt : sql.split(";")) {
				String trimmed = stmt.trim();
				if (trimmed.isEmpty()) continue;
				try {
					st.executeUpdate(trimmed);
					total++;
				} catch (Exception e) {
					log.warn("[SEED] skip: {}", e.getMessage());
				}
			}
			log.info("[SEED] INV_SEED.sql executed — {} statements", total);
		}
	}
}
