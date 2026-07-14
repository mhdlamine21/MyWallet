import { useState, useRef, useEffect } from "react";
import { useAuthStore } from "../store/authStore";
import { useI18nStore } from "../i18n/useI18n";
import { useCurrencyStore } from "../store/useCurrencyStore";

interface UserNavDropdownProps {
  onOpenGuide: () => void;
  onOpenContact: () => void;
}

export default function UserNavDropdown({ onOpenGuide, onOpenContact }: UserNavDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { user, clear } = useAuthStore();
  const { lang, setLang, t } = useI18nStore();
  const { currency, setCurrency } = useCurrencyStore();

  // Close dropdown on click outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const emailInitial = user?.email ? user.email.charAt(0).toUpperCase() : "U";
  const userRole = user?.roles && user.roles.length > 0 ? user.roles[0].replace("ROLE_", "") : "INVESTOR";

  return (
    <>
      {/* Mobile backdrop for seamless outside clicks */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-xs sm:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}

      <div className="relative inline-block z-40" ref={dropdownRef}>
        {/* Dropdown Toggle Button */}
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className="flex items-center gap-2 px-2.5 sm:px-3 py-1.5 rounded-xl bg-[#0d1933] border border-ocean-500/20 hover:border-ocean-500/50 hover:bg-[#132347] transition-all shadow-sm cursor-pointer group"
          aria-expanded={isOpen}
          aria-label="Menu utilisateur"
        >
          {/* User Avatar */}
          <div className="w-7 h-7 rounded-lg bg-ocean-600 flex items-center justify-center font-bold text-xs text-white shadow-md shadow-ocean-600/30 group-hover:scale-105 transition-transform shrink-0">
            {emailInitial}
          </div>

          {/* User summary (hidden on very small mobile) */}
          <div className="hidden md:block text-left">
            <div className="text-xs font-semibold text-white leading-tight max-w-[130px] truncate">
              {user?.email ?? "Utilisateur"}
            </div>
            <div className="text-[10px] text-ocean-300 font-mono leading-tight">
              {userRole}
            </div>
          </div>

          {/* Caret icon */}
          <span className={`text-[10px] text-slate-400 transition-transform duration-200 shrink-0 ${isOpen ? "rotate-180 text-ocean-300" : ""}`}>
            ▼
          </span>
        </button>

        {/* Popover Dropdown Menu - 100% Mobile Safe */}
        {isOpen && (
          <div
            className="fixed sm:absolute right-3 sm:right-0 top-14 sm:top-auto sm:mt-2 w-[calc(100vw-1.5rem)] sm:w-72 max-w-xs rounded-2xl bg-[#0d1933] border border-ocean-500/30 shadow-2xl p-4 text-xs z-50 animate-fadeIn space-y-3.5 backdrop-blur-xl"
            style={{ maxHeight: "calc(100vh - 5rem)", overflowY: "auto" }}
          >
            {/* User Info Header */}
            <div className="pb-3 border-b border-ocean-500/20">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-ocean-600 flex items-center justify-center text-sm font-bold text-white shadow-md shadow-ocean-600/30 shrink-0">
                  {emailInitial}
                </div>
                <div className="overflow-hidden min-w-0">
                  <p className="font-semibold text-white truncate text-xs">{user?.email}</p>
                  <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                    <span className="text-[9px] font-mono font-bold px-1.5 py-0.5 rounded bg-ocean-500/20 text-ocean-300 border border-ocean-500/30 shrink-0">
                      {userRole}
                    </span>
                    <span className="text-[9px] font-mono text-emerald-400 flex items-center gap-1 shrink-0">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                      En ligne
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Language Selector */}
            <div className="flex items-center justify-between p-2 rounded-xl bg-[#132347] border border-ocean-500/20">
              <span className="text-[11px] text-slate-300 font-medium flex items-center gap-1.5">
                <span>🌐</span>
                <span>{lang === "fr" ? "Langue" : "Language"}</span>
              </span>
              <div className="flex gap-1">
                <button
                  type="button"
                  onClick={() => setLang("fr")}
                  className={`px-2 py-1 text-[10px] font-bold rounded cursor-pointer transition-all ${
                    lang === "fr" ? "bg-ocean-600 text-white shadow-sm" : "text-slate-400 hover:text-white"
                  }`}
                >
                  FR 🇫🇷
                </button>
                <button
                  type="button"
                  onClick={() => setLang("en")}
                  className={`px-2 py-1 text-[10px] font-bold rounded cursor-pointer transition-all ${
                    lang === "en" ? "bg-ocean-600 text-white shadow-sm" : "text-slate-400 hover:text-white"
                  }`}
                >
                  EN 🇬🇧
                </button>
              </div>
            </div>

            {/* Currency Switcher */}
            <div className="flex items-center justify-between p-2 rounded-xl bg-[#132347] border border-ocean-500/20">
              <span className="text-[11px] text-slate-300 font-medium flex items-center gap-1.5">
                <span>💱</span>
                <span>{lang === "fr" ? "Devise" : "Currency"}</span>
              </span>
              <div className="flex gap-1">
                <button
                  type="button"
                  onClick={() => setCurrency("FCFA")}
                  className={`px-2 py-1 text-[10px] font-bold rounded cursor-pointer transition-all ${
                    currency === "FCFA" ? "bg-ocean-600 text-white shadow-sm" : "text-slate-400 hover:text-white"
                  }`}
                  title="Franc CFA (XOF/XAF)"
                >
                  FCFA
                </button>
                <button
                  type="button"
                  onClick={() => setCurrency("USD")}
                  className={`px-2 py-1 text-[10px] font-bold rounded cursor-pointer transition-all ${
                    currency === "USD" ? "bg-ocean-600 text-white shadow-sm" : "text-slate-400 hover:text-white"
                  }`}
                  title="Dollar américain ($)"
                >
                  USD ($)
                </button>
              </div>
            </div>

            {/* Quick Actions (Guide & Contact) */}
            <div className="space-y-1">
              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  onOpenGuide();
                }}
                className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-[#132347] text-left text-slate-300 hover:text-white transition-colors cursor-pointer"
              >
                <span className="text-sm">📖</span>
                <span>{lang === "fr" ? "Manuel d'utilisation" : "User Guide & Tutorial"}</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  onOpenContact();
                }}
                className="w-full flex items-center gap-2 px-3 py-2 rounded-xl hover:bg-[#132347] text-left text-slate-300 hover:text-white transition-colors cursor-pointer"
              >
                <span className="text-sm">✉️</span>
                <div className="truncate min-w-0">
                  <div className="truncate">{lang === "fr" ? "Support & Contact" : "Support & Contact"}</div>
                  <div className="text-[9px] text-ocean-300 font-mono truncate">mouhamedlniang@gmail.com</div>
                </div>
              </button>
            </div>

            {/* Sign Out Button */}
            <div className="pt-2 border-t border-ocean-500/20">
              <button
                type="button"
                onClick={() => {
                  setIsOpen(false);
                  clear();
                }}
                className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-rose-950/40 hover:bg-rose-900/60 text-rose-300 hover:text-rose-100 border border-rose-500/30 text-xs font-semibold transition-all cursor-pointer shadow-sm"
              >
                <span>🚪</span>
                <span>{t("nav.signout")}</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
