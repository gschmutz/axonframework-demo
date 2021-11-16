package io.axoniq.demo.shoppingcart.api

import org.axonframework.modelling.command.TargetAggregateIdentifier

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

// Commands

data class StartShoppingCartCommand(@TargetAggregateIdentifier val id: String, val customer: String)
data class AddArticleCommand(@TargetAggregateIdentifier val id: String, val article: String)
data class RemoveArticleCommand(@TargetAggregateIdentifier val id: String, val article: String)
data class CancelShoppingCartCommand(@TargetAggregateIdentifier val id: String)

// Events

data class ShoppingCartStartedEvent(val id: String, val customer: String)
data class ArticleAddedEvent(val id: String, val article: String, val price: Double)
data class ArticleRemovedEvent(val id: String, val article: String)
data class ShoppingCartCanceledEvent(val id: String)

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
