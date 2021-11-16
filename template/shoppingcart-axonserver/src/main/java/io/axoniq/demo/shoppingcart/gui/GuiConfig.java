package io.axoniq.demo.shoppingcart.gui;

import io.axoniq.demo.shoppingcart.api.CountShoppingCartSummariesQuery;
import io.axoniq.demo.shoppingcart.api.CountShoppingCartSummariesResponse;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("gui")
public class GuiConfig {

    @EventListener(ApplicationReadyEvent.class)
    public void helloHub(ApplicationReadyEvent event) {
        QueryGateway queryGateway = event.getApplicationContext()
                                         .getBean(QueryGateway.class);

        queryGateway.query(new CountShoppingCartSummariesQuery(), CountShoppingCartSummariesResponse.class);
    }
}
