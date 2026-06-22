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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AgenticRagProcessManager {

    @Value("${app.agentic-rag.enabled:true}")
    private boolean enabled;

    @Value("${app.agentic-rag.port:8000}")
    private int port;

    @Value("${app.agentic-rag.path:ai_service}")
    private String servicePath;

    @Value("${app.agentic-rag.install-dependencies:true}")
    private boolean installDependencies;

    @Value("${app.agentic-rag.start-timeout-seconds:90}")
    private long startTimeoutSeconds;

    @Value("${app.ai.gemini.api-key:}")
    private String geminiApiKey;

    private Process process;

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void startAgenticRag() {
        if (!enabled) {
            log.info("Agentic RAG auto-start disabled");
            return;
        }
        if (isPortOpen(port)) {
            log.info("Agentic RAG already running at http://localhost:{}", port);
            return;
        }

        Path root = Path.of("").toAbsolutePath();
        Path serviceDir = root.resolve(servicePath).normalize();
        Path requirements = serviceDir.resolve("requirements.txt");
        Path apiEntry = serviceDir.resolve("src").resolve("api.py");
        if (!Files.exists(requirements) || !Files.exists(apiEntry)) {
            log.warn("Agentic RAG source not found at {}", serviceDir);
            return;
        }

        try {
            Files.createDirectories(root.resolve("target"));
            Path outLog = root.resolve("target").resolve("agentic-rag.out.log");
            Path errLog = root.resolve("target").resolve("agentic-rag.err.log");
            Path python = preparePython(serviceDir, requirements, outLog, errLog);
            if (python == null) {
                return;
            }

            ProcessBuilder builder = new ProcessBuilder(
                    python.toString(), "-m", "uvicorn", "src.api:app",
                    "--host", "127.0.0.1", "--port", String.valueOf(port)
            );
            builder.directory(serviceDir.toFile());
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                builder.environment().put("GEMINI_API_KEY", geminiApiKey);
            }
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(outLog.toFile()));
            builder.redirectError(ProcessBuilder.Redirect.appendTo(errLog.toFile()));
            process = builder.start();

            log.info("Starting Agentic RAG at http://localhost:{}, pid={}", port, process.pid());
            if (waitForPort(Duration.ofSeconds(startTimeoutSeconds))) {
                log.info("Agentic RAG started at http://localhost:{}, pid={}", port, process.pid());
            } else if (process.isAlive()) {
                log.warn("Agentic RAG is still starting after {} seconds. Check {}", startTimeoutSeconds, errLog);
            } else {
                log.warn("Agentic RAG exited before opening port {}. Last error log:\n{}", port, tail(errLog, 30));
            }
        } catch (IOException exception) {
            log.warn("Could not auto-start Agentic RAG from {}", serviceDir, exception);
        }
    }

    @PreDestroy
    public synchronized void stopAgenticRag() {
        if (process == null || !process.isAlive()) {
            return;
        }
        log.info("Stopping Agentic RAG, pid={}", process.pid());
        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private Path preparePython(Path serviceDir, Path requirements, Path outLog, Path errLog) throws IOException {
        Path venvDir = serviceDir.resolve(".venv");
        Path venvPython = venvPython(venvDir);
        if (!Files.exists(venvPython)) {
            String systemPython = findSystemPython(serviceDir);
            if (systemPython == null) {
                log.warn("Python 3 was not found. Install Python 3.10+ to run Agentic RAG.");
                return null;
            }
            log.info("Creating Agentic RAG virtual environment at {}", venvDir);
            if (run(List.of(systemPython, "-m", "venv", venvDir.toString()), serviceDir, outLog, errLog) != 0) {
                log.warn("Could not create Agentic RAG virtual environment. Check {}", errLog);
                return null;
            }
        }

        Path stamp = venvDir.resolve(".requirements-installed");
        boolean dependenciesChanged = !Files.exists(stamp)
                || Files.getLastModifiedTime(requirements).compareTo(Files.getLastModifiedTime(stamp)) > 0;
        if (installDependencies && dependenciesChanged) {
            log.info("Installing Agentic RAG Python dependencies");
            int exitCode = run(
                    List.of(venvPython.toString(), "-m", "pip", "install", "-r", requirements.toString()),
                    serviceDir,
                    outLog,
                    errLog
            );
            if (exitCode != 0) {
                log.warn("Agentic RAG dependency install failed with exit code {}. Check {}", exitCode, errLog);
                return null;
            }
            Files.writeString(stamp, "ok", StandardCharsets.UTF_8);
        }
        return venvPython;
    }

    private String findSystemPython(Path workingDirectory) {
        for (String candidate : List.of(isWindows() ? "python.exe" : "python3", "python")) {
            try {
                if (run(List.of(candidate, "--version"), workingDirectory, null, null) == 0) {
                    return candidate;
                }
            } catch (IOException ignored) {
                // Try the next executable name.
            }
        }
        return null;
    }

    private Path venvPython(Path venvDir) {
        return isWindows()
                ? venvDir.resolve("Scripts").resolve("python.exe")
                : venvDir.resolve("bin").resolve("python");
    }

    private int run(List<String> command, Path workingDirectory, Path outLog, Path errLog) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectOutput(outLog == null ? ProcessBuilder.Redirect.DISCARD : ProcessBuilder.Redirect.appendTo(outLog.toFile()));
        builder.redirectError(errLog == null ? ProcessBuilder.Redirect.DISCARD : ProcessBuilder.Redirect.appendTo(errLog.toFile()));
        Process commandProcess = builder.start();
        try {
            if (!commandProcess.waitFor(10, TimeUnit.MINUTES)) {
                commandProcess.destroyForcibly();
                return 124;
            }
            return commandProcess.exitValue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            commandProcess.destroyForcibly();
            return 130;
        }
    }

    private boolean waitForPort(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (isPortOpen(port)) {
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

    private String tail(Path file, int lineCount) {
        if (!Files.exists(file)) {
            return "(no log file)";
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return String.join(System.lineSeparator(), lines.subList(Math.max(0, lines.size() - lineCount), lines.size()));
        } catch (IOException exception) {
            return "(could not read log file)";
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
