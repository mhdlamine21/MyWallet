package io.mywallet.portfolio.domain.model;

import io.mywallet.common.domain.AggregateRoot;
import io.mywallet.common.domain.DomainEvent;
import io.mywallet.common.domain.EventMetadata;
import io.mywallet.portfolio.domain.event.PortfolioCreated;
import io.mywallet.portfolio.domain.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The Portfolio aggregate - the second event-sourced aggregate in MyWallet, alongside
 * {@link io.mywallet.order.domain.model.Order}.
 *
 * =========================================================================================
 * NOTE D'APPRENTISSAGE ÉTUDIANT (Semaine 2 - Le piège du type double & l'invariant de caisse)
 * =========================================================================================
 * - Erreur initiale (Semaine 1) :
 *   J'utilisais `double cashBalance = 1000.0;`. En faisant 1000.0 - 0.1, Java m'a affiché
 *   999.8999999999999 ! Sur une application financière, c'est une faute éliminatoire (perte de centimes).
 * - Correction :
 *   Migration immédiate vers `java.math.BigDecimal` avec échelle explicite et arrondi `RoundingMode.HALF_EVEN`.
 *
 * NOTE ARCHITECTURE (Semaine 5 - Séparation Projections vs Agrégat) :
 * - Question initiale : Pourquoi ne pas stocker la liste des positions `List<Position>` directement ici ?
 * - Réponse découverte : Si un portefeuille détient 80 positions différentes, charger et rejouer
 *   tout l'historique d'événements pour juste débiter 10€ de cash serait affreusement lent.
 *   L'agrégat ne protège que son invariant strict (le cashBalance >= 0). Les positions vivent
 *   dans une table de projection optimisée (`portfolio_positions`), mise à jour asynchronement via RabbitMQ.
 * =========================================================================================
 */
public class Portfolio extends AggregateRoot {

    private UUID accountId;
    private String name;
    private PortfolioMode mode;
    private BigDecimal cashBalance;

    private Portfolio(UUID id) {
        super(id);
    }

    public static Portfolio create(
        UUID accountId,
        String name,
        PortfolioMode mode,
        BigDecimal initialCashBalance,
        UUID correlationId,
        UUID actorId
    ) {
        if (initialCashBalance.signum() < 0) {
            throw new InsufficientFundsException("Initial cash balance cannot be negative");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Portfolio name must not be blank");
        }

        Portfolio portfolio = new Portfolio(UUID.randomUUID());
        portfolio.raise(new PortfolioCreated(
            EventMetadata.create(correlationId, null, actorId),
            portfolio.getId(),
            accountId,
            name,
            mode,
            initialCashBalance
        ));
        return portfolio;
    }

    public static Portfolio reconstruct(UUID id, List<DomainEvent> history) {
        Portfolio portfolio = new Portfolio(id);
        portfolio.loadFromHistory(history);
        return portfolio;
    }

    /** Empty shell for the repository's replay mechanism - see {@code EventStoreRepository#load}. */
    public static Portfolio emptyShell(UUID id) {
        return new Portfolio(id);
    }

    @Override
    protected void apply(DomainEvent event) {
        switch (event) {
            case PortfolioCreated e -> {
                this.accountId = e.accountId();
                this.name = e.name();
                this.mode = e.mode();
                this.cashBalance = e.initialCashBalance();
            }
            default -> throw new IllegalStateException(
                "Portfolio does not know how to apply event type: " + event.eventType());
        }
    }

    public UUID accountId() {
        return accountId;
    }

    public String name() {
        return name;
    }

    public PortfolioMode mode() {
        return mode;
    }

    public BigDecimal cashBalance() {
        return cashBalance;
    }
}
