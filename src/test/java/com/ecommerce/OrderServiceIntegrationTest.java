package com.ecommerce;

import com.ecommerce.exception.InsufficientStockException;
import com.ecommerce.model.dto.OrderDTO;
import com.ecommerce.model.dto.OrderItemDTO;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.Payment;
import com.ecommerce.model.entity.Product;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.model.entity.Category;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.Role;
import com.ecommerce.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
// Depending on testing setup, we usually want to use an embedded database or
// rollback per test.
// By default, @Transactional on tests rolls back at the end.
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Long testProductId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Clear previous runs just in case, though @Transactional usually rolls back
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        User user = User.builder()
                .email("testuser@example.com")
                .password("pass")
                .name("Test User")
                .role(Role.USER)
                .build();
        user = userRepository.save(user);
        testUserId = user.getId();

        Category category = Category.builder()
                .name("Test Category")
                .build();
        category = categoryRepository.save(category);

        Product product = Product.builder()
                .name("Test Product")
                .price(BigDecimal.valueOf(99.99))
                .stock(100)
                .category(category)
                .isActive(true)
                .build();
        product = productRepository.save(product);
        testProductId = product.getId();
    }

    @Test
    @Transactional
    void testSuccessfulOrderCreation_CommitsTransactions() {
        // Given we have products seeded from flyway (ID 1 has 100 stock)
        Product initialProduct = productRepository.findById(testProductId).orElseThrow();
        Integer initialStock = initialProduct.getStock();

        OrderDTO orderRequest = new OrderDTO();
        orderRequest.setUserId(testUserId); // User from seed data
        orderRequest.setShippingAddress("123 Test St");

        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(testProductId);
        item.setQuantity(2);

        orderRequest.setItems(List.of(item));

        // When
        Order createdOrder = orderService.createOrder(orderRequest, "CREDIT_CARD");

        // Then
        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.getId()).isNotNull();

        // Verify Stock Deducted
        Product updatedProduct = productRepository.findById(testProductId).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 2);

        // Verify Order Saved
        Order dbOrder = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertThat(dbOrder.getOrderItems()).hasSize(1);

        // Verify Payment logic triggered and saved
        Payment dbPayment = paymentRepository.findByOrderId(createdOrder.getId()).orElseThrow();
        assertThat(dbPayment.getStatus().name()).isEqualTo("SUCCESS");
    }

    @Test
    @Transactional
    void testFailedOrderCreation_InsufficientStock_RollsBackTransactions() {
        // Given
        Product initialProduct = productRepository.findById(testProductId).orElseThrow();
        Integer initialStock = initialProduct.getStock();

        OrderDTO orderRequest = new OrderDTO();
        orderRequest.setUserId(testUserId);
        orderRequest.setShippingAddress("123 Test St");

        OrderItemDTO item = new OrderItemDTO();
        item.setProductId(testProductId);
        item.setQuantity(initialStock + 10); // Trying to buy more than available

        orderRequest.setItems(List.of(item));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(orderRequest, "CREDIT_CARD"))
                .isInstanceOf(InsufficientStockException.class);

        // Verify Rollback: Stock should remain untouched
        Product unchangedProduct = productRepository.findById(testProductId).orElseThrow();
        assertThat(unchangedProduct.getStock()).isEqualTo(initialStock);
    }
}
