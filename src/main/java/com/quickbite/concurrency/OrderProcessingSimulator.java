package com.quickbite.concurrency;

import com.quickbite.dao.DeliveryDAO;
import com.quickbite.dao.OrderDAO;
import com.quickbite.model.Delivery;
import com.quickbite.model.Order;
import javafx.application.Platform;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Handles concurrent background order-processing tasks using ExecutorService and Runnable workers.
 * Manages the order lifecycle progression asynchronously without freezing the JavaFX application thread.
 */
public class OrderProcessingSimulator {
    private static OrderProcessingSimulator instance;

    // 4-thread pool as recommended in design specifications
    private final ExecutorService executorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
        private int count = 1;
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "QuickBite-Worker-" + (count++));
            t.setDaemon(true); // Allows JVM to exit cleanly
            return t;
        }
    });

    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final DeliveryDriverPool driverPool = DeliveryDriverPool.getInstance();
    private final ConcurrencyMonitorService monitor = ConcurrencyMonitorService.getInstance();

    private BiConsumer<Integer, String> statusUpdateCallback;

    private OrderProcessingSimulator() {}

    public static synchronized OrderProcessingSimulator getInstance() {
        if (instance == null) {
            instance = new OrderProcessingSimulator();
        }
        return instance;
    }

    public void setStatusUpdateCallback(BiConsumer<Integer, String> callback) {
        this.statusUpdateCallback = callback;
    }

    /**
     * Submits an asynchronous background simulation pipeline for an order.
     * Progresses through lifecycle: PLACED -> CONFIRMED -> PREPARING -> READY -> OUT_FOR_DELIVERY -> DELIVERED.
     *
     * @param orderId ID of order to simulate
     * @param autoAdvanceAll If true, automatically progresses through all stages with timed delays;
     *                       if false, simulates kitchen prep then halts at READY for driver staff.
     */
    public void startOrderSimulation(int orderId, boolean autoAdvanceAll) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    monitor.logEvent("OrderProcessor", "Started asynchronous processing pipeline for Order #" + orderId);

                    // Step 1: PLACED -> CONFIRMED (delay ~2s)
                    Thread.sleep(2000);
                    advanceStatus(orderId, Order.STATUS_CONFIRMED);

                    // Step 2: CONFIRMED -> PREPARING (delay ~3s)
                    Thread.sleep(3000);
                    advanceStatus(orderId, Order.STATUS_PREPARING);

                    // Step 3: PREPARING -> READY (delay ~4s)
                    Thread.sleep(4000);
                    advanceStatus(orderId, Order.STATUS_READY);

                    // Step 4: Acquire driver from shared resource pool
                    int driverId = driverPool.acquireDriver(orderId);
                    if (driverId != -1) {
                        monitor.logEvent("OrderProcessor", "Driver #" + driverId + " successfully dispatched for Order #" + orderId);
                    } else {
                        monitor.logEvent("OrderProcessor", "No driver available yet for Order #" + orderId + ". Waiting in queue.");
                    }

                    if (autoAdvanceAll) {
                        // Step 5: READY -> OUT_FOR_DELIVERY (delay ~4s)
                        Thread.sleep(4000);
                        advanceStatus(orderId, Order.STATUS_OUT_FOR_DELIVERY);
                        if (driverId != -1) {
                            Delivery deliv = deliveryDAO.getByOrderId(orderId);
                            if (deliv != null) {
                                deliveryDAO.updateStatus(deliv.getId(), Delivery.STATUS_PICKED_UP);
                            }
                        }

                        // Step 6: OUT_FOR_DELIVERY -> DELIVERED (delay ~5s)
                        Thread.sleep(5000);
                        advanceStatus(orderId, Order.STATUS_DELIVERED);
                        if (driverId != -1) {
                            Delivery deliv = deliveryDAO.getByOrderId(orderId);
                            if (deliv != null) {
                                deliveryDAO.updateStatus(deliv.getId(), Delivery.STATUS_DELIVERED);
                            }
                            driverPool.releaseDriver(driverId);
                        }
                        monitor.logEvent("OrderProcessor", "Order #" + orderId + " fully delivered! Task completed.");
                    } else {
                        monitor.logEvent("OrderProcessor", "Order #" + orderId + " is READY for pickup. Awaiting delivery staff action.");
                    }

                } catch (InterruptedException e) {
                    monitor.logEvent("OrderProcessor", "Simulation interrupted for Order #" + orderId);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    monitor.logEvent("OrderProcessor", "Error during simulation for Order #" + orderId + ": " + e.getMessage());
                }
            }
        });
    }

    private void advanceStatus(int orderId, String newStatus) {
        orderDAO.updateStatus(orderId, newStatus);
        monitor.logEvent("OrderProcessor", "Order #" + orderId + " transitioned to status: " + newStatus);

        // Notify UI on JavaFX thread via Platform.runLater if toolkit is initialized
        try {
            Platform.runLater(() -> {
                if (statusUpdateCallback != null) {
                    statusUpdateCallback.accept(orderId, newStatus);
                }
            });
        } catch (IllegalStateException ignored) {}
    }

    public void submitCustomTask(Runnable task) {
        executorService.submit(task);
    }

    public void shutdown() {
        monitor.logEvent("OrderProcessor", "Shutting down thread pool executor...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
