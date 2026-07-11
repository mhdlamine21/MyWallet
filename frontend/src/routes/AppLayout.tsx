import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";
import UserGuideModal from "../components/UserGuideModal";
import ContactModal from "../components/ContactModal";
import CookieBanner from "../components/CookieBanner";
import UserNavDropdown from "../components/UserNavDropdown";
import Logo from "../components/Logo";

export default function AppLayout() {
  const { lang, t } = useI18nStore();
  const { currency, setCurrency } = useCurrencyStore();
  const [isGuideOpen, setIsGuideOpen] = useState(false);
  const [isContactOpen, setIsContactOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const navItems = [
    { to: "/dashboard", label: t("nav.dashboard"), icon: "📊" },
    { to: "/portfolios", label: t("nav.portfolios"), icon: "💼" },
    { to: "/strategies", label: t("nav.strategies"), icon: "⚡" },
  ];

  return (
    <div className="min-h-screen bg-[#091224] text-slate-100 flex flex-col md:flex-row font-sans selection:bg-ocean-500/30 selection:text-white">
      {/* Mobile Top Header */}
      <div className="md:hidden flex items-center justify-between px-3 sm:px-4 py-2.5 bg-[#0D1933] border-b border-ocean-500/20 sticky top-0 z-40">
        <div className="flex items-center gap-2">
          <Logo variant="icon" size="sm" />
          <div className="leading-tight">
            <span className="text-sm font-bold text-white tracking-tight block">MyWallet</span>
            <span className="block text-[8px] uppercase font-mono text-ocean-400">Trading Sim</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {/* Top-Right User Dropdown (Dépliable) */}
          <UserNavDropdown
            onOpenGuide={() => setIsGuideOpen(true)}
            onOpenContact={() => setIsContactOpen(true)}
          />

          {/* Hamburger button */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="w-8 h-8 rounded-lg bg-navy-850 border border-ocean-500/30 flex items-center justify-center text-slate-300 hover:text-white"
            aria-label="Toggle navigation menu"
          >
            {isMobileMenuOpen ? "✕" : "☰"}
          </button>
        </div>
      </div>

      {/* Mobile Drawer Overlay */}
      {isMobileMenuOpen && (
        <div
          className="md:hidden fixed inset-0 z-50 bg-black/75 backdrop-blur-sm animate-fadeIn"
          onClick={() => setIsMobileMenuOpen(false)}
        >
          <div
            className="w-4/5 max-w-xs h-full bg-[#0D1933] border-r border-ocean-500/20 p-5 flex flex-col justify-between"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="space-y-6">
              <div className="flex items-center justify-between pb-3 border-b border-ocean-500/20">
                <div className="flex items-center gap-2">
                  <Logo variant="full" size="sm" />
                </div>
                <button
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="w-7 h-7 rounded-lg bg-white/5 text-slate-400 hover:text-white flex items-center justify-center text-xs"
                >
                  ✕
                </button>
              </div>

              {/* Navigation Links */}
              <nav className="flex flex-col gap-1.5">
                {navItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={() => setIsMobileMenuOpen(false)}
                    className={({ isActive }) =>
                      `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium transition-all ${
                        isActive
                          ? "bg-ocean-600 text-white shadow-lg shadow-sky-950/40 ring-1 ring-white/10"
                          : "text-slate-300 hover:text-white hover:bg-white/[0.05]"
                      }`
                    }
                  >
                    <span className="text-base">{item.icon}</span>
                    <span>{item.label}</span>
                  </NavLink>
                ))}
              </nav>

              <button
                onClick={() => {
                  setIsMobileMenuOpen(false);
                  setIsGuideOpen(true);
                }}
                className="w-full flex items-center gap-2.5 p-3 rounded-xl bg-navy-850 border border-ocean-500/20 text-left text-xs font-semibold text-ocean-300"
              >
                <span>📖</span>
                <span>{lang === "fr" ? "Manuel & Tutoriel" : "User Guide & Tutorial"}</span>
              </button>
            </div>

            <div className="pt-4 border-t border-ocean-500/20 space-y-2 text-[11px] text-slate-400">
              <div className="flex items-center justify-between">
                <span>Devise active :</span>
                <span className="font-bold text-ocean-300 font-mono">{currency}</span>
              </div>
              <div className="text-[10px] text-slate-500">
                Gérez votre profil, langue et déconnexion dans le menu dépliable en haut à droite.
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Desktop Sidebar Navigation (Ajustée, Épurée & Redimensionnée) */}
      <aside className="hidden md:flex w-64 border-r border-ocean-500/20 bg-[#0D1933] p-5 flex-col justify-between shrink-0 shadow-2xl">
        <div className="space-y-6">
          {/* Logo & Platform Name */}
          <div className="px-2">
            <Logo variant="full" size="md" />
            <p className="text-[10px] uppercase font-mono tracking-wider text-ocean-400 font-semibold pl-1 mt-1">
              Simulated Trading
            </p>
          </div>

          {/* Navigation Items */}
          <nav className="flex flex-col gap-1.5 pt-2">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium transition-all ${
                    isActive
                      ? "bg-ocean-600 text-white shadow-lg shadow-sky-950/40 ring-1 ring-white/10"
                      : "text-slate-300 hover:text-white hover:bg-white/[0.05]"
                  }`
                }
              >
                <span className="text-sm">{item.icon}</span>
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>

          {/* Interactive Guide Trigger Card */}
          <div className="pt-2">
            <button
              onClick={() => setIsGuideOpen(true)}
              className="w-full flex items-center justify-between p-3 rounded-xl bg-[#132347] border border-ocean-500/20 hover:border-ocean-500/40 text-left transition-all group shadow-sm cursor-pointer"
            >
              <div className="flex items-center gap-2.5">
                <span className="text-base group-hover:scale-110 transition-transform">📖</span>
                <div>
                  <div className="text-xs font-semibold text-ocean-300">
                    {lang === "fr" ? "Guide & Tutoriel" : "User Guide & Tutorial"}
                  </div>
                  <div className="text-[10px] text-slate-400">
                    {lang === "fr" ? "Manuel interactif" : "Interactive manual"}
                  </div>
                </div>
              </div>
              <span className="text-[10px] bg-ocean-500/20 text-ocean-300 px-2 py-0.5 rounded-full font-mono">
                Aide
              </span>
            </button>
          </div>
        </div>

        {/* Sidebar Compact Footer (Sans l'email, la langue ni la déconnexion qui sont dépliables en haut à droite) */}
        <div className="pt-4 border-t border-ocean-500/20 space-y-2">
          <div className="p-3 rounded-xl bg-[#132347] border border-ocean-500/20 space-y-2 text-[11px]">
            <div className="flex items-center justify-between">
              <span className="text-slate-400">{lang === "fr" ? "Moteur" : "Engine"} :</span>
              <span className="text-emerald-400 font-mono font-bold flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                STOMP / EventStore
              </span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-slate-400">{lang === "fr" ? "Devise active" : "Active currency"} :</span>
              <span className="text-ocean-300 font-mono font-bold">{currency}</span>
            </div>
          </div>

          <div className="flex items-center justify-between px-2 text-[10px] text-slate-500 font-mono">
            <span>MyWallet Core</span>
            <span>v0.1.0</span>
          </div>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto flex flex-col min-w-0">
        {/* Top Header avec Bannière et Bouton Dépliable en Haut à Droite */}
        <header className="border-b border-ocean-500/20 bg-[#0A1428] px-4 md:px-8 py-3 flex items-center justify-between text-xs gap-3 sticky top-0 z-30 shadow-md">
          {/* Left status / disclaimer */}
          <div className="flex items-center gap-2 text-slate-300 text-[11px]">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shrink-0"></span>
            <span className="truncate hidden sm:inline">{t("app.disclaimer")}</span>
            <span className="truncate sm:hidden">Simulateur MyWallet</span>
          </div>

          {/* Right Controls: Devise Pill + User Nav Dropdown (Dépliable) */}
          <div className="flex items-center gap-3">
            {/* Quick Currency Toggle Pill */}
            <button
              onClick={() => setCurrency(currency === "FCFA" ? "USD" : "FCFA")}
              className="px-2.5 py-1.5 rounded-xl bg-navy-850 hover:bg-[#132347] border border-ocean-500/30 text-[11px] font-mono text-ocean-300 hover:text-white font-semibold transition-all shadow-sm cursor-pointer"
              title="Cliquer pour basculer la devise (FCFA ↔ USD)"
            >
              Devise : <strong className="text-white">{currency}</strong>
            </button>

            {/* Support shortcut button */}
            <button
              onClick={() => setIsContactOpen(true)}
              className="hidden lg:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-navy-850 hover:bg-[#132347] border border-ocean-500/30 text-slate-300 hover:text-white text-xs font-medium transition-colors cursor-pointer"
              title="Contacter le support à mouhamedlniang@gmail.com"
            >
              <span>✉️</span>
              <span>Support</span>
            </button>

            {/* Menu Déroulant Dépliable (Email, Langue, Déconnexion, etc.) */}
            <UserNavDropdown
              onOpenGuide={() => setIsGuideOpen(true)}
              onOpenContact={() => setIsContactOpen(true)}
            />
          </div>
        </header>

        {/* Content Outlet */}
        <div className="flex-1 w-full min-w-0 overflow-x-hidden bg-gradient-to-b from-[#091224] via-[#0A152B] to-[#0D1933]">
          <Outlet />
        </div>

        {/* Global Footer */}
        <footer className="border-t border-ocean-500/20 bg-[#070E1C] px-4 md:px-8 py-4 text-xs text-slate-400 flex flex-col sm:flex-row items-center justify-between gap-3 text-center sm:text-left">
          <div className="flex items-center gap-2 flex-wrap justify-center sm:justify-start">
            <span>© 2026 MyWallet Architecture Showcase</span>
            <span className="text-slate-600 hidden sm:inline">•</span>
            <span className="text-ocean-400 font-mono text-[11px]">v0.1.0-SNAPSHOT</span>
          </div>
          <div className="flex flex-wrap items-center justify-center sm:justify-end gap-x-3 gap-y-1.5 text-[11px]">
            <NavLink to="/privacy" className="hover:text-ocean-300 transition-colors">
              {lang === "fr" ? "Confidentialité (RGPD)" : "Privacy Policy"}
            </NavLink>
            <span className="text-slate-600">•</span>
            <NavLink to="/terms" className="hover:text-ocean-300 transition-colors">
              {lang === "fr" ? "Conditions (CGU)" : "Terms of Service"}
            </NavLink>
            <span className="text-slate-600">•</span>
            <button
              onClick={() => setIsContactOpen(true)}
              className="hover:text-ocean-300 transition-colors cursor-pointer flex items-center gap-1"
            >
              <span>✉️</span>
              <span>Contact</span>
              <span className="text-slate-500">(mouhamedlniang@gmail.com)</span>
            </button>
          </div>
        </footer>
      </main>

      {/* Guide Modal */}
      <UserGuideModal isOpen={isGuideOpen} onClose={() => setIsGuideOpen(false)} />

      {/* Contact Modal (avec réception sur mouhamedlniang@gmail.com) */}
      <ContactModal isOpen={isContactOpen} onClose={() => setIsContactOpen(false)} />

      {/* Cookie Consent Banner */}
      <CookieBanner />
    </div>
  );
}
