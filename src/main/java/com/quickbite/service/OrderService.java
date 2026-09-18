package com.quickbite.service;

import com.quickbite.concurrency.ConcurrencyMonitorService;
import com.quickbite.concurrency.OrderProcessingSimulator;
import com.quickbite.dao.OrderDAO;
import com.quickbite.model.CartItem;
import com.quickbite.model.Order;
import com.quickbite.model.OrderItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service managing order transactions, lifecycle transitions, and analytics.
 */
public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderProcessingSimulator simulator = OrderProcessingSimulator.getInstance();
    private final ConcurrencyMonitorService monitor = ConcurrencyMonitorService.getInstance();

    public Order placeOrder(int userId, int restaurantId, List<CartItem> cartItems,
                            String deliveryAddress, String paymentMethod, double deliveryFee,
                            boolean startAsyncSimulation) throws IllegalArgumentException {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart cannot be empty.");
        }
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery address is required.");
        }
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            paymentMethod = "Cash on Delivery";
        }

        double subtotal = 0.0;
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            subtotal += cartItem.getSubtotal();
            OrderItem item = new OrderItem(0, 0, cartItem.getFoodItem().getId(),
                    cartItem.getFoodItem().getName(),
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice());
            orderItems.add(item);
        }

        double totalAmount = subtotal + deliveryFee;

        Order order = new Order();
        order.setUserId(userId);
        order.setRestaurantId(restaurantId);
        order.setTotalAmount(totalAmount);
        order.setDeliveryFee(deliveryFee);
        order.setStatus(Order.STATUS_PLACED);
        order.setDeliveryAddress(deliveryAddress.trim());
        order.setPaymentMethod(paymentMethod);
        order.setItems(orderItems);

        boolean success = orderDAO.createOrder(order);
        if (!success) {
            throw new RuntimeException("Could not place order. Transaction failed in database.");
        }

        monitor.logEvent("OrderService", "Placed Order #" + order.getId() + " ($" + String.format("%.2f", totalAmount) + ") successfully.");

        // Optionally trigger background asynchronous simulation
        if (startAsyncSimulation) {
            simulator.startOrderSimulation(order.getId(), false);
        }

        return order;
    }

    public Order getOrder(int orderId) {
        return orderDAO.getById(orderId);
    }

    public List<Order> getCustomerOrders(int userId) {
        return orderDAO.getByUserId(userId);
    }

    public List<Order> getRestaurantOrders(int restaurantId) {
        return orderDAO.getByRestaurantId(restaurantId);
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        boolean updated = orderDAO.updateStatus(orderId, newStatus);
        if (updated) {
            monitor.logEvent("OrderService", "Manually updated Order #" + orderId + " to status: " + newStatus);
        }
        return updated;
    }

    public boolean cancelOrder(int orderId) {
        Order order = orderDAO.getById(orderId);
        if (order == null) return false;

        // Can only cancel if PLACED or CONFIRMED
        if (Order.STATUS_PLACED.equals(order.getStatus()) || Order.STATUS_CONFIRMED.equals(order.getStatus())) {
            return orderDAO.updateStatus(orderId, Order.STATUS_CANCELLED);
        } else {
            throw new IllegalStateException("Order cannot be cancelled once it is in preparation or out for delivery.");
        }
    }

    public Map<String, Object> getRestaurantStatistics(int restaurantId) {
        return orderDAO.getRevenueStatistics(restaurantId);
    }
}
