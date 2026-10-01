# MyWallet

Simulateur de gestion de portefeuille et de trading, avec détection des risques en temps réel.

> **Attention : c'est un simulateur pédagogique.** Aucun ordre réel n'est passé, aucun courtier
> n'est connecté et aucun argent réel n'est utilisé. Rien ici n'est un conseil financier.

[![Backend CI](https://github.com/mhdlamine21/MyWallet/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/mhdlamine21/MyWallet/actions/workflows/backend-ci.yml)

---

## D'où vient le projet

L'idée vient de l'examen final du **Semestre 4 de Licence 2 Informatique**, en **Analyse et
Conception de Systèmes Orientés Objet**. Un exercice portait sur la modélisation de
portefeuilles, d'actifs et de passage d'ordres.

Le sujet m'a plu, alors j'ai voulu aller plus loin que le diagramme UML sur papier et coder
l'application en entier : le domaine métier, l'API, la base de données, le temps réel et
l'interface web.

## Ce que fait l'application

- Création de portefeuilles et passage d'ordres simulés (achat / vente)
- Historique des ordres en **Event Sourcing** (on peut rejouer les événements et retrouver
  l'état d'un portefeuille à une date donnée)
- **Moteur de règles** pour les stratégies de trading, basé sur un arbre syntaxique
  (pas de `eval()`, donc pas d'exécution de code arbitraire)
- **Moteur de risque** : 5 contrôles avant chaque ordre + un bouton d'arrêt d'urgence global
- **Backtest** des stratégies avec simulation Monte Carlo
- Détection d'anomalies sur les prix (Z-Score)
- Cours en temps réel via WebSocket (STOMP)
- Interface en français et en anglais, montants en **FCFA** ou en **USD**

## Tester l'application

Comptes de démo (créés par `DataSeeder` avec le profil Spring `seed` ou `demo`) :

| Rôle | Email |
|---|---|
| Investisseur | `demo.investor@mywallet.dev` |
| Trader | `demo.trader@mywallet.dev` |
| Analyste | `demo.analyst@mywallet.dev` |
| Admin | `demo.admin@mywallet.dev` |
| Auditeur | `demo.auditor@mywallet.dev` |

Mot de passe pour tous : `demo-password-not-for-real-use`

## Où trouver quoi

| Fonctionnalité | Emplacement |
|---|---|
| Agrégat `Order` en Event Sourcing, rejeu des événements | [`docs/architecture/sequence-event-replay.md`](docs/architecture/sequence-event-replay.md) |
| Parseur de règles de trading (AST) | `backend/.../ruleengine` |
| Moteur de risque et arrêt d'urgence | `backend/.../risk` |
| Backtest + Monte Carlo | `backend/.../backtest` |
| Métriques Micrometer | `backend/.../infrastructure/observability` |
| Mise à jour des positions via RabbitMQ | `backend/.../portfolio/infrastructure/messaging` |

Certaines choses ne sont pas encore faites (backtest multi-actifs, VaR, trading automatique,
certains indicateurs comme MACD ou Bollinger...). La liste est dans
[`docs/architecture/known-limitations.md`](docs/architecture/known-limitations.md).

## Architecture

Le diagramme de classes est dans [`docs/architecture/domain-model.md`](docs/architecture/domain-model.md)
et les choix techniques (RabbitMQ plutôt que Kafka, event store dans Postgres...) sont
expliqués dans [`docs/architecture/adr/`](docs/architecture/adr/).

```
MyWallet/
├── backend/                      API Spring Boot 3 (architecture hexagonale, Java 21)
├── frontend/                     Application React + TypeScript
├── docs/architecture/            Diagrammes UML, ADR, schéma de la BDD
├── infra/                        Config Prometheus
├── docker-compose.yml            Stack complète en local
└── docker-compose.prod-lite.yml  Version allégée pour un petit serveur
```

## Lancer le projet en local

```bash
cp .env.example .env
# remplir POSTGRES_PASSWORD, RABBITMQ_PASSWORD et JWT_SECRET dans .env

docker compose up --build
```

- Backend : http://localhost:8080 (Swagger sur `/swagger-ui.html`)
- Frontend : http://localhost:5173
- RabbitMQ : http://localhost:15672
- Grafana : http://localhost:3000

Le backend démarre avec le profil `seed`, donc les comptes de démo, les actifs et une
stratégie d'exemple sont créés au premier lancement.

```bash
# Tests backend (Docker doit tourner, Testcontainers lance Postgres et RabbitMQ)
cd backend && mvn clean verify

# Frontend
cd frontend && npm install && npm run dev
```

## Technologies

Java 21 · Spring Boot 3 · PostgreSQL · Flyway · Spring Security (JWT) · RabbitMQ · Redis ·
WebSocket · React · TypeScript · Tailwind · Docker · GitHub Actions · JUnit 5 / Mockito / Testcontainers
