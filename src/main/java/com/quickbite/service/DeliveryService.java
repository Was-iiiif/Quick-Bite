package com.quickbite.service;

import com.quickbite.concurrency.ConcurrencyMonitorService;
import com.quickbite.concurrency.DeliveryDriverPool;
import com.quickbite.dao.DeliveryDAO;
import com.quickbite.dao.OrderDAO;
import com.quickbite.model.Delivery;
import com.quickbite.model.Order;
import java.util.List;

/**
 * Service managing delivery assignments, driver status updates, and pool synchronization.
 */
public class DeliveryService {
    private final DeliveryDAO deliveryDAO = new DeliveryDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeliveryDriverPool driverPool = DeliveryDriverPool.getInstance();
    private final ConcurrencyMonitorService monitor = ConcurrencyMonitorService.getInstance();

    public List<Delivery> getStaffDeliveries(int staffId) {
        return deliveryDAO.getDeliveriesByStaffId(staffId);
    }

    public List<Delivery> getAllActiveDeliveries() {
        return deliveryDAO.getAllActiveDeliveries();
    }

    public boolean markPickedUp(int deliveryId, int orderId) {
        boolean delivUpdated = deliveryDAO.updateStatus(deliveryId, Delivery.STATUS_PICKED_UP);
        boolean orderUpdated = orderDAO.updateStatus(orderId, Order.STATUS_OUT_FOR_DELIVERY);

        if (delivUpdated && orderUpdated) {
            monitor.logEvent("DeliveryService", "Delivery #" + deliveryId + " for Order #" + orderId + " marked OUT_FOR_DELIVERY");
            return true;
        }
        return false;
    }

    public boolean markDelivered(int deliveryId, int orderId, int driverId) {
        boolean delivUpdated = deliveryDAO.updateStatus(deliveryId, Delivery.STATUS_DELIVERED);
        boolean orderUpdated = orderDAO.updateStatus(orderId, Order.STATUS_DELIVERED);

        if (delivUpdated && orderUpdated) {
            // Release driver back to synchronized pool
            driverPool.releaseDriver(driverId);
            monitor.logEvent("DeliveryService", "Delivery #" + deliveryId + " for Order #" + orderId + " marked DELIVERED. Driver #" + driverId + " released.");
            return true;
        }
        return false;
    }
}
