package io.axoniq.demo.shoppingcart.command;

import io.axoniq.demo.shoppingcart.api.*;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import java.util.*;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Profile("command")
@Aggregate  // (cache = "shoppingCartCache")
public class ShoppingCartAggregate {
}