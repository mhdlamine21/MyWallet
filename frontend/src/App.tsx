/**
 * App.tsx - Point d'entrée du routage React
 *
 * J'ai organisé les routes en 3 niveaux :
 *  1. Routes publiques sans layout (/login)
 *  2. Routes protégées avec AppLayout + StompProvider (connexion JWT requise)
 *  3. Routes publiques AVEC layout minimal (privacy, terms) - accessibles sans compte
 *
 * NOTE: J'ai fait l'erreur au départ de mettre /privacy et /terms uniquement dans
 * la zone protégée, ce qui les rendait inaccessibles depuis la page de login.
 * Correction apportée : PublicPageLayout pour envelopper ces pages légales.
 *
 * StompProvider est placé ici (au niveau layout) pour que toute la zone authentifiée
 * puisse recevoir les événements WebSocket (prix en temps réel, anomalies).
 */

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import PortfoliosPage from "./pages/PortfoliosPage";
import PortfolioDetailPage from "./pages/PortfolioDetailPage";
import StrategiesPage from "./pages/StrategiesPage";
import StrategyDetailPage from "./pages/StrategyDetailPage";
import PrivacyPage from "./pages/PrivacyPage";
import TermsPage from "./pages/TermsPage";
import NotFoundPage from "./pages/NotFoundPage";
import ProtectedRoute from "./routes/ProtectedRoute";
import AppLayout from "./routes/AppLayout";
import PublicPageLayout from "./routes/PublicPageLayout";
import { StompProvider } from "./hooks/StompProvider";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Page de connexion - pas de layout, fond plein écran */}
        <Route path="/login" element={<LoginPage />} />

        {/* Zone sécurisée : vérifie la présence d'un JWT avant de rendre le contenu */}
        <Route element={<ProtectedRoute />}>
          {/* AppLayout contient la sidebar, le header fixe et le footer */}
          {/* StompProvider initialise la connexion WebSocket STOMP pour les cours en temps réel */}
          <Route
            element={
              <StompProvider>
                <AppLayout />
              </StompProvider>
            }
          >
            {/* Redirection racine vers le dashboard */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/portfolios" element={<PortfoliosPage />} />
            <Route path="/portfolios/:id" element={<PortfolioDetailPage />} />
            <Route path="/strategies" element={<StrategiesPage />} />
            <Route path="/strategies/:id" element={<StrategyDetailPage />} />

            {/* Accessible aussi en zone connectée avec breadcrumb vers dashboard */}
            <Route path="/privacy" element={<PrivacyPage />} />
            <Route path="/terms" element={<TermsPage />} />
          </Route>
        </Route>

        {/* Routes publiques - accessibles sans authentification (depuis la page de login par ex.) */}
        {/* Enveloppées dans PublicPageLayout pour un header/footer minimaliste cohérent */}
        <Route element={<PublicPageLayout />}>
          <Route path="/privacy" element={<PrivacyPage />} />
          <Route path="/terms" element={<TermsPage />} />
        </Route>

        {/* Catch-all : page 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
}
