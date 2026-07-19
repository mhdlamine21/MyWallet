# MyWallet

Simulateur de gestion de portefeuille et de trading, avec détection des risques en temps réel.

> **Attention : c'est un simulateur pédagogique.** Aucun ordre réel n'est passé, aucun courtier
> n'est connecté et aucun argent réel n'est utilisé. Rien ici n'est un conseil financier.

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

## Architecture

Le diagramme de classes est dans [`docs/architecture/domain-model.md`](docs/architecture/domain-model.md)
et les choix techniques (RabbitMQ plutôt que Kafka, event store dans Postgres...) sont
expliqués dans [`docs/architecture/adr/`](docs/architecture/adr/).

```
MyWallet/
├── backend/            API Spring Boot 3 (architecture hexagonale, Java 21)
├── frontend/           Application React + TypeScript
└── docs/architecture/  Diagrammes UML, ADR, schéma de la BDD
```

## Lancer le projet en local

Il faut installer sur sa machine : Java 21, Maven, Node.js, PostgreSQL, RabbitMQ et Redis.

1. Créer une base `mywallet` (utilisateur `mywallet`) dans PostgreSQL
2. Copier `.env.example` en `.env` et remplir les mots de passe et le `JWT_SECRET`
3. Lancer le backend puis le frontend dans deux terminaux :

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=seed
cd frontend && npm install && npm run dev
```

- Backend : http://localhost:8080 (Swagger sur `/swagger-ui.html`)
- Frontend : http://localhost:5173

Les tests d'intégration utilisent Testcontainers, il faut donc que Docker Desktop soit lancé :

```bash
cd backend && mvn clean verify
```

## Technologies

Java 21 · Spring Boot 3 · PostgreSQL · Flyway · Spring Security (JWT) · RabbitMQ · Redis ·
WebSocket · React · TypeScript · Tailwind · JUnit 5 / Mockito / Testcontainers
