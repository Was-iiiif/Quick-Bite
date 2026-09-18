package com.quickbite.concurrency;

import javafx.application.Platform;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Service to log and broadcast concurrency events, thread state changes,
 * and lock acquisitions for the live Concurrency Monitor GUI.
 */
public class ConcurrencyMonitorService {
    private static final ConcurrencyMonitorService INSTANCE = new ConcurrencyMonitorService();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final List<String> eventLogs = Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<String>> logListeners = new ArrayList<>();

    private ConcurrencyMonitorService() {}

    public static ConcurrencyMonitorService getInstance() {
        return INSTANCE;
    }

    public void logEvent(String source, String message) {
        String logEntry = String.format("[%s] [%s | Thread: %s] %s",
                LocalTime.now().format(TIME_FMT),
                source,
                Thread.currentThread().getName(),
                message);

        eventLogs.add(logEntry);
        System.out.println(logEntry);

        // Notify listeners on JavaFX Application Thread if Toolkit is initialized
        try {
            Platform.runLater(() -> {
                for (Consumer<String> listener : logListeners) {
                    try {
                        listener.accept(logEntry);
                    } catch (Exception ignored) {}
                }
            });
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit not initialized yet (e.g. during headless testing or startup)
        }
    }

    public synchronized void addLogListener(Consumer<String> listener) {
        logListeners.add(listener);
    }

    public synchronized void removeLogListener(Consumer<String> listener) {
        logListeners.remove(listener);
    }

    public List<String> getRecentLogs() {
        synchronized (eventLogs) {
            return new ArrayList<>(eventLogs);
        }
    }
}
