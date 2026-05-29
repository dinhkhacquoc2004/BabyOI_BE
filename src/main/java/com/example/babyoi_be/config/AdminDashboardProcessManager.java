package com.example.babyoi_be.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AdminDashboardProcessManager {

    @Value("${app.admin-dashboard.enabled:true}")
    private boolean enabled;

    @Value("${app.admin-dashboard.port:8090}")
    private int port;

    @Value("${app.admin-dashboard.path:admin-dashboard}")
    private String dashboardPath;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    private Process process;

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void startAdminDashboard() {
        if (!enabled) {
            log.info("AdminJS dashboard auto-start disabled");
            return;
        }

        if (isPortOpen(port)) {
            log.info("AdminJS dashboard already running at http://localhost:{}/admin", port);
            return;
        }

        Path root = Path.of("").toAbsolutePath();
        Path adminDir = root.resolve(dashboardPath).normalize();
        Path entryFile = adminDir.resolve("src").resolve("index.js");
        if (!Files.exists(entryFile)) {
            log.warn("AdminJS dashboard entry not found: {}", entryFile);
            return;
        }

        ensureEnvFile(adminDir);
        try {
            Files.createDirectories(root.resolve("target"));
            Path outLog = root.resolve("target").resolve("admin-dashboard.out.log");
            Path errLog = root.resolve("target").resolve("admin-dashboard.err.log");

            ProcessBuilder builder = new ProcessBuilder(buildNodeCommand());
            builder.directory(adminDir.toFile());
            configureDatabaseEnvironment(builder.environment());
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outLog.toFile()));
            builder.redirectError(ProcessBuilder.Redirect.appendTo(errLog.toFile()));

            process = builder.start();
            log.info("Starting AdminJS dashboard at http://localhost:{}/admin, pid={}", port, process.pid());
            log.info("AdminJS logs: {} and {}", outLog, errLog);
        } catch (IOException exception) {
            log.warn("Could not start AdminJS dashboard. Run manually: cd {} && node src/index.js", adminDir, exception);
        }
    }

    @PreDestroy
    public synchronized void stopAdminDashboard() {
        if (process == null || !process.isAlive()) {
            return;
        }

        log.info("Stopping AdminJS dashboard, pid={}", process.pid());
        process.destroy();
        try {
            boolean stopped = process.waitFor(Duration.ofSeconds(5).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!stopped) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private List<String> buildNodeCommand() {
        List<String> command = new ArrayList<>();
        command.add("node");
        command.add("src/index.js");
        return command;
    }

    private void configureDatabaseEnvironment(Map<String, String> environment) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            return;
        }

        environment.put("SPRING_DATASOURCE_URL", datasourceUrl);
        environment.put("SPRING_DATASOURCE_USERNAME", datasourceUsername);
        environment.put("SPRING_DATASOURCE_PASSWORD", datasourcePassword);

        String normalizedUrl = datasourceUrl.replaceFirst("^jdbc:", "");
        try {
            java.net.URI uri = java.net.URI.create(normalizedUrl);
            java.net.URI uriWithCredentials = new java.net.URI(
                    uri.getScheme(),
                    datasourceUsername + ":" + datasourcePassword,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            );
            environment.put("DATABASE_URL", uriWithCredentials.toString());
        } catch (Exception exception) {
            log.warn("Could not convert Spring datasource URL for AdminJS: {}", datasourceUrl, exception);
        }
    }

    private void ensureEnvFile(Path adminDir) {
        Path env = adminDir.resolve(".env");
        Path example = adminDir.resolve(".env.example");
        if (Files.exists(env) || !Files.exists(example)) {
            return;
        }

        try {
            Files.copy(example, env);
            log.info("Created AdminJS .env from .env.example");
        } catch (IOException exception) {
            log.warn("Could not create AdminJS .env from {}", example, exception);
        }
    }

    private boolean isPortOpen(int targetPort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", targetPort), 300);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
