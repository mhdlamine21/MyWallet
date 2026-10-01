import { create } from "zustand";

export type Language = "fr" | "en";

interface I18nStore {
  lang: Language;
  setLang: (lang: Language) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
}

const translations: Record<Language, Record<string, string>> = {
  fr: {
    // Layout & Nav
    "app.title": "MyWallet",
    "app.subtitle": "Simulateur de Trading & Risque",
    "app.disclaimer": "Simulateur éducatif - aucun ordre réel n'est exécuté, ceci ne constitue pas un conseil financier.",
    "nav.dashboard": "Tableau de bord",
    "nav.portfolios": "Portefeuilles",
    "nav.strategies": "Stratégies",
    "nav.guide": "Guide d'utilisation",
    "nav.signout": "Se déconnecter",
    "lang.toggle": "FR",

    // Login Page
    "login.title": "Bienvenue sur MyWallet",
    "login.subtitle": "Plateforme éducative de simulation financière & gestion proactive du risque.",
    "login.email": "Adresse email",
    "login.password": "Mot de passe",
    "login.signin": "Se connecter",
    "login.createAccount": "Créer un compte",
    "login.loading": "Connexion en cours…",
    "login.newHere": "Nouveau sur MyWallet ? Créer un compte",
    "login.alreadyAccount": "Déjà inscrit ? Se connecter",
    "login.demoAccounts": "Comptes de démonstration (cliquez pour tester) :",
    "login.demoSharedPassword": "Mot de passe partagé :",
    "login.error.invalid": "Email ou mot de passe incorrect.",
    "login.error.register": "Impossible de créer le compte (l'adresse email est peut-être déjà utilisée).",
    "demo.investor": "Profil Investisseur",
    "demo.trader": "Profil Trader",
    "demo.analyst": "Profil Analyste",
    "demo.admin": "Profil Administrateur",

    // Dashboard
    "dashboard.title": "Tableau de bord",
    "dashboard.subtitle": "Aperçu global de votre patrimoine simulé et suivi en temps réel.",
    "dashboard.totalCash": "Liquidités globales",
    "dashboard.portfolioCount": "Portefeuilles actifs",
    "dashboard.simulationMode": "Mode d'exécution",
    "dashboard.simulationModeDesc": "Éducatif (Zéro risque)",
    "dashboard.myPortfolios": "Vos portefeuilles",
    "dashboard.emptyPortfolios": "Aucun portefeuille pour le moment. Rendez-vous dans l'onglet Portefeuilles pour créer votre premier compte.",
    "dashboard.availableCash": "liquidités disponibles",
    "dashboard.viewDetail": "Consulter le portefeuille ->",

    // Portfolios
    "portfolios.title": "Portefeuilles & Comptes",
    "portfolios.subtitle": "Gérez vos allocations de capital et explorez les carnets d'ordres.",
    "portfolios.new": "+ Nouveau portefeuille",
    "portfolios.needAccount": "Vous devez d'abord créer un compte simulé.",
    "portfolios.createAccount": "Créer un compte simulé par défaut",
    "portfolios.selectAccount": "Sélectionner un compte…",
    "portfolios.namePlaceholder": "Nom du portefeuille (ex: Tech & Crypto)",
    "portfolios.modeSimulated": "Simulation standard",
    "portfolios.modeDemo": "Mode Démo",
    "portfolios.initialCash": "Solde initial en cash (ex: 25000)",
    "portfolios.createBtn": "Créer le portefeuille",

    // Portfolio Detail
    "portfolioDetail.liveUpdates": "Flux temps réel connecté",
    "portfolioDetail.liveDisconnected": "Flux temps réel déconnecté",
    "portfolioDetail.equityCurve": "Évolution de la valeur totale (Equity Curve)",
    "portfolioDetail.positions": "Positions détenues",
    "portfolioDetail.noPositions": "Aucune position ouverte pour le moment.",
    "portfolioDetail.asset": "Actif",
    "portfolioDetail.quantity": "Quantité",
    "portfolioDetail.avgPrice": "Prix moyen d'acquisition",
    "portfolioDetail.placeOrder": "Passer un ordre",
    "portfolioDetail.orderType": "Type d'ordre",
    "portfolioDetail.side": "Sens de l'ordre",
    "portfolioDetail.buy": "ACHAT (BUY)",
    "portfolioDetail.sell": "VENTE (SELL)",
    "portfolioDetail.limitPrice": "Prix limite ($)",
    "portfolioDetail.submitOrder": "Transmettre l'ordre",
    "portfolioDetail.orderSuccess": "Ordre soumis avec succès ! Traité via Event Sourcing.",
    "portfolioDetail.orderHistory": "Historique des ordres",
    "portfolioDetail.riskAlerts": "Alertes du moteur de risque",
    "portfolioDetail.noAlerts": "Aucune infraction de risque détectée. Tout est conforme.",

    // Strategies
    "strategies.title": "Stratégies de Trading Automatisées",
    "strategies.subtitle": "Concevez des règles algorithmiques sans code arbitraire et testez-les sur l'historique.",
    "strategies.new": "+ Nouvelle stratégie",
    "strategies.leaderboard": "Classement des stratégies (Leaderboard)",
    "strategies.ruleExpression": "Expression de la règle (Syntaxe AST sécurisée)",
    "strategies.createBtn": "Enregistrer la stratégie",
    "strategies.status": "Statut",
    "strategies.activate": "Activer",
    "strategies.suspend": "Suspendre",
    "strategies.deactivate": "Désactiver",
    "strategies.backtest": "Lancer un Backtest",
    "strategies.backtestPeriod": "Période d'évaluation",
    "strategies.initialCapital": "Capital initial",
    "strategies.runBacktest": "Exécuter la simulation Monte Carlo",

    // Guide Modal
    "guide.title": "Guide d'utilisation & Manuel MyWallet",
    "guide.subtitle": "Apprenez à maîtriser toutes les fonctionnalités de la plateforme.",
    "guide.tab.overview": "Vue d'ensemble",
    "guide.tab.portfolio": "Portefeuilles & Ordres",
    "guide.tab.strategies": "Moteur de règles",
    "guide.tab.risk": "Contrôle des Risques",
    "guide.tab.backtest": "Backtesting & Monte Carlo",
    "guide.close": "Fermer le guide"
  },
  en: {
    // Layout & Nav
    "app.title": "MyWallet",
    "app.subtitle": "Simulated Trading & Risk Platform",
    "app.disclaimer": "Educational simulator - no real orders are executed, this is not financial advice.",
    "nav.dashboard": "Dashboard",
    "nav.portfolios": "Portfolios",
    "nav.strategies": "Strategies",
    "nav.guide": "User Guide",
    "nav.signout": "Sign out",
    "lang.toggle": "EN",

    // Login Page
    "login.title": "Welcome to MyWallet",
    "login.subtitle": "Educational platform for financial simulation & proactive risk control.",
    "login.email": "Email address",
    "login.password": "Password",
    "login.signin": "Sign in",
    "login.createAccount": "Create account",
    "login.loading": "Signing in…",
    "login.newHere": "New to MyWallet? Create an account",
    "login.alreadyAccount": "Already registered? Sign in",
    "login.demoAccounts": "Demo accounts (click to test):",
    "login.demoSharedPassword": "Shared password:",
    "login.error.invalid": "Invalid email or password.",
    "login.error.register": "Could not create the account (email may already be in use).",
    "demo.investor": "Investor Demo",
    "demo.trader": "Trader Demo",
    "demo.analyst": "Analyst Demo",
    "demo.admin": "Admin Demo",

    // Dashboard
    "dashboard.title": "Dashboard",
    "dashboard.subtitle": "Global overview of your simulated wealth with real-time streaming.",
    "dashboard.totalCash": "Total Available Cash",
    "dashboard.portfolioCount": "Active Portfolios",
    "dashboard.simulationMode": "Execution Mode",
    "dashboard.simulationModeDesc": "Educational (Zero Risk)",
    "dashboard.myPortfolios": "Your Portfolios",
    "dashboard.emptyPortfolios": "No portfolios yet. Visit the Portfolios tab to create your first account and portfolio.",
    "dashboard.availableCash": "available cash",
    "dashboard.viewDetail": "View Portfolio ->",

    // Portfolios
    "portfolios.title": "Portfolios & Accounts",
    "portfolios.subtitle": "Manage capital allocations and explore simulated order execution.",
    "portfolios.new": "+ New portfolio",
    "portfolios.needAccount": "You need a simulated account first.",
    "portfolios.createAccount": "Create default simulated account",
    "portfolios.selectAccount": "Select an account…",
    "portfolios.namePlaceholder": "Portfolio name (e.g. Tech & Crypto)",
    "portfolios.modeSimulated": "Simulated",
    "portfolios.modeDemo": "Demo",
    "portfolios.initialCash": "Initial cash balance (e.g. 25000)",
    "portfolios.createBtn": "Create portfolio",

    // Portfolio Detail
    "portfolioDetail.liveUpdates": "Live stream connected",
    "portfolioDetail.liveDisconnected": "Live stream disconnected",
    "portfolioDetail.equityCurve": "Total Valuation History (Equity Curve)",
    "portfolioDetail.positions": "Held Positions",
    "portfolioDetail.noPositions": "No open positions yet.",
    "portfolioDetail.asset": "Asset",
    "portfolioDetail.quantity": "Quantity",
    "portfolioDetail.avgPrice": "Avg. Acquisition Price",
    "portfolioDetail.placeOrder": "Place an Order",
    "portfolioDetail.orderType": "Order Type",
    "portfolioDetail.side": "Order Side",
    "portfolioDetail.buy": "BUY",
    "portfolioDetail.sell": "SELL",
    "portfolioDetail.limitPrice": "Limit Price ($)",
    "portfolioDetail.submitOrder": "Submit Order",
    "portfolioDetail.orderSuccess": "Order submitted successfully! Replayed via Event Sourcing.",
    "portfolioDetail.orderHistory": "Order History",
    "portfolioDetail.riskAlerts": "Risk Engine Alerts",
    "portfolioDetail.noAlerts": "No risk breaches detected. Everything is compliant.",

    // Strategies
    "strategies.title": "Automated Trading Strategies",
    "strategies.subtitle": "Design algorithmic rules without arbitrary code execution and test against history.",
    "strategies.new": "+ New strategy",
    "strategies.leaderboard": "Strategy Leaderboard",
    "strategies.ruleExpression": "Rule Expression (Secure AST Syntax)",
    "strategies.createBtn": "Save Strategy",
    "strategies.status": "Status",
    "strategies.activate": "Activate",
    "strategies.suspend": "Suspend",
    "strategies.deactivate": "Deactivate",
    "strategies.backtest": "Run Backtest",
    "strategies.backtestPeriod": "Evaluation Period",
    "strategies.initialCapital": "Initial Capital",
    "strategies.runBacktest": "Run Monte Carlo Simulation",

    // Guide Modal
    "guide.title": "MyWallet User Guide & Manual",
    "guide.subtitle": "Learn how to master every capability of the platform.",
    "guide.tab.overview": "Overview",
    "guide.tab.portfolio": "Portfolios & Orders",
    "guide.tab.strategies": "Rule Engine",
    "guide.tab.risk": "Risk Control",
    "guide.tab.backtest": "Backtesting & Monte Carlo",
    "guide.close": "Close Guide"
  }
};

const savedLang = (localStorage.getItem("mywallet_lang") as Language) || "fr";

export const useI18nStore = create<I18nStore>((set, get) => ({
  lang: savedLang,
  setLang: (lang) => {
    localStorage.setItem("mywallet_lang", lang);
    set({ lang });
  },
  t: (key, params) => {
    const { lang } = get();
    let text = translations[lang]?.[key] || translations["fr"]?.[key] || key;
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        text = text.replace(`{${k}}`, String(v));
      });
    }
    return text;
  }
}));
