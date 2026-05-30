package io.eventus.examples.payments.risk;

import io.eventus.examples.payments.payment.PaymentAuthorized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final ApplicationEventPublisher events;

    public RiskService(ApplicationEventPublisher events) {
        this.events = events;
    }

    @ApplicationModuleListener
    public void on(PaymentAuthorized payment) {
        log.info("Running risk check for order {}", payment.orderId());

        // Simplified: always clears (real impl would call a fraud detection model)
        String score = "LOW";
        log.info("Risk cleared for order {} — score {}", payment.orderId(), score);
        events.publishEvent(new RiskCleared(payment.orderId(), score));
    }
}
