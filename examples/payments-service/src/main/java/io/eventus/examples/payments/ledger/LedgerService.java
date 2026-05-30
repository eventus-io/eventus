package io.eventus.examples.payments.ledger;

import io.eventus.examples.payments.risk.RiskCleared;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final ApplicationEventPublisher events;

    public LedgerService(ApplicationEventPublisher events) {
        this.events = events;
    }

    @ApplicationModuleListener
    public void on(RiskCleared cleared) {
        String ref = "LDG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Settling funds for order {} — ledger ref {}", cleared.orderId(), ref);
        events.publishEvent(new FundsSettled(cleared.orderId(), ref));
    }
}
