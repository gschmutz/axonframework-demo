# Event Sourcing & CQRS mit Axon Demo

## Showcase finished solution

Start the application 

```bash
mvn spring-boot:run
```

Navigate to <http://localhost:8080>

Add a new shopping cart and check the output on the console

```
Command ========>: StartShoppingCartCommand(id=1, customer=Guido)
ShoppingCartStartedEvent(id=1, customer=Guido)
```

Add a new article

```
ShoppingCartStartedEvent(id=1, customer=Guido)
Command ========>: AddArticleCommand(id=1, article=IPad)
ArticleAddedEvent(id=1, article=IPad, price=0.0)
```

## Commands & Events (Event Sourcing)

```java
// Commands

data class StartShoppingCartCommand(@TargetAggregateIdentifier val id: String, val customer: String)
data class AddArticleCommand(@TargetAggregateIdentifier val id: String, val article: String)
data class RemoveArticleCommand(@TargetAggregateIdentifier val id: String, val article: String)
data class CancelShoppingCartCommand(@TargetAggregateIdentifier val id: String)
```

```java
// Events

data class ShoppingCartStartedEvent(val id: String, val customer: String)
data class ArticleAddedEvent(val id: String, val article: String, val price: Double)
data class ArticleRemovedEvent(val id: String, val article: String)
data class ShoppingCartCanceledEvent(val id: String)
```


```java
// Queries

data class ShoppingCartSummaryFilter(val idStartsWith: String = "")
class CountShoppingCartSummariesQuery(val filter: ShoppingCartSummaryFilter = ShoppingCartSummaryFilter()) {
    override fun toString(): String = "CountCardSummariesQuery"
}

data class FetchShoppingCartSummariesQuery(val offset: Int, val limit: Int, val filter: ShoppingCartSummaryFilter)
class CountChangedUpdate

// Query Responses

@Entity
@NamedQueries(
        NamedQuery(
                name = "CardSummary.fetch",
                query = "SELECT c FROM ShoppingCartSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%') ORDER BY c.id"
        ),
        NamedQuery(
                name = "CardSummary.count",
                query = "SELECT COUNT(c) FROM ShoppingCartSummary c WHERE c.id LIKE CONCAT(:idStartsWith, '%')"
        )
)
data class ShoppingCartSummary(@Id var id: String, var customer: String, var items: String) {
    constructor() : this("", "","")
}

data class CountShoppingCartSummariesResponse(val count: Int, val lastEvent: Long)
```

## Configuration

Add the following values to the `application.properties` file

```properties
# The name of this app:
spring.application.name=ShoppingCart-App-${spring.profiles.active}
server.port=8086

# Debugging on
logging.level.io.axoniq.demo=info
logging.level.root=info

# The default profiles are "all of them"
spring.profiles.active=command,query,gui

# Database specifics
spring.datasource.url=jdbc:h2:./database;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE
spring.jpa.hibernate.ddl-auto=update

# We look for Axon Server locally, unless we find a PCF Binding for AxonServer
axon.axonserver.servers=dataplatform:18124

# Management endpoints inclusion
management.endpoint.health.show-details=always
management.endpoints.web.exposure.include=*
```

Compile the project and run it. It should not work, due to command handler not being implemented.


## ShoppingCart Aggregate

Add the following code to `ShoppingCartAggregate`

```java
@Aggregate  // (cache = "shoppingCartCache")
public class ShoppingCartAggregate {

    @AggregateIdentifier
    private String cartId;
    private String customer;
    private Map<String, ShoppingCartItem> items = null;
    
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
```

Add command handler to `ShoppingCartAggregate`

```java
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
```

Add event sourcing handler `ShoppingCartAggregate`
 
```java
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
```

Run the application 

```bash
mvn spring-boot:run
```

Navigate to <http://localhost:8086>

## Shopping Cart Summary Projection (CQRS)

Add event handler to `ShoppingCartSummaryProjection`

```java
    /*
     * Update our read model by inserting the new card. This is done so that upcoming regular
     * (non-subscription) queries get correct data.
     *
     * Serve the subscribed queries by emitting an update. This reads as follows:
     * - to all current subscriptions of type CountCardSummariesQuery
     * - for which is true that the id of the gift card having been issued starts with the idStartWith string
     *   in the query's filter
     * - send a message that the count of queries matching this query has been changed.
     */
    @EventHandler
    public void on(ShoppingCartStartedEvent event) {
        entityManager.persist(new ShoppingCartSummary(event.getId(), event.getCustomer(), ""));

    }

    /*
     * Update our read model by updating the existing card. This is done so that upcoming regular
     * (non-subscription) queries get correct data.
     *
     * Serve the subscribed queries by emitting an update. This reads as follows:
     * - to all current subscriptions of type FetchCardSummariesQuery
     * - for which is true that the id of the gift card having been redeemed starts with the idStartWith string
     *   in the query's filter
     * - send a message containing the new state of this gift card summary
     */
    @EventHandler
    public void on(ArticleAddedEvent event) {
        ShoppingCartSummary summary = entityManager.find(ShoppingCartSummary.class, event.getId());

        List<String> items = new ArrayList<>();
        if (!summary.getItems().equals("")) {
            items = Splitter.on(",").trimResults().splitToList(summary.getItems());
        }
        items = Lists.newArrayList(items);
        items.add(event.getArticle());
        summary.setItems(Joiner.on(",").join(items));

        queryUpdateEmitter.emit(FetchShoppingCartSummariesQuery.class,
                                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                                summary);
    }

    /*
     * Update our read model by updating the existing card. This is done so that upcoming regular
     * (non-subscription) queries get correct data.
     *
     * Serve the subscribed queries by emitting an update. This reads as follows:
     * - to all current subscriptions of type FetchCardSummariesQuery
     * - for which is true that the id of the gift card having been redeemed starts with the idStartWith string
     *   in the query's filter
     * - send a message containing the new state of this gift card summary
     */
    @EventHandler
    public void on(ArticleRemovedEvent event) {
        ShoppingCartSummary summary = entityManager.find(ShoppingCartSummary.class, event.getId());

        List<String> items = Splitter.on(",").trimResults().splitToList(summary.getItems());
        items = Lists.newArrayList(items);
        items.remove(event.getArticle());
        summary.setItems(Joiner.on(",").join(items));

        queryUpdateEmitter.emit(FetchShoppingCartSummariesQuery.class,
                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                summary);
    }
```

## Show benefit of Event Sourcing

Add to `AddArticleCommand` command handler

```java
        if (customer.equals("GUIDO")) {
            System.out.println("this article is a good choice!");
        }
```

Add to `ShoppingCartStartedEvent` event sourcing handler

```java
        customer = event.getCustomer().toUpperCase();
```

## Running Application as Microservices

In 3 separate terminals, run


this command to run the command side

```bash
cd /Users/gus/workspace/GitHub/gschmutz/axon-example/shoppingcart-axonserver
mvn spring-boot:run -Dspring-boot.run.profiles=command
```

this command to run the query side

```bash
cd /Users/gus/workspace/GitHub/gschmutz/axon-example/shoppingcart-axonserver
mvn spring-boot:run -Dspring-boot.run.profiles=query
```

this command to run the gui side


```bash
cd /Users/gus/workspace/GitHub/gschmutz/axon-example/shoppingcart-axonserver
mvn spring-boot:run -Dspring-boot.run.profiles=gui
```


