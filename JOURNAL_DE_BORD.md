# Journal de bord - MyWallet

Projet perso commencé après l'examen final du S4 (Licence 2 Informatique), en Analyse et
Conception de Systèmes Orientés Objet. Un exercice demandait de modéliser des portefeuilles,
des actifs et des ordres. J'ai voulu aller plus loin que le diagramme UML et coder
l'application en vrai.

Au départ je n'avais que les bases de Java vues en cours (classes, héritage, interfaces,
collections). Je note ici ce que j'ai fait, ce qui m'a bloqué et comment je m'en suis sorti.

---

## Semaine du 5 mai : mise en place et le piège du `double`

- Création du dépôt, projet Maven avec Spring Boot. PostgreSQL installé directement sur ma
  machine, les identifiants sont dans un `.env` (jamais commité).
- Premières classes : `Order`, `Portfolio`, `Account`, `Asset`.
- **Erreur de débutant** : j'avais mis des `double` pour l'argent. `0.1 + 0.2` donnait
  `0.30000000000000004`. En cherchant, j'ai compris qu'on n'utilise jamais `double` pour de
  l'argent. Tout est passé en `BigDecimal` (classes `Money` et `Quantity`), avec un arrondi
  explicite.
- Premiers tests unitaires sur `Order` avec JUnit 5.

## Semaine du 11 mai : Event Sourcing

- **Problème** : avec une table `orders` mise à jour par des `UPDATE`, j'écrasais l'ancien état.
  Impossible de savoir quand un ordre était passé de `PENDING` à `PARTIALLY_FILLED`.
- **Solution trouvée** : l'Event Sourcing. Chaque changement est un événement
  (`OrderCreatedEvent`, `OrderFilledEvent`, `OrderCancelledEvent`...) et on rejoue les
  événements pour retrouver l'état.
- Tests sur `Portfolio` : premier vrai bug trouvé grâce aux tests, un achat pouvait faire
  passer le cash en négatif. Ajout d'une vérification avec une exception dédiée.

## Semaine du 18 mai : persistance, Flyway et tests d'intégration

- Découverte de Flyway : on ne laisse pas Hibernate créer les tables, on écrit des migrations
  SQL versionnées (`V1`, `V2`, `V3`...).
- Event store dans PostgreSQL (ADR 0003 pour garder une trace du choix).
- **Blocage** : je voulais tester avec une vraie base, pas une base en mémoire. J'ai trouvé
  Testcontainers, mais il lui faut Docker. J'ai installé Docker Desktop juste pour ça, sans
  vraiment comprendre comment ça marchait. Les premiers tests plantaient sans message clair :
  Docker Desktop n'était tout simplement pas lancé.

## Semaine du 25 mai : architecture hexagonale et API REST

- J'avais mis `@Entity` directement sur mes classes métier. Mon prof m'a rappelé que le domaine
  ne doit pas dépendre du framework. J'ai séparé `domain/`, `application/`, `infrastructure/`
  et `interfaces/`.
- API REST pour les ordres et les portefeuilles, puis tests des controllers.

## Semaine du 1er juin : Spring Security et JWT

- **Blocage** : dès que j'ai ajouté Spring Security, tout renvoyait 403, même les routes
  publiques. J'ai tout ouvert le temps de comprendre la chaîne de filtres.
- Mots de passe hachés avec BCrypt, authentification par JWT (access token + refresh token).
- **Bug** : le token expirait immédiatement. Je mélangeais secondes et millisecondes dans le
  calcul de l'expiration. Une soirée perdue là-dessus.
- Même message d'erreur que l'email existe ou non, pour ne pas révéler les comptes.

## Semaine du 7 juin : force brute et données de marché

- Verrouillage du compte après plusieurs échecs (migration `V7`).
- Rate limiting sur `/auth/login` après lecture de l'OWASP Top 10.
- Générateur de prix simulés pour avoir des cours qui bougent.

## Semaine du 14 juin : RabbitMQ

- Recalculer les positions directement dans la requête HTTP ralentissait tout. Les événements
  partent maintenant dans RabbitMQ et un listener met à jour les positions (ADR 0002 : pourquoi
  RabbitMQ et pas Kafka).
- **Bug** : quand RabbitMQ renvoyait un message, la position était mise à jour deux fois.
  Ajout d'une vérification d'idempotence (`processed_projector_events`) pour ignorer les
  doublons.

## Semaine du 18 juin : moteur de règles sans `eval()`

- Je voulais que l'utilisateur écrive des règles comme `SMA(BTCUSDT, 20) > SMA(BTCUSDT, 50)`.
  Ma première idée était un moteur d'expressions dynamique, mais c'est une porte ouverte à
  l'exécution de code arbitraire.
- J'ai écrit un lexer, un parser et un évaluateur qui parcourt l'arbre. Aucun `eval()`.
- Pour les indicateurs (SMA, EMA, RSI), j'ai d'abord tout codé à la main puis je suis passé à
  `ta4j`, beaucoup plus fiable sur les cas limites.

## Semaine du 22 juin : stratégies et moteur de risque

- Création et cycle de vie des stratégies (brouillon, active, en pause...).
- Moteur de risque : 5 contrôles avant chaque ordre (cash dispo, quantité dispo, montant max,
  exposition max par actif, nombre d'ordres par jour).
- Kill switch global côté admin pour tout bloquer d'un coup.
- Détection d'anomalies sur les prix avec un Z-Score.

## Semaine du 29 juin : backtest

- Backtest des stratégies + simulation Monte Carlo (`Apache Commons Math`).
- **Bug** : les tests du backtest donnaient un résultat différent à chaque lancement. Il fallait
  fixer la graine du générateur aléatoire dans les tests.

## Semaine du 4 juillet : temps réel avec WebSocket

- **Blocage sans solution le premier soir** : la connexion WebSocket était refusée depuis le
  navigateur. J'ai commité en "wip" et je suis allé dormir.
- Le lendemain : l'origine du front n'était pas autorisée et le token JWT n'était pas transmis
  au moment du handshake STOMP. Corrigé avec un intercepteur.
- Métriques avec Micrometer, données de démo (`DataSeeder`), test d'architecture avec ArchUnit
  qui échoue si le domaine importe Spring.

## Semaine du 9 juillet : frontend React

- Premier projet React : Vite + TypeScript + Tailwind.
- Page de connexion, client API avec Axios, token gardé en mémoire avec Zustand.
- Dashboard, pages portefeuilles et stratégies, traduction FR / EN.
- Graphiques en temps réel avec Recharts, branchés sur le WebSocket.

## Semaine du 14 juillet : finitions

- Devises : FCFA par défaut et USD.
- Menu utilisateur en haut à droite (langue, devise, déconnexion) au lieu de la sidebar.
- Logo SVG de l'application, pages Confidentialité et Conditions.
- **Bug** : les pages Confidentialité et Conditions n'étaient pas accessibles sans être
  connecté (bloquées par `ProtectedRoute`). Ajout d'un `PublicPageLayout` pour les rendre publiques.
- Documentation : diagrammes, schéma de la base, README.
