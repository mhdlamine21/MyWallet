import { useState } from "react";
import { useI18nStore } from "../i18n/useI18n";

interface ContactModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SUPPORT_EMAIL = "mouhamedlniang@gmail.com";

export default function ContactModal({ isOpen, onClose }: ContactModalProps) {
  const { lang } = useI18nStore();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [copied, setCopied] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleCopyEmail = () => {
    navigator.clipboard.writeText(SUPPORT_EMAIL);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  const handleDirectMailto = () => {
    const encodedSubject = encodeURIComponent(subject || (lang === "fr" ? "Contact MyWallet Platform" : "MyWallet Platform Inquiry"));
    const encodedBody = encodeURIComponent(
      `Nom: ${name || "Non spécifié"}\nEmail: ${email || "Non spécifié"}\n\nMessage:\n${message}`
    );
    window.open(`mailto:${SUPPORT_EMAIL}?subject=${encodedSubject}&body=${encodedBody}`, "_blank");
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      setError(lang === "fr" ? "Veuillez saisir une adresse email valide." : "Please enter a valid email address.");
      return;
    }
    if (!message.trim() || message.trim().length < 10) {
      setError(
        lang === "fr"
          ? "Le message doit contenir au moins 10 caractères."
          : "Message must contain at least 10 characters."
      );
      return;
    }

    // Attempt direct mailto in background while confirming in UI
    handleDirectMailto();
    setSubmitted(true);
  };

  const resetForm = () => {
    setName("");
    setEmail("");
    setSubject("");
    setMessage("");
    setError(null);
    setSubmitted(false);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="w-full max-w-lg rounded-2xl bg-[#0d1933] border border-ocean-500/30 shadow-2xl p-6 text-slate-200 space-y-5 relative overflow-hidden">
        {/* Glow decorative effect */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-ocean-600/10 rounded-full blur-3xl pointer-events-none" />

        {/* Modal Header */}
        <div className="flex items-center justify-between border-b border-ocean-500/20 pb-4 relative z-10">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-ocean-500/20 text-ocean-300 flex items-center justify-center font-bold text-lg">
              ✉️
            </div>
            <div>
              <h2 className="text-base font-bold text-white">
                {lang === "fr" ? "Contact & Support Technique" : "Contact & Technical Support"}
              </h2>
              <p className="text-[11px] text-slate-400">
                {lang === "fr" ? "Assistance directe pour la plateforme MyWallet" : "Direct assistance for MyWallet platform"}
              </p>
            </div>
          </div>
          <button
            onClick={resetForm}
            className="w-8 h-8 rounded-lg bg-navy-850 hover:bg-white/10 text-slate-400 hover:text-white flex items-center justify-center text-sm transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Recipient Notice Banner */}
        <div className="p-3 rounded-xl bg-[#132347] border border-ocean-500/20 flex items-center justify-between text-xs relative z-10">
          <div className="flex items-center gap-2">
            <span className="text-sm">📬</span>
            <div>
              <span className="text-slate-400 block text-[10px]">
                {lang === "fr" ? "Destinataire officiel des messages :" : "Official message recipient:"}
              </span>
              <span className="font-mono font-semibold text-ocean-300 text-xs">{SUPPORT_EMAIL}</span>
            </div>
          </div>
          <button
            type="button"
            onClick={handleCopyEmail}
            className="px-2.5 py-1 rounded-md bg-ocean-600/30 hover:bg-ocean-600/50 border border-ocean-500/30 text-[11px] text-ocean-200 font-medium transition-colors cursor-pointer"
          >
            {copied ? (lang === "fr" ? "✓ Copié !" : "✓ Copied!") : (lang === "fr" ? "Copier" : "Copy")}
          </button>
        </div>

        {submitted ? (
          <div className="p-6 rounded-xl bg-emerald-950/40 border border-emerald-500/30 text-center space-y-3 relative z-10">
            <div className="text-4xl">✅</div>
            <h3 className="font-bold text-white text-base">
              {lang === "fr" ? "Message prêt & transmis !" : "Message prepared & routed!"}
            </h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              {lang === "fr" ? (
                <>
                  Votre message a été adressé à{" "}
                  <strong className="text-ocean-300 font-mono">{SUPPORT_EMAIL}</strong>. Le client de messagerie s'est ouvert pour confirmation d'envoi.
                </>
              ) : (
                <>
                  Your message has been addressed to{" "}
                  <strong className="text-ocean-300 font-mono">{SUPPORT_EMAIL}</strong>.
                </>
              )}
            </p>
            <div className="pt-2">
              <button
                onClick={resetForm}
                className="px-5 py-2 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white text-xs font-semibold shadow-lg shadow-ocean-600/30 transition-all cursor-pointer"
              >
                {lang === "fr" ? "Fermer la fenêtre" : "Close window"}
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-3.5 text-xs relative z-10">
            {error && (
              <div className="p-2.5 rounded-lg bg-red-950/50 border border-red-500/30 text-red-300 flex items-center gap-2">
                <span>⚠️</span>
                <span>{error}</span>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-slate-300 font-medium block text-[11px]">
                  {lang === "fr" ? "Votre Nom" : "Your Name"}
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="ex: Mouhamadou Lamine"
                  className="w-full px-3 py-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-white placeholder-slate-500 focus:outline-none focus:border-ocean-400 font-sans"
                />
              </div>

              <div className="space-y-1">
                <label className="text-slate-300 font-medium block text-[11px]">
                  {lang === "fr" ? "Votre Email" : "Your Email"} *
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="votre.email@exemple.com"
                  className="w-full px-3 py-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-white placeholder-slate-500 focus:outline-none focus:border-ocean-400 font-sans"
                />
              </div>
            </div>

            <div className="space-y-1">
              <label className="text-slate-300 font-medium block text-[11px]">
                {lang === "fr" ? "Objet de la demande" : "Subject"}
              </label>
              <input
                type="text"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                placeholder={lang === "fr" ? "Ex: Question sur le moteur de règles ou retour de test" : "e.g., Question about rule engine"}
                className="w-full px-3 py-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-white placeholder-slate-500 focus:outline-none focus:border-ocean-400 font-sans"
              />
            </div>

            <div className="space-y-1">
              <label className="text-slate-300 font-medium block text-[11px]">
                {lang === "fr" ? "Message" : "Message"} *
              </label>
              <textarea
                required
                rows={4}
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder={lang === "fr" ? "Détaillez votre message, question ou suggestion..." : "Describe your inquiry or feedback..."}
                className="w-full px-3 py-2 rounded-xl bg-[#132347] border border-ocean-500/20 text-white placeholder-slate-500 focus:outline-none focus:border-ocean-400 resize-none font-sans"
              />
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-ocean-500/20">
              <button
                type="button"
                onClick={handleDirectMailto}
                className="text-[11px] text-ocean-400 hover:text-ocean-300 font-medium underline flex items-center gap-1 cursor-pointer"
              >
                <span>🚀</span>
                <span>{lang === "fr" ? "Ouvrir client email direct" : "Open email client directly"}</span>
              </button>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={resetForm}
                  className="px-3.5 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 transition-colors cursor-pointer"
                >
                  {lang === "fr" ? "Annuler" : "Cancel"}
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-ocean-600 hover:bg-ocean-500 text-white font-semibold shadow-lg shadow-ocean-600/30 transition-all cursor-pointer flex items-center gap-1.5"
                >
                  <span>✉️</span>
                  <span>{lang === "fr" ? "Envoyer à l'équipe" : "Send message"}</span>
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
