import { useState } from "react";
import { useI18nStore } from "../i18n/useI18n";

interface UserGuideModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function UserGuideModal({ isOpen, onClose }: UserGuideModalProps) {
  const { lang } = useI18nStore();
  const [activeTab, setActiveTab] = useState<"overview" | "portfolios" | "rules" | "risk" | "backtest" | "genesis">("overview");

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div 
        className="w-full max-w-4xl max-h-[88vh] rounded-2xl bg-[#0D0F14] border border-white/10 shadow-[0_0_50px_-10px_rgba(99,102,241,0.25)] flex flex-col overflow-hidden text-slate-200"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-white/10 bg-[#12151C]/60">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 via-indigo-600 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/25">
              <span className="text-lg">📖</span>
            </div>
            <div>
              <h2 className="text-base font-semibold text-white tracking-wide">
                {lang === "fr" ? "Guide d'utilisation & Manuel MyWallet" : "MyWallet User Guide & Manual"}
              </h2>
              <p className="text-xs text-slate-400">
                {lang === "fr" 
                  ? "Découvrez l'architecture, le passage d'ordres et les fonctionnalités avancées"
                  : "Discover the architecture, order placement, and advanced capabilities"}
              </p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="w-8 h-8 rounded-lg bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white flex items-center justify-center transition-colors text-sm"
          >
            ✕
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="flex border-b border-white/5 bg-[#0A0C10] px-6 overflow-x-auto gap-2 py-2">
          {[
            { id: "overview", label: lang === "fr" ? "🌟 Vue d'ensemble" : "🌟 Overview" },
            { id: "portfolios", label: lang === "fr" ? "💼 Portefeuilles & Ordres" : "💼 Portfolios & Orders" },
            { id: "rules", label: lang === "fr" ? "🧠 Moteur de règles" : "🧠 Rule Engine" },
            { id: "risk", label: lang === "fr" ? "🛡️ Gestion du Risque" : "🛡️ Risk Management" },
            { id: "backtest", label: lang === "fr" ? "📈 Backtest & Monte Carlo" : "📈 Backtesting" },
            { id: "genesis", label: lang === "fr" ? "🎓 Genèse & Contexte" : "🎓 Origin Story" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-all whitespace-nowrap ${
                activeTab === tab.id
                  ? "bg-indigo-600/30 text-indigo-400 border border-indigo-500/30 shadow-sm shadow-indigo-500/10"
                  : "text-slate-400 hover:text-slate-200 hover:bg-white/5"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 text-sm text-slate-300 leading-relaxed max-h-[calc(88vh-140px)]">
          {activeTab === "overview" && (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-gradient-to-r from-indigo-950/40 via-purple-950/20 to-transparent border border-indigo-500/20">
                <h3 className="text-white font-medium text-base mb-1">
                  {lang === "fr" ? "Qu'est-ce que MyWallet ?" : "What is MyWallet?"}
                </h3>
                <p className="text-xs text-slate-300">
                  {lang === "fr"
                    ? "MyWallet est une plateforme professionnelle de simulation de trading algorithmique, de suivi de portefeuille multi-comptes et de contrôle de risque en temps réel. Elle applique des patrons d'architecture logicielle de niveau Senior : Event Sourcing (ES), CQRS, Architecture Hexagonale et communications asynchrones RabbitMQ."
                    : "MyWallet is a professional simulated algorithmic trading and real-time risk platform. It demonstrates senior engineering patterns: Event Sourcing, CQRS, Hexagonal Architecture, and asynchronous RabbitMQ messaging."}
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-4 rounded-xl bg-[#141721] border border-white/5 hover:border-indigo-500/30 transition-all">
                  <div className="text-indigo-400 font-semibold mb-1 flex items-center gap-2">
                    <span>⚡</span> {lang === "fr" ? "Event Sourcing" : "Event Sourcing"}
                  </div>
                  <p className="text-xs text-slate-400">
                    {lang === "fr"
                      ? "Chaque ordre est une séquence d'événements immuables (CREATED, APPROVED, EXECUTED, REJECTED) permettant de rejouer l'historique à n'importe quel instant T."
                      : "Every order is recorded as an immutable stream of events allowing exact point-in-time state reconstruction."}
                  </p>
                </div>
                <div className="p-4 rounded-xl bg-[#141721] border border-white/5 hover:border-emerald-500/30 transition-all">
                  <div className="text-emerald-400 font-semibold mb-1 flex items-center gap-2">
                    <span>🛡️</span> {lang === "fr" ? "Moteur de Risque" : "Risk Engine"}
                  </div>
                  <p className="text-xs text-slate-400">
                    {lang === "fr"
                      ? "5 contrôles en temps réel (solde disponible, exposition maximale, taille d'ordre) bloquent les ordres frauduleux ou imprudents avant transmission."
                      : "5 real-time checks ensure orders never exceed cash balance, single-asset exposure, or velocity thresholds."}
                  </p>
                </div>
                <div className="p-4 rounded-xl bg-[#141721] border border-white/5 hover:border-purple-500/30 transition-all">
                  <div className="text-purple-400 font-semibold mb-1 flex items-center gap-2">
                    <span>📈</span> {lang === "fr" ? "Monte Carlo" : "Monte Carlo"}
                  </div>
                  <p className="text-xs text-slate-400">
                    {lang === "fr"
                      ? "Backtest avec bandes de confiance percentiles P5 (scénario défavorable), P50 (médiane) et P95 (scénario optimiste) sur votre capital."
                      : "Backtesting simulator with P5 (bear), P50 (median) and P95 (bull) Monte Carlo confidence intervals."}
                  </p>
                </div>
              </div>

              <div className="p-3 rounded-lg bg-amber-950/20 border border-amber-600/30 text-amber-300 text-xs flex items-center gap-3">
                <span className="text-base">⚠️</span>
                <span>
                  {lang === "fr"
                    ? "Rappel important : Aucun compte de courtage réel n'est connecté et aucun argent réel n'est engagé. Tous les prix et flux de marché sont simulés mathématiquement."
                    : "Important reminder: No real brokerage is connected, no real money is involved. All market ticks and orders are simulated."}
                </span>
              </div>
            </div>
          )}

          {activeTab === "portfolios" && (
            <div className="space-y-4">
              <h3 className="text-white font-medium text-base">
                {lang === "fr" ? "Gestion des Portefeuilles & Passage d'Ordres" : "Portfolios & Order Execution"}
              </h3>
              <ol className="list-decimal list-inside space-y-3 text-xs text-slate-300">
                <li className="p-3 rounded-lg bg-[#141721] border border-white/5">
                  <strong className="text-white">{lang === "fr" ? "Créer un compte et un portefeuille :" : "Create an account and portfolio:"}</strong>{" "}
                  {lang === "fr"
                    ? "Dans l'onglet Portefeuilles, cliquez sur '+ Nouveau portefeuille'. Si aucun compte n'existe encore, créez d'abord le compte simulé avec son solde de départ (ex: 25 000 $)."
                    : "In the Portfolios tab, click '+ New portfolio'. Create your default simulated account if needed and set initial capital."}
                </li>
                <li className="p-3 rounded-lg bg-[#141721] border border-white/5">
                  <strong className="text-white">{lang === "fr" ? "Consulter la vue détaillée :" : "View portfolio details:"}</strong>{" "}
                  {lang === "fr"
                    ? "Cliquez sur votre portefeuille pour observer vos positions ouvertes, la valorisation globale en temps réel (Equity Curve) et les graphiques de prix en direct via WebSocket STOMP."
                    : "Click any portfolio card to monitor live positions, equity evolution and asset price charts via STOMP WebSocket."}
                </li>
                <li className="p-3 rounded-lg bg-[#141721] border border-white/5">
                  <strong className="text-white">{lang === "fr" ? "Passer un ordre d'Achat ou de Vente :" : "Place a Buy or Sell order:"}</strong>{" "}
                  {lang === "fr"
                    ? "Choisissez un actif (BTCUSDT, ETHUSDT, AAPL), indiquez le type d'ordre (MARKET au cours actuel, ou LIMIT à un prix cible) puis validez. Le moteur de risque évalue l'ordre instantanément avant de l'envoyer dans la file RabbitMQ."
                    : "Select an asset, specify MARKET or LIMIT type, enter quantity and price. The risk engine inspects it before queuing it into RabbitMQ."}
                </li>
              </ol>
            </div>
          )}

          {activeTab === "rules" && (
            <div className="space-y-4">
              <h3 className="text-white font-medium text-base">
                {lang === "fr" ? "Moteur de Règles & Grammaire AST Sécurisée" : "Secure AST Rule Engine"}
              </h3>
              <p className="text-xs text-slate-300">
                {lang === "fr"
                  ? "Pour prévenir toute faille d'injection de code, MyWallet n'utilise JAMAIS eval() ni d'interpréteur de script non sécurisé. Le moteur s'appuie sur un Lexer et un Parser récursif descendant qui génère un Arbre Syntaxique Abstrait (AST)."
                  : "To eliminate code injection risks, MyWallet NEVER uses eval() or arbitrary scripting. It uses a custom Lexer and recursive-descent Parser generating an AST."}
              </p>

              <div className="p-4 rounded-xl bg-[#090A0E] border border-white/10 font-mono text-xs text-indigo-300 space-y-2">
                <div className="text-slate-400">// {lang === "fr" ? "Exemple de règle valide :" : "Valid rule example:"}</div>
                <div className="text-emerald-400">
                  SMA(BTCUSDT, 20) &gt; SMA(BTCUSDT, 50) AND RSI(BTCUSDT, 14) &lt; 70 AND PortfolioExposure &lt; 50%
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-[#141721] border border-white/5">
                  <strong className="text-white block mb-1">Indicateurs disponibles :</strong>
                  <ul className="list-disc list-inside text-slate-400 space-y-1">
                    <li><code className="text-indigo-400">SMA(symbole, période)</code> - Moyenne mobile simple</li>
                    <li><code className="text-indigo-400">RSI(symbole, période)</code> - Relative Strength Index (0 à 100)</li>
                    <li><code className="text-indigo-400">Price(symbole)</code> - Dernier prix constaté</li>
                    <li><code className="text-indigo-400">PortfolioExposure</code> - % d'exposition global</li>
                  </ul>
                </div>
                <div className="p-3 rounded-lg bg-[#141721] border border-white/5">
                  <strong className="text-white block mb-1">Opérateurs supportés :</strong>
                  <ul className="list-disc list-inside text-slate-400 space-y-1">
                    <li>Comparateurs : <code className="text-indigo-400">&gt;, &lt;, &gt;=, &lt;=, ==</code></li>
                    <li>Logiques : <code className="text-indigo-400">AND, OR, NOT</code></li>
                    <li>Parenthèses imbriquées sans limite</li>
                  </ul>
                </div>
              </div>
            </div>
          )}

          {activeTab === "risk" && (
            <div className="space-y-4">
              <h3 className="text-white font-medium text-base">
                {lang === "fr" ? "Contrôle des Risques & Coupe-Circuit (Kill Switch)" : "Risk Control & Global Kill Switch"}
              </h3>
              <p className="text-xs text-slate-300">
                {lang === "fr"
                  ? "Le moteur de risque évalue chaque ordre avant qu'il ne puisse être exécuté. En cas de violation, l'ordre est automatiquement rejeté et une alerte de risque est diffusée en temps réel."
                  : "The risk engine validates every order prior to execution. Breaches trigger immediate rejection and real-time STOMP alerts."}
              </p>

              <div className="space-y-2 text-xs">
                {[
                  { name: "Available Cash Check", desc: lang === "fr" ? "Refuse l'achat si les liquidités du portefeuille sont insuffisantes." : "Rejects purchases if cash balance is insufficient." },
                  { name: "Available Quantity Check", desc: lang === "fr" ? "Refuse la vente si vous ne possédez pas la quantité d'actifs demandée." : "Rejects sell orders exceeding current position quantity." },
                  { name: "Max Order Value Limit", desc: lang === "fr" ? "Plafonne la valeur maximale unitaire d'un ordre pour éviter les erreurs de saisie." : "Enforces a hard limit on individual order notional value." },
                  { name: "Max Asset Exposure Limit", desc: lang === "fr" ? "Empêche une concentration excessive sur un seul actif (ex: max 30% du portefeuille)." : "Limits maximum portfolio concentration per single asset." },
                  { name: "Kill Switch Global", desc: lang === "fr" ? "Interrupteur d'urgence administrateur stoppant instantanément tout passage d'ordre en cas d'anomalie de marché." : "Emergency administrator switch instantly blocking all trading operations." },
                ].map((item, idx) => (
                  <div key={idx} className="p-3 rounded-lg bg-[#141721] border border-white/5 flex items-start gap-3">
                    <span className="text-indigo-400 font-bold">0{idx + 1}.</span>
                    <div>
                      <div className="font-semibold text-white">{item.name}</div>
                      <div className="text-slate-400">{item.desc}</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === "backtest" && (
            <div className="space-y-4">
              <h3 className="text-white font-medium text-base">
                {lang === "fr" ? "Simulation Historique & Intervalles Monte Carlo" : "Historical Backtest & Monte Carlo"}
              </h3>
              <p className="text-xs text-slate-300">
                {lang === "fr"
                  ? "Le module de backtesting évalue votre stratégie contre les données de marché historiques et projette 100 simulations Monte Carlo pour évaluer la robustesse de votre stratégie."
                  : "The backtesting engine executes your strategy over historical bars and computes 100 Monte Carlo bootstrap paths."}
              </p>

              <div className="p-4 rounded-xl bg-[#090A0E] border border-white/10 space-y-2 text-xs">
                <div className="font-semibold text-white">{lang === "fr" ? "Comprendre les courbes :" : "Understanding the curves:"}</div>
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-indigo-500 inline-block" />
                  <span className="text-slate-300"><strong className="text-white">Stratégie :</strong> Valeur de votre capital avec les règles appliquées.</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-slate-500 inline-block" />
                  <span className="text-slate-300"><strong className="text-white">Buy & Hold :</strong> Performance de référence si vous aviez simplement conservé l'actif.</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-emerald-500/70 inline-block" />
                  <span className="text-slate-300"><strong className="text-white">P95 (Optimiste) :</strong> 95% des scénarios simulés ont produit un résultat inférieur à cette courbe.</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-amber-500 inline-block" />
                  <span className="text-slate-300"><strong className="text-white">P50 (Médiane) :</strong> Trajectoire médiane probabiliste.</span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-red-500/70 inline-block" />
                  <span className="text-slate-300"><strong className="text-white">P5 (Défavorable) :</strong> Pire 5% des trajectoires de risque.</span>
                </div>
              </div>
            </div>
          )}

          {activeTab === "genesis" && (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-gradient-to-r from-purple-950/40 via-indigo-950/20 to-transparent border border-purple-500/20">
                <h3 className="text-white font-medium text-base mb-1">
                  🎓 {lang === "fr" ? "Origine & Histoire du projet" : "Project Genesis & Context"}
                </h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  {lang === "fr"
                    ? "Ce projet a été inspiré lors de notre examen final du Semestre 4 de Licence 2 Informatique, dans le cadre du cours d'Analyse et Conception de Systèmes Orientés Objet (ACSOO). Durant l'épreuve, un exercice portait sur un cas d'étude de gestion de portefeuilles, d'actifs et de transactions. Trouvant le contexte particulièrement captivant et riche en défis d'ingénierie, j'ai pris l'initiative d'aller bien au-delà de l'exercice académique pour concevoir et développer une application complète, distribuée et hautement disponible : MyWallet."
                    : "This project originated from our Semester 4 final examination in 2nd-year Computer Science during the Object-Oriented Systems Analysis & Design course. The exam presented an exercise modeling investment portfolios and transactions. Intrigued by the engineering challenges, I expanded the concept into this full production-ready distributed platform."}
                </p>
              </div>

              <div className="p-4 rounded-xl bg-[#141721] border border-white/5 space-y-2 text-xs">
                <div className="font-semibold text-white">{lang === "fr" ? "Technologies clés mises en œuvre :" : "Key technologies implemented:"}</div>
                <div className="grid grid-cols-2 gap-2 text-slate-400">
                  <div>• Java 21 & Spring Boot 3.3.4</div>
                  <div>• PostgreSQL 16 & Flyway (9 migrations)</div>
                  <div>• Event Sourcing (Order aggregate)</div>
                  <div>• RabbitMQ & AMQP Message Broker</div>
                  <div>• Redis 7 (Caching & Lockout)</div>
                  <div>• React 18, TypeScript & Vite</div>
                  <div>• STOMP WebSocket Live Ticker</div>
                  <div>• Recharts & TailwindCSS</div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-white/10 bg-[#12151C]/60 flex justify-between items-center text-xs">
          <span className="text-slate-500">MyWallet v0.1.0 · Licence 2 Informatique</span>
          <button 
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white font-medium transition-colors"
          >
            {lang === "fr" ? "J'ai compris, explorer la plateforme" : "Got it, explore platform"}
          </button>
        </div>
      </div>
    </div>
  );
}
