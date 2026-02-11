package com.example.EcoGo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the application bootstrap class so JaCoCo package `com.example.EcoGo`
 * reaches the required threshold.
 */
class EcoGoApplicationMainTest {

    @AfterEach
    void clearAutoCloseProperty() {
        System.clearProperty("ecogo.autoclose");
    }

    @Test
    void main_canStartAndAutoClose_withMinimalConfig() {
        System.setProperty("ecogo.autoclose", "true");

        // Keep startup minimal and dependency-free.
        EcoGoApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.main.lazy-initialization=true",
                "--spring.main.banner-mode=off",
                "--spring.profiles.active=test",
                "--spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.scheduling.SchedulingAutoConfiguration"
        });
    }

    @Test
    void start_returnsContext_andCanClose() {
        ConfigurableApplicationContext ctx = EcoGoApplication.start(
                "--spring.main.web-application-type=none",
                "--spring.main.lazy-initialization=true",
                "--spring.main.banner-mode=off",
                "--spring.profiles.active=test",
                "--spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.scheduling.SchedulingAutoConfiguration"
        );
        assertNotNull(ctx);
        assertTrue(ctx.isActive());
        ctx.close();
        assertFalse(ctx.isActive());
    }

    @Test
    void maybeAutoClose_enabled_closesContext() {
        System.setProperty("ecogo.autoclose", "true");
        ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);

        EcoGoApplication.maybeAutoClose(ctx);

        verify(ctx).close();
    }

    @Test
    void maybeAutoClose_disabled_doesNotCloseContext() {
        System.setProperty("ecogo.autoclose", "false");
        ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);

        EcoGoApplication.maybeAutoClose(ctx);

        verify(ctx, never()).close();
    }

    @Test
    void maybeAutoClose_nullContext_noThrow() {
        System.setProperty("ecogo.autoclose", "true");
        assertDoesNotThrow(() -> EcoGoApplication.maybeAutoClose(null));
    }
}

