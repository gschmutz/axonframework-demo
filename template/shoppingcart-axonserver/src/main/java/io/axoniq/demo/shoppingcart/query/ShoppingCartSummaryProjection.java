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
