package io.eventus.examples.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    private static final ApplicationModules modules =
            ApplicationModules.of(BookstoreApplication.class);

    @Test
    void modulesAreStructurallyValid() {
        modules.verify();
    }

    @Test
    void documentModules() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
