package com.example.EcoGo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class EcoGoApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = start(args);
		maybeAutoClose(ctx);
	}

	public static ConfigurableApplicationContext start(String... args) {
		return SpringApplication.run(EcoGoApplication.class, args);
	}

	/**
	 * Test helper: allow unit tests to exercise {@link #main(String[])} safely.
	 * Usage: -Decogo.autoclose=true
	 */
	static void maybeAutoClose(ConfigurableApplicationContext ctx) {
		if (ctx == null) {
			return;
		}
		if (Boolean.getBoolean("ecogo.autoclose")) {
			ctx.close();
		}
	}

}
