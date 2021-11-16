package io.axoniq.demo.shoppingcart.command;

import io.axoniq.demo.shoppingcart.api.*;
import lombok.Builder;
import lombok.Data;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventhandling.DomainEventSequenceAware;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.util.*;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Profile("command")
@Aggregate  // (cache = "shoppingCartCache")
public class ShoppingCartAggregate {

    @AggregateIdentifier
    private String cartId;
    private String customer;
    private Map<String, ShoppingCartItem> items = null;

    @CommandHandler
    public ShoppingCartAggregate(StartShoppingCartCommand command) {
        System.out.println("Command ========>: " + command);
        if (command.getCustomer() == null || command.getCustomer().isEmpty()) {
            throw new IllegalArgumentException("customer must be specfied!");
        }
        apply(new ShoppingCartStartedEvent(command.getId(), command.getCustomer()));
    }

    @CommandHandler
    public void handle(AddArticleCommand command) {
        System.out.println("Command ========>: " + command);
        if (command.getArticle() == null) {
            throw new IllegalArgumentException("article must be specified");
        }

        if (items.containsKey(command.getArticle())) {
            throw new IllegalArgumentException("article is already in the cart");
        }

        apply(new ArticleAddedEvent(cartId, command.getArticle(), 0.0));
    }

    @CommandHandler
    public void handle(RemoveArticleCommand command) {
        System.out.println("Command ========>: " + command);

        if (items.containsKey(command.getArticle())) {
            apply(new ArticleRemovedEvent(cartId, command.getArticle()));
        }

    }

    @EventSourcingHandler
    public void on(ShoppingCartStartedEvent event) {
        System.out.println(event);

        cartId = event.getId();
        customer = event.getCustomer();
        items = new HashMap<>();
    }

    @EventSourcingHandler
    public void on(ArticleAddedEvent event) {
        System.out.println(event);

        if (!items.containsKey(event.getArticle())) {
            items.put(event.getArticle(), ShoppingCartItem.builder().price(event.getPrice()).build());
        }
    }

    @EventSourcingHandler
    public void on(ArticleRemovedEvent event) {
        System.out.println(event);

        ShoppingCartItem item = items.remove(event.getArticle());
    }

    public ShoppingCartAggregate() {
        // Required by Axon to construct an empty instance to initiate Event Sourcing.
    }
}

@Data
@Builder
class ShoppingCartItem {
    private String article;
    private Double price;
}
