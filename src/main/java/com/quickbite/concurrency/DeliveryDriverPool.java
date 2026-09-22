package com.quickbite.concurrency;

import com.quickbite.dao.DeliveryDAO;
import com.quickbite.dao.UserDAO;
import com.quickbite.model.DeliveryStaff;
import java.util.*;

/**
 * Demonstrates shared resource concurrency and thread synchronization.
 * Multiple concurrent orders compete to acquire available delivery drivers.
 * Uses synchronized critical sections to prevent duplicate driver assignment and race conditions.
 */
public class DeliveryDriverPool {
    private static DeliveryDriverPool instance;

    private final Set<Integer> availableDriverIds = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Map<Integer, Integer> activeDriverToOrderMap = Collections.synchronizedMap(new HashMap<>());
    private final UserDAO userDAO = new UserDAO();
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final ConcurrencyMonitorService monitor = ConcurrencyMonitorService.getInstance();

    private DeliveryDriverPool() {
        reloadDrivers();
    }

    public static synchronized DeliveryDriverPool getInstance() {
        if (instance == null) {
            instance = new DeliveryDriverPool();
        }
        return instance;
    }

    /**
     * Re-populates the driver pool from database records.
     */
    public synchronized void reloadDrivers() {
        List<DeliveryStaff> staffList = userDAO.getAllDeliveryStaff();
        for (DeliveryStaff staff : staffList) {
            if (!activeDriverToOrderMap.containsKey(staff.getId())) {
                availableDriverIds.add(staff.getId());
            }
        }
        monitor.logEvent("DriverPool", "Pool initialized with " + availableDriverIds.size() + " active drivers.");
    }

    /**
     * CRITICAL SECTION:
     * Synchronized method protecting the shared pool of delivery drivers.
     * Prevents race conditions where two simultaneous orders try to claim the same driver.
     *
     * @param orderId ID of the order needing delivery
     * @return Assigned driver ID, or -1 if all drivers are currently busy
     */
    public synchronized int acquireDriver(int orderId) {
        monitor.logEvent("DriverPool", "Thread requesting driver for Order #" + orderId + " [LOCK ACQUIRED]");
        try {
            // Check if this order already has an assigned driver
            for (Map.Entry<Integer, Integer> entry : activeDriverToOrderMap.entrySet()) {
                if (entry.getValue().equals(orderId)) {
                    monitor.logEvent("DriverPool", "Order #" + orderId + " already has Driver #" + entry.getKey());
                    return entry.getKey();
                }
            }

            if (availableDriverIds.isEmpty()) {
                monitor.logEvent("DriverPool", "RESOURCE EXHAUSTION: No available drivers for Order #" + orderId + ". Waiting in queue.");
                return -1;
            }

            // Pick first available driver
            Iterator<Integer> it = availableDriverIds.iterator();
            int selectedDriverId = it.next();
            it.remove(); // Remove from available set

            activeDriverToOrderMap.put(selectedDriverId, orderId);

            // Record assignment in persistent database
            deliveryDAO.createOrAssignDelivery(orderId, selectedDriverId);

            monitor.logEvent("DriverPool", "SUCCESS: Assigned Driver #" + selectedDriverId + " to Order #" + orderId +
                    ". Remaining available: " + availableDriverIds.size());
            return selectedDriverId;
        } finally {
            monitor.logEvent("DriverPool", "Exiting assignment lock for Order #" + orderId + " [LOCK RELEASED]");
        }
    }

    /**
     * Releases a driver back into the pool of available drivers upon delivery completion.
     * Synchronized to guarantee atomic state mutation.
     *
     * @param driverId ID of driver to return to pool
     */
    public synchronized void releaseDriver(int driverId) {
        Integer completedOrderId = activeDriverToOrderMap.remove(driverId);
        availableDriverIds.add(driverId);
        monitor.logEvent("DriverPool", "Driver #" + driverId + " finished Order #" +
                (completedOrderId != null ? completedOrderId : "N/A") + " and returned to pool. Available count: " + availableDriverIds.size());
    }

    public synchronized int getAvailableCount() {
        return availableDriverIds.size();
    }

    public synchronized int getBusyCount() {
        return activeDriverToOrderMap.size();
    }

    public synchronized Map<Integer, Integer> getActiveAssignments() {
        return new HashMap<>(activeDriverToOrderMap);
    }
}
