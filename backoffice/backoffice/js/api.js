/**
 * Petit client HTTP pour l'API FastAPI du backend Livraison.
 * Pas de framework/bundler : ce fichier est chargé tel quel par index.html.
 */

const Api = (() => {
  const STORAGE_KEY = "livraison_admin_session";

  function loadSession() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }

  function saveSession(session) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }

  function clearSession() {
    localStorage.removeItem(STORAGE_KEY);
  }

  let session = loadSession();

  function getApiBase() {
    return (session && session.apiBase) || "http://127.0.0.1:8000";
  }

  function isLoggedIn() {
    return !!(session && session.token);
  }

  async function login(apiBase, email, password) {
    const res = await fetch(`${apiBase}/admin/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.detail || "Échec de la connexion");
    }
    const data = await res.json();
    session = { apiBase, token: data.access_token, userId: data.user_id };
    saveSession(session);
    return session;
  }

  function logout() {
    session = null;
    clearSession();
  }

  async function request(path, { method = "GET", body } = {}) {
    if (!session || !session.token) {
      throw new Error("Non authentifié");
    }
    const res = await fetch(`${session.apiBase}${path}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${session.token}`,
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    if (res.status === 401) {
      logout();
      throw new Error("Session expirée, reconnecte-toi.");
    }

    if (!res.ok) {
      const errBody = await res.json().catch(() => ({}));
      throw new Error(errBody.detail || `Erreur API (${res.status})`);
    }

    if (res.status === 204) return null;
    return res.json();
  }

  return {
    login,
    logout,
    isLoggedIn,
    getApiBase,
    get: (path) => request(path),
    post: (path, body) => request(path, { method: "POST", body }),
    patch: (path, body) => request(path, { method: "PATCH", body }),
    del: (path) => request(path, { method: "DELETE" }),
  };
})();
