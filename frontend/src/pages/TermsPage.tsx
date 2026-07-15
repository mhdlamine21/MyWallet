import { Link } from "react-router-dom";
import { useI18nStore } from "../i18n/useI18n";
import Breadcrumb from "../components/Breadcrumb";

export default function TermsPage() {
  const { lang } = useI18nStore();

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8 animate-fadeIn text-slate-200">
      <Breadcrumb
        items={[
          { label: lang === "fr" ? "Conditions d'Utilisation" : "Terms of Service" },
        ]}
      />

      <div className="space-y-3 border-b border-white/10 pb-6">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs font-semibold">
          📜 {lang === "fr" ? "Avertissement Légal & CGU" : "Legal Notice & Terms"}
        </div>
        <h1 className="text-3xl font-bold text-white tracking-tight">
          {lang === "fr" ? "Conditions Générales d'Utilisation (CGU)" : "Terms of Service"}
        </h1>
        <p className="text-sm text-slate-400">
          {lang === "fr"
            ? "Cadre juridique et avertissement réglementaire sur la simulation financière MyWallet."
            : "Legal framework and regulatory notice for the MyWallet financial simulator."}
        </p>
      </div>

      <div className="space-y-6 text-sm leading-relaxed text-slate-300">
        <div className="p-5 rounded-2xl bg-amber-950/20 border border-amber-500/30 text-amber-200 space-y-2">
          <h3 className="font-bold text-white flex items-center gap-2">
            <span>⚠️</span> {lang === "fr" ? "Avertissement Financier Majeur" : "Major Financial Disclaimer"}
          </h3>
          <p className="text-xs leading-relaxed text-amber-200/90">
            {lang === "fr"
              ? "MyWallet est un simulateur purement éducatif et technologique. Aucun ordre n'est transmis à un courtier réel, aucun actif financier réel n'est négocié, et aucun argent réel n'est impliqué. Rien dans cette application ne constitue un conseil en investissement ou une incitation à trader."
              : "MyWallet is an educational trading simulator showcase. No real broker orders are executed, no real financial assets are traded, and no real capital is involved. Nothing in this application constitutes financial or investment advice."}
          </p>
        </div>

        <section className="space-y-2 rounded-2xl bg-[#0F121A]/80 border border-white/5 p-6">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <span>🎯</span> {lang === "fr" ? "1. Objet du Service" : "1. Purpose of the Platform"}
          </h2>
          <p>
            {lang === "fr"
              ? "La plateforme met à disposition des portefeuilles virtuels, un générateur stochastique de données de marché (GBM), un moteur d'exécution de règles AST sans eval() et des simulations de Monte Carlo afin de tester des stratégies quantitatives en environnement simulé."
              : "The platform offers virtual simulated portfolios, a stochastic Geometric Brownian Motion price engine, a safe AST rule engine, and Monte Carlo confidence bounds."}
          </p>
        </section>

        <section className="space-y-2 rounded-2xl bg-[#0F121A]/80 border border-white/5 p-6">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <span>🎓</span> {lang === "fr" ? "2. Genèse Académique & Propriété Intellectuelle" : "2. Academic Origin & Intellectual Property"}
          </h2>
          <p>
            {lang === "fr"
              ? "Ce projet a été conçu à la suite de l'examen final du Semestre 4 de Licence 2 Informatique en Analyse et Conception de Systèmes Orientés Objet, pour démontrer la mise en œuvre pratique d'une architecture hexagonale événementielle distribuée."
              : "This project originated from a final examination in Object-Oriented Analysis and Design (Computer Science Bachelor, Semester 4), developed as a production-grade hexagonal architecture showcase."}
          </p>
        </section>

        <section className="space-y-2 rounded-2xl bg-[#0F121A]/80 border border-white/5 p-6">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <span>🛡️</span> {lang === "fr" ? "3. Coupe-Circuit & Responsabilité" : "3. Kill Switch & Platform Guardrails"}
          </h2>
          <p>
            {lang === "fr"
              ? "L'administrateur de la plateforme dispose d'un Kill Switch d'urgence permettant d'interrompre instantanément le passage d'ordres ou les exécutions de stratégies en cas de comportement anormal détecté par le RiskEngine ou l'Isolation Forest."
              : "The platform features a real-time Kill Switch to freeze simulated order submission in the event of anomalous system behavior."}
          </p>
        </section>

        <div className="pt-4 flex items-center justify-between border-t border-white/10">
          <Link
            to="/privacy"
            className="text-xs text-indigo-400 hover:text-indigo-300 underline underline-offset-2"
          >
            {lang === "fr" ? "Politique de confidentialité (RGPD)" : "Privacy Policy (GDPR)"}
          </Link>
          <Link
            to="/dashboard"
            className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-md shadow-indigo-600/20"
          >
            {lang === "fr" ? "Retour au tableau de bord" : "Back to Dashboard"}
          </Link>
        </div>
      </div>
    </div>
  );
}
