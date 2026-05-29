package io.eventus.examples.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    private static final ApplicationModules modules =
            ApplicationModules.of(BookstoreApplication.class);

    @Test
    void documentModules() {
        // modules.verify() is intentionally omitted — the application contains
        // deliberate coupling violations (PaymentService→OrderService,
        // FulfillmentSaga→OrderService direct calls) to demonstrate Eventus
        // violation detection.
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
