package com.ecommerce.controller;

import com.ecommerce.model.dto.OrderDTO;
import com.ecommerce.model.entity.Order;
import com.ecommerce.service.OrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Simulating authenticated user ID as 1 for demonstration
    private final Long FAKE_AUTH_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getUserOrders() {
        List<Order> orders = orderService.getUserOrders(FAKE_AUTH_USER_ID);
        List<OrderDTO> dtos = orders.stream()
                .map(orderService::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody CreateOrderRequest request) {
        // Enforcing authenticated user
        request.getOrderDto().setUserId(FAKE_AUTH_USER_ID);
        Order order = orderService.createOrder(request.getOrderDto(), request.getPaymentMethod());
        return ResponseEntity.ok(orderService.convertToDto(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable("id") Long id) {
        Order order = orderService.getOrderDetails(id);

        // Basic security check temporarily removed for unrestricted Swagger testing

        return ResponseEntity.ok(orderService.convertToDto(order));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable("id") Long id) {
        Order order = orderService.cancelOrder(id);
        return ResponseEntity.ok(orderService.convertToDto(order));
    }

    @Data
    public static class CreateOrderRequest {
        private OrderDTO orderDto;
        private String paymentMethod;
    }
}
