import { Link } from "react-router-dom";
import { useI18nStore } from "../i18n/useI18n";
import Breadcrumb from "../components/Breadcrumb";

export default function PrivacyPage() {
  const { lang } = useI18nStore();

  return (
    <div className="p-8 max-w-4xl mx-auto space-y-8 animate-fadeIn text-slate-200">
      <Breadcrumb
        items={[
          { label: lang === "fr" ? "Confidentialité & RGPD" : "Privacy Policy" },
        ]}
      />

      <div className="space-y-3 border-b border-white/10 pb-6">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold">
          ⚖️ {lang === "fr" ? "Conformité Légale & RGPD" : "Legal Compliance & GDPR"}
        </div>
        <h1 className="text-3xl font-bold text-white tracking-tight">
          {lang === "fr" ? "Politique de Confidentialité" : "Privacy Policy"}
        </h1>
        <p className="text-sm text-slate-400">
          {lang === "fr"
            ? "Dernière mise à jour : 1er Octobre 2026. Transparence totale sur la protection de vos données."
            : "Last updated: October 1, 2026. Complete transparency on how your data is protected."}
        </p>
      </div>

      <div className="space-y-6 text-sm leading-relaxed text-slate-300">
        <section className="space-y-2 rounded-2xl bg-[#0F121A]/80 border border-white/5 p-6">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <span>🛡️</span> {lang === "fr" ? "1. Cadre du Simulateur & Données Collectées" : "1. Simulator Scope & Collected Data"}
          </h2>
          <p>
            {lang === "fr"
              ? "MyWallet est une plateforme de simulation académique et pédagogique. Aucune information bancaire réelle, aucun numéro de carte et aucune coordonnée financière n'est jamais demandée ni enregistrée."
              : "MyWallet is an educational trading simulator. No real banking details, credit card numbers, or real financial information are ever requested or stored."}
          </p>
          <p>
            {lang === "fr"
              ? "Les seules données collectées lors de votre inscription sont votre adresse email (identifiant de compte) et l'empreinte cryptographique sécurisée de votre mot de passe (haché via l'algorithme BCrypt avec sel unique)."
              : "The only data collected upon registration is your email address (account identifier) and the salted cryptographic hash of your password (via BCrypt)."}
          </p>
        </section>

        <section className="space-y-2 rounded-2xl bg-[#0F121A]/80 border border-white/5 p-6">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <span>🔐</span> {lang === "fr" ? "2. Sécurité & Mesures Techniques" : "2. Security & Technical Safeguards"}
          </h2>
          <ul className="list-disc list-inside space-y-1 text-slate-400">
            <li>
              {lang === "fr"
                ? "Authentification par jetons JWT stateless signés avec HMAC-SHA512."
                : "Stateless JWT authentication signed with HMAC-SHA512."}
            </li>
            <li>
              {lang === "fr"
                ? "Protection anti-force brute : verrouillage automatique du compte pendant 15 minutes après 5 échecs consécutifs."
                : "Brute-force protection: automatic 15-minute account lockout after 5 failed attempts."}
            </li>
            <li>
              {lang === "fr"
                ? "Limiteur de débit réseau (Rate Limiter) sur l'ensemble des points d'accès sensibles d'authentification."
                : "Rate limiter on all sensitive authentication endpoints."}
            </li>
            <li>
              {lang === "fr"
                ? "Journalisation sécurisée : aucun mot de passe ou secret n'apparaît dans les journaux applicatifs."
                : "Secure logging: no passwords or secrets are ever recorded in application logs."}
            </li>
          </ul>
        </section>

        <section className="space-y-2 rounded-2xl bg-[#0F121A]/80 border border-white/5 p-6">
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <span>👤</span> {lang === "fr" ? "3. Vos Droits Informatique & Libertés (RGPD)" : "3. Your Rights Under GDPR"}
          </h2>
          <p>
            {lang === "fr"
              ? "Conformément au Règlement Général sur la Protection des Données (RGPD), vous disposez à tout moment d'un droit d'accès, de rectification, de portabilité et de suppression de vos données personnelles."
              : "Under GDPR regulations, you hold the right to access, rectify, export, and delete your personal data at any time."}
          </p>
          <p>
            {lang === "fr"
              ? "Pour exercer ces droits ou pour toute question relative à vos données, vous pouvez contacter l'administrateur à : privacy@mywallet.dev."
              : "To exercise these rights, contact our privacy contact at: privacy@mywallet.dev."}
          </p>
        </section>

        <div className="pt-4 flex items-center justify-between border-t border-white/10">
          <Link
            to="/terms"
            className="text-xs text-indigo-400 hover:text-indigo-300 underline underline-offset-2"
          >
            {lang === "fr" ? "Consulter les Conditions Générales d'Utilisation (CGU)" : "View Terms of Service"}
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
