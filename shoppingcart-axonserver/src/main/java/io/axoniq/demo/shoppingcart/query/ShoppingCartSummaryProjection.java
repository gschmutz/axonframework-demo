package io.axoniq.demo.shoppingcart.query;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.axoniq.demo.shoppingcart.api.*;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@Profile("query")
@Service
@ProcessingGroup("shoppingcart-summary")
public class ShoppingCartSummaryProjection {

    private final EntityManager entityManager;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public ShoppingCartSummaryProjection(EntityManager entityManager,
                                         @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") QueryUpdateEmitter queryUpdateEmitter) {
        this.entityManager = entityManager;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

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

        queryUpdateEmitter.emit(CountShoppingCartSummariesQuery.class,
                                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                                new CountChangedUpdate());
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

    @SuppressWarnings("unused")
    @QueryHandler
    public List<ShoppingCartSummary> handle(FetchShoppingCartSummariesQuery query) {
        TypedQuery<ShoppingCartSummary> jpaQuery = entityManager.createNamedQuery("CardSummary.fetch", ShoppingCartSummary.class);
        jpaQuery.setParameter("idStartsWith", query.getFilter().getIdStartsWith());
        jpaQuery.setFirstResult(query.getOffset());
        jpaQuery.setMaxResults(query.getLimit());
        return jpaQuery.getResultList();
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountShoppingCartSummariesResponse handle(CountShoppingCartSummariesQuery query) {
        TypedQuery<Long> jpaQuery = entityManager.createNamedQuery("CardSummary.count", Long.class);
        jpaQuery.setParameter("idStartsWith", query.getFilter().getIdStartsWith());
        return new CountShoppingCartSummariesResponse(jpaQuery.getSingleResult().intValue(), Instant.now().toEpochMilli());
    }
}
