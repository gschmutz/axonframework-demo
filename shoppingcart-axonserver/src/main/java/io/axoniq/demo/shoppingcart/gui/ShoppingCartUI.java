package io.axoniq.demo.shoppingcart.gui;

import com.vaadin.annotations.Push;
import com.vaadin.server.DefaultErrorHandler;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import io.axoniq.demo.shoppingcart.api.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Profile("gui")
@SpringUI
@Push
public class ShoppingCartUI extends UI {

    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;
    private ShoppingCartSummaryDataProvider shoppingCartSummaryDataProvider;
    private ScheduledFuture<?> updaterThread;

    public ShoppingCartUI(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        HorizontalLayout commandBar = new HorizontalLayout();
        commandBar.setWidth("100%");
        commandBar.addComponents(startShoppingPanel(), addArticlePanel(), removeArticlePanel());

        Grid<ShoppingCartSummary> summary = summaryGrid();

        HorizontalLayout statusBar = new HorizontalLayout();
        Label statusLabel = new Label("Status");
        statusBar.setDefaultComponentAlignment(Alignment.MIDDLE_RIGHT);
        statusBar.addComponent(statusLabel);
        statusBar.setWidth("100%");

        VerticalLayout layout = new VerticalLayout();
        layout.addComponents(commandBar, summary, statusBar);
        layout.setExpandRatio(summary, 1f);
        layout.setSizeFull();

        setContent(layout);

        UI.getCurrent().setErrorHandler(new DefaultErrorHandler() {
            @Override
            public void error(com.vaadin.server.ErrorEvent event) {
                Throwable cause = event.getThrowable();
                logger.error("An error occurred", cause);
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                Notification.show("Error", cause.getMessage(), Notification.Type.ERROR_MESSAGE);
            }
        });

        setPollInterval(1000);
        int offset = Page.getCurrent().getWebBrowser().getTimezoneOffset();
        // offset is in milliseconds
        ZoneOffset instantOffset = ZoneOffset.ofTotalSeconds(offset / 1000);
        StatusUpdater statusUpdater = new StatusUpdater(statusLabel, instantOffset);
        updaterThread = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(statusUpdater, 1000,
                                                                                5000, TimeUnit.MILLISECONDS);
        setPollInterval(1000);
        getSession().getSession().setMaxInactiveInterval(120);
        addDetachListener((DetachListener) detachEvent -> {
            logger.warn("Closing UI");
            updaterThread.cancel(true);
        });
    }

    private Panel startShoppingPanel() {
        TextField id = new TextField("ShoppingCart id");
        TextField customer = new TextField("Customer");
        Button submit = new Button("Submit");

        submit.addClickListener(evt -> {
            commandGateway.sendAndWait(new StartShoppingCartCommand(id.getValue(), customer.getValue()));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE)
                        .addCloseListener(e -> shoppingCartSummaryDataProvider.refreshAll());
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, customer, submit);
        form.setMargin(true);

        Panel panel = new Panel("Start ShoppingCart");
        panel.setContent(form);
        return panel;
    }

    private Panel addArticlePanel() {
        TextField id = new TextField("ShoppingCart id");
        TextField article = new TextField("Article");
        Button submit = new Button("Submit");
        Panel panel = new Panel("Add Article");

        submit.addClickListener(evt -> {
            commandGateway.sendAndWait(new AddArticleCommand(id.getValue(), article.getValue()));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE)
                    .addCloseListener(e -> shoppingCartSummaryDataProvider.refreshAll());
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, article, submit);
        form.setMargin(true);

        panel.setContent(form);
        return panel;
    }

    private Panel removeArticlePanel() {
        TextField id = new TextField("ShoppingCart id");
        TextField article = new TextField("Article Id");
        Button submit = new Button("Submit");

        submit.addClickListener(evt -> {
            commandGateway.sendAndWait(new RemoveArticleCommand(id.getValue(), article.getValue()));
            Notification.show("Success", Notification.Type.HUMANIZED_MESSAGE)
                        .addCloseListener(e -> shoppingCartSummaryDataProvider.refreshAll());
        });

        FormLayout form = new FormLayout();
        form.addComponents(id, article, submit);
        form.setMargin(true);

        Panel panel = new Panel("Remove Article");
        panel.setContent(form);
        return panel;
    }

    private Grid<ShoppingCartSummary> summaryGrid() {
        shoppingCartSummaryDataProvider = new ShoppingCartSummaryDataProvider(queryGateway);
        Grid<ShoppingCartSummary> grid = new Grid<>();
        grid.addColumn(ShoppingCartSummary::getId).setCaption("Card ID");
        grid.addColumn(ShoppingCartSummary::getCustomer).setCaption("Customer");
        grid.addColumn(ShoppingCartSummary::getItems).setCaption("ShoppingCart Items");
        grid.setSizeFull();
        grid.setDataProvider(shoppingCartSummaryDataProvider);
        return grid;
    }

    private class StatusUpdater implements Runnable {

        private final Label statusLabel;
        private final ZoneOffset instantOffset;

        public StatusUpdater(Label statusLabel, ZoneOffset instantOffset) {
            this.statusLabel = statusLabel;
            this.instantOffset = instantOffset;
        }

        @Override
        public void run() {
            queryGateway.query(new CountShoppingCartSummariesQuery(), CountShoppingCartSummariesResponse.class)
                        .whenComplete((r, exception) -> {
                            if (exception == null) {
                                statusLabel.setValue(Instant.ofEpochMilli(r.getLastEvent())
                                                            .atOffset(instantOffset).toString());
                            }
                        });
        }
    }
}
