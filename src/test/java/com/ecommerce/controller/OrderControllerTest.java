package com.ecommerce.controller;

import com.ecommerce.model.dto.OrderDTO;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order order;
    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).name("Test User").build();

        order = Order.builder()
                .id(1L)
                .orderNumber("ORD-12345")
                .user(user)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(150.00))
                .build();

        orderDTO = OrderDTO.builder()
                .id(1L)
                .orderNumber("ORD-12345")
                .userId(1L)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(150.00))
                .build();
    }

    @Test
    void testGetUserOrders() throws Exception {
        when(orderService.getUserOrders(1L)).thenReturn(List.of(order));
        when(orderService.convertToDto(any(Order.class))).thenReturn(orderDTO);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-12345"));
    }

    @Test
    void testCreateOrder() throws Exception {
        when(orderService.createOrder(any(OrderDTO.class), anyString())).thenReturn(order);
        when(orderService.convertToDto(any(Order.class))).thenReturn(orderDTO);

        OrderController.CreateOrderRequest request = new OrderController.CreateOrderRequest();
        request.setOrderDto(orderDTO);
        request.setPaymentMethod("CREDIT_CARD");

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345"));
    }

    @Test
    void testGetOrderById() throws Exception {
        when(orderService.getOrderDetails(1L)).thenReturn(order);
        when(orderService.convertToDto(any(Order.class))).thenReturn(orderDTO);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345"));
    }

    @Test
    void testCancelOrder() throws Exception {
        Order cancelledOrder = Order.builder()
                .id(1L)
                .orderNumber("ORD-12345")
                .status(OrderStatus.CANCELLED)
                .build();

        OrderDTO cancelledDTO = OrderDTO.builder()
                .id(1L)
                .orderNumber("ORD-12345")
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder(1L)).thenReturn(cancelledOrder);
        when(orderService.convertToDto(any(Order.class))).thenReturn(cancelledDTO);

        mockMvc.perform(put("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
