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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
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

    @Value("${app.admin-dashboard.install-dependencies:true}")
    private boolean installDependencies;

    @Value("${app.admin-dashboard.start-timeout-seconds:25}")
    private long startTimeoutSeconds;

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

            if (!isSupportedNodeVersion(adminDir, errLog)) {
                return;
            }

            if (installDependencies && !installDependencies(adminDir, outLog, errLog)) {
                return;
            }

            ProcessBuilder builder = new ProcessBuilder(findExecutable("node"), "src/index.js");
            builder.directory(adminDir.toFile());
            builder.environment().putIfAbsent("ADMIN_PORT", String.valueOf(port));
            configureDatabaseEnvironment(builder.environment());
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outLog.toFile()));
            builder.redirectError(ProcessBuilder.Redirect.appendTo(errLog.toFile()));

            process = builder.start();
            log.info("Starting AdminJS dashboard at http://localhost:{}/admin, pid={}", port, process.pid());
            log.info("AdminJS logs: {} and {}", outLog, errLog);
            if (waitForPort(port, Duration.ofSeconds(startTimeoutSeconds))) {
                log.info("AdminJS dashboard started at http://localhost:{}/admin, pid={}", port, process.pid());
                return;
            }

            if (process.isAlive()) {
                log.warn("AdminJS process is running but port {} did not open within {} seconds. Check {}", port, startTimeoutSeconds, errLog);
                return;
            }

            log.warn("AdminJS dashboard exited before opening port {}. Last error log:\n{}", port, tail(errLog, 25));
        } catch (IOException exception) {
            log.warn("Could not start AdminJS dashboard. Run manually: cd {} && pnpm install --frozen-lockfile && pnpm start", adminDir, exception);
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

    private boolean isSupportedNodeVersion(Path adminDir, Path errLog) throws IOException {
        CommandResult result = runCommand(List.of(findExecutable("node"), "--version"), adminDir, null, errLog);
        if (result.exitCode() != 0) {
            log.warn("Node.js was not found. Install Node.js 20.10+ before starting AdminJS.");
            return false;
        }

        String version = result.output().trim();
        if (isNodeAtLeast(version, 20, 10)) {
            return true;
        }

        log.warn("AdminJS requires Node.js 20.10+ but found {}. Upgrade Node on this machine, then restart the backend.", version);
        return false;
    }

    private boolean installDependencies(Path adminDir, Path outLog, Path errLog) throws IOException {
        List<String> installCommand = buildInstallCommand(adminDir);
        if (installCommand.isEmpty()) {
            log.warn("Could not find pnpm or corepack. Install pnpm, or run `corepack enable`, then restart the backend.");
            return false;
        }

        log.info("Checking AdminJS dependencies with `{}`", String.join(" ", installCommand));
        CommandResult result = runCommand(installCommand, adminDir, outLog, errLog);
        if (result.exitCode() == 0) {
            return true;
        }

        log.warn("AdminJS dependency install failed with exit code {}. Last error log:\n{}", result.exitCode(), tail(errLog, 25));
        return false;
    }

    private List<String> buildInstallCommand(Path adminDir) throws IOException {
        List<String> pnpm = List.of(findExecutable("pnpm"));
        if (isCommandAvailable(pnpm, adminDir)) {
            return List.of(pnpm.get(0), "install", "--frozen-lockfile");
        }

        List<String> corepack = List.of(findExecutable("corepack"));
        if (isCommandAvailable(corepack, adminDir)) {
            return List.of(corepack.get(0), "pnpm", "install", "--frozen-lockfile");
        }

        List<String> npm = List.of(findExecutable("npm"));
        if (isCommandAvailable(npm, adminDir) && !Files.exists(adminDir.resolve("pnpm-lock.yaml"))) {
            return List.of(npm.get(0), "install");
        }

        return List.of();
    }

    private boolean isCommandAvailable(List<String> command, Path workingDirectory) throws IOException {
        List<String> versionCommand = new ArrayList<>(command);
        versionCommand.add("--version");
        try {
            return runCommand(versionCommand, workingDirectory, null, null).exitCode() == 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private CommandResult runCommand(List<String> command, Path workingDirectory, Path outLog, Path errLog) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        if (outLog != null) {
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outLog.toFile()));
        } else {
            builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
        }
        if (errLog != null) {
            builder.redirectError(ProcessBuilder.Redirect.appendTo(errLog.toFile()));
        } else {
            builder.redirectError(ProcessBuilder.Redirect.PIPE);
        }

        Process commandProcess = builder.start();
        String output = "";
        if (outLog == null) {
            output = new String(commandProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            boolean finished = commandProcess.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                commandProcess.destroyForcibly();
                return new CommandResult(124, output);
            }
            return new CommandResult(commandProcess.exitValue(), output);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            commandProcess.destroyForcibly();
            return new CommandResult(130, output);
        }
    }

    private boolean waitForPort(int targetPort, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isPortOpen(targetPort)) {
                return true;
            }
            if (process != null && !process.isAlive()) {
                return false;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isPortOpen(int targetPort) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", targetPort), 300);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean isNodeAtLeast(String version, int requiredMajor, int requiredMinor) {
        String normalized = version.replaceFirst("^v", "").trim();
        String[] parts = normalized.split("\\.");
        if (parts.length < 2) {
            return false;
        }

        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            return major > requiredMajor || (major == requiredMajor && minor >= requiredMinor);
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private String findExecutable(String command) {
        if (!isWindows()) {
            return command;
        }
        if ("node".equals(command)) {
            return "node.exe";
        }
        return command + ".cmd";
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    private String tail(Path file, int lines) {
        if (!Files.exists(file)) {
            return "";
        }

        try {
            List<String> allLines = Files.readAllLines(file);
            int start = Math.max(0, allLines.size() - lines);
            return String.join(System.lineSeparator(), allLines.subList(start, allLines.size()));
        } catch (IOException exception) {
            return "Could not read " + file + ": " + exception.getMessage();
        }
    }

    private record CommandResult(int exitCode, String output) {
    }
}
