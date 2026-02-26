package com.ecommerce.service;

import com.ecommerce.exception.PaymentFailedException;
import com.ecommerce.model.entity.Order;
import com.ecommerce.model.entity.Payment;
import com.ecommerce.model.enums.PaymentStatus;
import com.ecommerce.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // We use Requires_NEW to simulate a call to an external payment provider
    // Although in this monolithic demo, we might just keep it in the same
    // transaction
    // depending on the design. Standard REQUIRED is fine here.
    @Transactional(propagation = Propagation.REQUIRED)
    public Payment processPayment(Order order, String paymentMethod) {
        log.info("Processing payment for order: " + order.getOrderNumber());

        // Simulate payment gateway failure
        if ("INVALID_METHOD".equals(paymentMethod)) {
            throw new PaymentFailedException("Payment method rejected by gateway");
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(paymentMethod)
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-" + UUID.randomUUID().toString())
                .build();

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for order"));
    }
}
