/**
 * useCurrencyStore.ts - Gestion de la devise active (Zustand)
 *
 * Ce store gère la devise affichée dans toute l'application.
 * Deux devises sont supportées : FCFA (Franc CFA) et USD (Dollar américain).
 *
 * Pourquoi FCFA en priorité ?
 * - L'application cible des étudiants et investisseurs en Afrique de l'Ouest.
 * - Le FCFA est la devise de référence au Sénégal, Mali, Côte d'Ivoire...
 * - Taux de conversion fixe 1 USD = 600 FCFA (taux simplifié pour la simulation).
 *
 * La devise est persistée dans localStorage pour survivre aux rechargements de page.
 *
 * J'aurais voulu ajouter EUR mais ça compliquerait la gestion des taux de change.
 * Pour l'instant on garde simple : FCFA <-> USD.
 */

import { create } from "zustand";

// Devises supportées par l'application
export type Currency = "FCFA" | "USD";

interface CurrencyStore {
  currency: Currency;
  setCurrency: (c: Currency) => void;
  toggleCurrency: () => void;
  // formatMoney : convertit un montant USD dans la devise active et formate l'affichage
  formatMoney: (amountInUSD: number | string | null | undefined, options?: { hideSymbol?: boolean; showCode?: boolean }) => string;
}

// Taux de conversion fixe USD -> FCFA
// NOTE: Dans une vraie application, ce taux viendrait d'une API de change en temps réel.
// Ici on le fixe à 600 pour simplifier (taux indicatif cohérent avec la zone CFA en 2024-2025).
const USD_TO_FCFA_RATE = 600;

export const useCurrencyStore = create<CurrencyStore>((set, get) => ({
  // Charge la devise sauvegardée depuis localStorage, ou FCFA par défaut
  currency: (localStorage.getItem("mywallet_currency") as Currency) || "FCFA",

  // Change la devise active et la persiste dans le localStorage
  setCurrency: (currency: Currency) => {
    localStorage.setItem("mywallet_currency", currency);
    set({ currency });
  },

  // Bascule entre les deux devises disponibles
  toggleCurrency: () => {
    const next: Currency = get().currency === "FCFA" ? "USD" : "FCFA";
    localStorage.setItem("mywallet_currency", next);
    set({ currency: next });
  },

  /**
   * Formate un montant (en USD à l'origine) dans la devise active.
   *
   * @param amountInUSD - Montant en USD (les données backend sont toujours en USD)
   * @param options.hideSymbol - Si true, n'ajoute pas le symbole de devise (ex: pour les inputs)
   *
   * Exemples :
   *   formatMoney(1000)            -> "600 000 FCFA" (en mode FCFA)
   *   formatMoney(1000)            -> "$1,000.00"     (en mode USD)
   *   formatMoney(1000, { hideSymbol: true }) -> "600 000" (sans symbole)
   */
  formatMoney: (amountInUSD, options = {}) => {
    const num = Number(amountInUSD) || 0;
    const { currency } = get();

    if (currency === "FCFA") {
      // Conversion USD -> FCFA puis arrondi (le FCFA n'a pas de centimes)
      const fcfaValue = Math.round(num * USD_TO_FCFA_RATE);

      // Format avec espaces comme séparateurs de milliers (norme française)
      // Exemple : 1 500 000 FCFA
      const formatted = new Intl.NumberFormat("fr-FR", {
        maximumFractionDigits: 0,
        useGrouping: true,
      }).format(fcfaValue);

      if (options.hideSymbol) return formatted;
      return `${formatted} FCFA`;
    } else {
      // Format US avec 2 décimales et virgule comme séparateur de milliers
      // Exemple : $1,500.00
      const formatted = new Intl.NumberFormat("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        useGrouping: true,
      }).format(num);

      if (options.hideSymbol) return formatted;
      return `$${formatted}`;
    }
  },
}));
