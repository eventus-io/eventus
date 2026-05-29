package io.eventus.examples.modulith.fulfillment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fulfillment")
public class FulfillmentController {

    private final FulfillmentService fulfillment;

    public FulfillmentController(FulfillmentService fulfillment) {
        this.fulfillment = fulfillment;
    }

    @PostMapping("/{orderId}/deliver")
    public ResponseEntity<Void> deliver(@PathVariable String orderId,
                                        @RequestParam(defaultValue = "TRK-MANUAL") String tracking) {
        fulfillment.markDelivered(orderId, tracking);
        return ResponseEntity.ok().build();
    }
}
