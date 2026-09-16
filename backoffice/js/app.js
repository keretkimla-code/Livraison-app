/**
 * Point d'entrée : gère l'écran de connexion, le routage par hash,
 * et le montage des vues (js/views.js) dans #app-content.
 */

(() => {
  const loginScreen = document.getElementById("login-screen");
  const appShell = document.getElementById("app-shell");
  const loginForm = document.getElementById("login-form");
  const loginError = document.getElementById("login-error");
  const apiBaseInput = document.getElementById("api-base");
  const appContent = document.getElementById("app-content");
  const pageTitle = document.getElementById("page-title");
  const logoutBtn = document.getElementById("logout-btn");

  const PAGE_TITLES = {
    dashboard: "Tableau de bord",
    couriers: "Livreurs",
    zones: "Tarifs & zones",
    orders: "Commandes",
    disputes: "Litiges",
  };

  // État partagé entre les vues (filtres sélectionnés, etc.) — perdu au
  // rechargement de page, ce qui est très bien pour un back-office.
  const viewState = {};

  apiBaseInput.value = localStorage.getItem("livraison_last_api_base") || "http://127.0.0.1:8000";

  function showLogin() {
    loginScreen.classList.remove("hidden");
    appShell.classList.add("hidden");
  }

  function showApp() {
    loginScreen.classList.add("hidden");
    appShell.classList.remove("hidden");
    route();
  }

  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    loginError.textContent = "";
    const apiBase = apiBaseInput.value.trim().replace(/\/+$/, "") || "http://127.0.0.1:8000";
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    localStorage.setItem("livraison_last_api_base", apiBase);

    try {
      await Api.login(apiBase, email, password);
      showApp();
    } catch (err) {
      loginError.textContent = err.message || "Échec de la connexion. Vérifie l'URL de l'API et tes identifiants.";
    }
  });

  logoutBtn.addEventListener("click", () => {
    Api.logout();
    showLogin();
  });

  window.addEventListener("hashchange", route);

  function currentRoute() {
    const hash = window.location.hash.replace(/^#\//, "") || "dashboard";
    return PAGE_TITLES[hash] ? hash : "dashboard";
  }

  function route() {
    const routeName = currentRoute();
    pageTitle.textContent = PAGE_TITLES[routeName];

    document.querySelectorAll(".sidebar nav a").forEach((a) => {
      a.classList.toggle("active", a.dataset.route === routeName);
    });

    switch (routeName) {
      case "dashboard":
        Views.renderDashboard(appContent);
        break;
      case "couriers":
        Views.renderCouriers(appContent, viewState);
        break;
      case "zones":
        Views.renderZones(appContent);
        break;
      case "orders":
        Views.renderOrders(appContent, viewState);
        break;
      case "disputes":
        Views.renderDisputes(appContent, viewState);
        break;
      default:
        Views.renderDashboard(appContent);
    }
  }

  // --- Démarrage ---
  if (Api.isLoggedIn()) {
    showApp();
  } else {
    showLogin();
  }
})();
