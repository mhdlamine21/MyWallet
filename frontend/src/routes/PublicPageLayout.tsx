/**
 * PublicPageLayout.tsx
 *
 * Layout minimal pour les pages publiques accessibles sans authentification :
 * - Politique de confidentialité (/privacy)
 * - Conditions d'utilisation (/terms)
 *
 * Ce layout est nécessaire car ces pages doivent rester accessibles
 * depuis la page de connexion, avant même que l'utilisateur soit connecté.
 *
 * Contient : un header simplifié avec le logo, et un lien de retour vers /login.
 */

import { Link } from "react-router-dom";
import Logo from "../components/Logo";
import { Outlet } from "react-router-dom";

export default function PublicPageLayout() {
  return (
    // Conteneur plein écran avec fond marine sombre, cohérent avec l'identité visuelle
    <div className="min-h-screen bg-[#091224] text-slate-100 font-sans">
      {/* Header minimal - pas de navigation complète, juste le logo et un retour */}
      <header className="sticky top-0 z-30 bg-[#0D1933] border-b border-ocean-500/20 px-4 sm:px-8 py-3 flex items-center justify-between shadow-md">
        {/* Logo cliquable - redirige vers login (pas dashboard, l'utilisateur n'est pas connecté) */}
        <Link to="/login" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
          <Logo variant="full" size="sm" />
        </Link>

        {/* Lien de retour clair pour ne pas perdre l'utilisateur */}
        <Link
          to="/login"
          className="text-xs text-ocean-300 hover:text-white border border-ocean-500/30 px-3 py-1.5 rounded-xl transition-colors"
        >
          &larr; Retour connexion
        </Link>
      </header>

      {/* Contenu de la page (PrivacyPage ou TermsPage injecté ici) */}
      <main className="max-w-4xl mx-auto px-4 py-8">
        <Outlet />
      </main>

      {/* Footer minimaliste */}
      <footer className="border-t border-ocean-500/20 bg-[#070E1C] px-8 py-4 text-center text-[11px] text-slate-500 font-mono">
        © 2026 MyWallet - Simulateur de Trading Académique
      </footer>
    </div>
  );
}
