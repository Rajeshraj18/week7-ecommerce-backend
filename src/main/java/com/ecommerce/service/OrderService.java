package com.ecommerce.service;

import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.model.dto.OrderDTO;
import com.ecommerce.model.dto.OrderItemDTO;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.OrderItem;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.model.entity.Product;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.entity.Payment;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;

    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderDTO requestDto, String paymentMethod) {
        log.info("Transaction Started for new order");

        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Order order = Order.builder()
                .orderNumber("ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-"
                        + user.getId())
                .user(user)
                .status(OrderStatus.PENDING)
                .shippingAddress(requestDto.getShippingAddress())
                .build();

        // Process Order Items and Inventory
        for (OrderItemDTO itemDto : requestDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            log.info("Lock acquired for product ID: " + product.getId());

            if (product.getStock() < itemDto.getQuantity()) {
                throw new InsufficientStockException("Not enough stock for product: " + product.getName());
            }

            log.info("Stock updated: Product " + product.getId() + " (" + product.getStock() + " -> "
                    + (product.getStock() - itemDto.getQuantity()) + ")");
            product.decreaseStock(itemDto.getQuantity());
            productRepository.save(product); // using optimistic locking implicitly assuming standard JPA

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .price(product.getPrice()) // Snapshot current price
                    .build();

            order.addOrderItem(orderItem);
        }

        BigDecimal totalAmount = order.calculateTotal();
        order.setTotalAmount(totalAmount);
        log.info("Total calculated: $" + totalAmount);

        // Save the order along with items due to cascade
        Order savedOrder = orderRepository.save(order);
        log.info("Order created: " + savedOrder.getOrderNumber());

        // Process Payment internally in the same transaction
        Payment payment = paymentService.processPayment(savedOrder, paymentMethod);
        savedOrder.setPayment(payment);

        log.info("Transaction Committed");
        return savedOrder;
    }

    @Transactional(readOnly = true)
    public Order getOrderDetails(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        // Example uses pageable, but to simplify returning list:
        return orderRepository.findByUser(user, org.springframework.data.domain.Pageable.unpaged()).getContent();
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = getOrderDetails(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Restore stock
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
        }

        return orderRepository.save(order);
    }

    public OrderDTO convertToDto(Order order) {
        List<OrderItemDTO> items = order.getOrderItems().stream().map(item -> OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getPrice())
                .subtotal(item.getSubtotal())
                .build()).collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .items(items)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
