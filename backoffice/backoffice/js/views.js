/**
 * Rendu des différentes pages du back-office.
 * Chaque fonction reçoit l'élément DOM #app-content et le remplit.
 */

const Views = (() => {

  // --- Aides ---

  function esc(str) {
    if (str === null || str === undefined) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;");
  }

  function fcfa(n) {
    if (n === null || n === undefined) return "—";
    return `${Number(n).toLocaleString("fr-FR")} FCFA`;
  }

  function fdate(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" });
  }

  function badge(value, kind = "neutral") {
    return `<span class="badge badge-${esc(kind)}">${esc(value)}</span>`;
  }

  const ORDER_STATUS_LABELS = {
    pending: "en attente", accepted: "acceptée", heading_to_pickup: "vers collecte",
    at_pickup: "au point de collecte", heading_to_dropoff: "vers livraison",
    at_dropoff: "au point de livraison", delivered: "livrée", paid: "payée",
    cancelled: "annulée",
  };

  const ORDER_STATUS_KIND = {
    pending: "pending", accepted: "pending", heading_to_pickup: "pending",
    at_pickup: "pending", heading_to_dropoff: "pending", at_dropoff: "pending",
    delivered: "delivered", paid: "paid", cancelled: "cancelled",
  };

  function orderBadge(status) {
    return badge(ORDER_STATUS_LABELS[status] || status, ORDER_STATUS_KIND[status] || "neutral");
  }

  function courierBadge(status) {
    const labels = { not_submitted: "aucun dossier", pending: "en attente", validated: "validé", rejected: "rejeté" };
    return badge(labels[status] || status, status);
  }

  function disputeBadge(status) {
    const labels = { open: "ouvert", in_review: "en cours", resolved: "résolu" };
    return badge(labels[status] || status, status);
  }

  function showError(container, err) {
    container.innerHTML = `<div class="panel"><p class="error-message">${esc(err.message || err)}</p></div>`;
  }

  // --- Dashboard ---

  async function renderDashboard(container) {
    container.innerHTML = `<div class="empty-state">Chargement…</div>`;
    try {
      const stats = await Api.get("/admin/dashboard");
      const statusRows = Object.entries(stats.orders_by_status)
        .filter(([, count]) => count > 0)
        .map(([status, count]) => `<tr><td>${orderBadge(status)}</td><td>${count}</td></tr>`)
        .join("") || `<tr><td colspan="2" class="muted">Aucune commande pour l'instant</td></tr>`;

      container.innerHTML = `
        <div class="stat-grid">
          <div class="stat-card"><div class="label">Commandes totales</div><div class="value">${stats.orders_total}</div></div>
          <div class="stat-card"><div class="label">Revenu (commandes payées)</div><div class="value">${fcfa(stats.revenue_total)}</div></div>
          <div class="stat-card"><div class="label">Commission plateforme</div><div class="value">${fcfa(stats.commission_total)}</div></div>
          <div class="stat-card"><div class="label">Livreurs validés</div><div class="value">${stats.couriers_validated}</div></div>
          <div class="stat-card"><div class="label">Dossiers en attente</div><div class="value">${stats.couriers_pending}</div></div>
          <div class="stat-card"><div class="label">Clients inscrits</div><div class="value">${stats.clients_total}</div></div>
          <div class="stat-card"><div class="label">Litiges ouverts</div><div class="value">${stats.disputes_open}</div></div>
        </div>
        <div class="panel">
          <h3>Commandes par statut</h3>
          <table><thead><tr><th>Statut</th><th>Nombre</th></tr></thead><tbody>${statusRows}</tbody></table>
        </div>
      `;
    } catch (err) {
      showError(container, err);
    }
  }

  // --- Livreurs ---

  async function renderCouriers(container, state) {
    const filter = state.courierFilter || "";
    container.innerHTML = `
      <div class="panel">
        <div class="toolbar">
          <label class="muted">Filtrer par statut :</label>
          <select id="courier-status-filter">
            <option value="">Tous</option>
            <option value="pending">En attente</option>
            <option value="validated">Validés</option>
            <option value="rejected">Rejetés</option>
            <option value="not_submitted">Sans dossier</option>
          </select>
        </div>
        <div id="courier-table-wrap"><div class="empty-state">Chargement…</div></div>
      </div>
    `;
    const select = container.querySelector("#courier-status-filter");
    select.value = filter;
    select.addEventListener("change", () => {
      state.courierFilter = select.value;
      loadCouriers();
    });

    async function loadCouriers() {
      const wrap = container.querySelector("#courier-table-wrap");
      wrap.innerHTML = `<div class="empty-state">Chargement…</div>`;
      try {
        const qs = state.courierFilter ? `?status=${encodeURIComponent(state.courierFilter)}` : "";
        const couriers = await Api.get(`/admin/couriers${qs}`);
        if (couriers.length === 0) {
          wrap.innerHTML = `<div class="empty-state">Aucun livreur pour ce filtre.</div>`;
          return;
        }
        wrap.innerHTML = `
          <table>
            <thead><tr>
              <th>Nom</th><th>Téléphone</th><th>Véhicule</th><th>Plaque</th>
              <th>Dossier</th><th>Statut</th><th>Note</th><th>Gains</th><th>Actions</th>
            </tr></thead>
            <tbody>
              ${couriers.map((c) => `
                <tr data-user-id="${esc(c.user_id)}">
                  <td>${esc(c.full_name || "—")}</td>
                  <td>${esc(c.phone)}</td>
                  <td>${esc(c.vehicle_type)}</td>
                  <td>${esc(c.plate_number || "—")}</td>
                  <td>${c.id_document_uploaded ? "✅ pièce" : "❌ pièce"} / ${c.vehicle_photo_uploaded ? "✅ véhicule" : "❌ véhicule"}</td>
                  <td>${courierBadge(c.status)}</td>
                  <td>${c.rating_avg?.toFixed?.(1) ?? c.rating_avg} (${c.rating_count})</td>
                  <td>${fcfa(c.total_earnings)}</td>
                  <td>
                    ${c.status !== "validated" ? `<button class="btn btn-success" data-action="validate">Valider</button>` : ""}
                    ${c.status !== "rejected" ? `<button class="btn btn-danger" data-action="reject">Rejeter</button>` : ""}
                  </td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        `;

        wrap.querySelectorAll("button[data-action]").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const row = btn.closest("tr");
            const userId = row.dataset.userId;
            const action = btn.dataset.action;
            btn.disabled = true;
            try {
              await Api.patch(`/admin/couriers/${encodeURIComponent(userId)}/${action}`, { note: null });
              loadCouriers();
            } catch (err) {
              alert(err.message);
              btn.disabled = false;
            }
          });
        });
      } catch (err) {
        showError(wrap, err);
      }
    }

    loadCouriers();
  }

  // --- Zones tarifaires ---

  async function renderZones(container) {
    container.innerHTML = `
      <div class="panel">
        <h3>Créer une zone tarifaire</h3>
        <p class="section-note">
          La zone marquée « par défaut » fixe le tarif appliqué à toutes les
          estimations/commandes (pas encore de découpage géographique par
          zone réelle — ça demande PostGIS, prévu en V1).
        </p>
        <form id="zone-form" class="form-grid">
          <div><label>Nom de la zone</label><input type="text" id="z-nom" required placeholder="N'Djamena centre" /></div>
          <div><label>Prise en charge (FCFA)</label><input type="number" id="z-base" required min="0" value="500" /></div>
          <div><label>Tarif / km (FCFA)</label><input type="number" id="z-km" required min="0" value="250" /></div>
          <div><label>Multiplicateur heure de pointe</label><input type="number" id="z-mult" step="0.1" min="1" value="1.0" /></div>
          <div class="checkbox-row"><input type="checkbox" id="z-default" /><label for="z-default">Zone par défaut</label></div>
          <div class="checkbox-row"><input type="checkbox" id="z-actif" checked /><label for="z-actif">Active</label></div>
          <div><button type="submit" class="btn btn-primary">Ajouter la zone</button></div>
        </form>
      </div>
      <div class="panel">
        <h3>Zones existantes</h3>
        <div id="zones-table-wrap"><div class="empty-state">Chargement…</div></div>
      </div>
    `;

    container.querySelector("#zone-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const payload = {
        nom_zone: container.querySelector("#z-nom").value.trim(),
        tarif_base: Number(container.querySelector("#z-base").value),
        tarif_km: Number(container.querySelector("#z-km").value),
        heure_pointe_multiplicateur: Number(container.querySelector("#z-mult").value) || 1.0,
        is_default: container.querySelector("#z-default").checked,
        actif: container.querySelector("#z-actif").checked,
      };
      try {
        await Api.post("/admin/zones", payload);
        e.target.reset();
        container.querySelector("#z-mult").value = "1.0";
        container.querySelector("#z-actif").checked = true;
        loadZones();
      } catch (err) {
        alert(err.message);
      }
    });

    async function loadZones() {
      const wrap = container.querySelector("#zones-table-wrap");
      wrap.innerHTML = `<div class="empty-state">Chargement…</div>`;
      try {
        const zones = await Api.get("/admin/zones");
        if (zones.length === 0) {
          wrap.innerHTML = `<div class="empty-state">Aucune zone configurée — le tarif par défaut du backend (config.py) s'applique.</div>`;
          return;
        }
        wrap.innerHTML = `
          <table>
            <thead><tr>
              <th>Zone</th><th>Prise en charge</th><th>Tarif/km</th><th>Multiplicateur</th>
              <th>Par défaut</th><th>Active</th><th>Actions</th>
            </tr></thead>
            <tbody>
              ${zones.map((z) => `
                <tr data-id="${esc(z.id)}">
                  <td>${esc(z.nom_zone)}</td>
                  <td>${fcfa(z.tarif_base)}</td>
                  <td>${fcfa(z.tarif_km)}</td>
                  <td>×${z.heure_pointe_multiplicateur}</td>
                  <td>${z.is_default ? badge("oui", "validated") : badge("non", "neutral")}</td>
                  <td>${z.actif ? badge("oui", "validated") : badge("non", "rejected")}</td>
                  <td>
                    ${!z.is_default ? `<button class="btn btn-primary" data-action="set-default">Définir par défaut</button>` : ""}
                    <button class="btn btn-danger" data-action="delete">Supprimer</button>
                  </td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        `;

        wrap.querySelectorAll("button[data-action='delete']").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const id = btn.closest("tr").dataset.id;
            if (!confirm("Supprimer cette zone ?")) return;
            try {
              await Api.del(`/admin/zones/${encodeURIComponent(id)}`);
              loadZones();
            } catch (err) {
              alert(err.message);
            }
          });
        });

        wrap.querySelectorAll("button[data-action='set-default']").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const id = btn.closest("tr").dataset.id;
            const zone = zones.find((z) => z.id === id);
            try {
              await Api.patch(`/admin/zones/${encodeURIComponent(id)}`, {
                nom_zone: zone.nom_zone,
                tarif_base: zone.tarif_base,
                tarif_km: zone.tarif_km,
                heure_pointe_multiplicateur: zone.heure_pointe_multiplicateur,
                is_default: true,
                actif: zone.actif,
              });
              loadZones();
            } catch (err) {
              alert(err.message);
            }
          });
        });
      } catch (err) {
        showError(wrap, err);
      }
    }

    loadZones();
  }

  // --- Commandes ---

  async function renderOrders(container, state) {
    container.innerHTML = `
      <div class="panel">
        <div class="toolbar">
          <label class="muted">Filtrer par statut :</label>
          <select id="order-status-filter">
            <option value="">Tous</option>
            ${Object.entries(ORDER_STATUS_LABELS).map(([v, l]) => `<option value="${v}">${l}</option>`).join("")}
          </select>
        </div>
        <div id="orders-table-wrap"><div class="empty-state">Chargement…</div></div>
      </div>
    `;
    const select = container.querySelector("#order-status-filter");
    select.value = state.orderFilter || "";
    select.addEventListener("change", () => {
      state.orderFilter = select.value;
      loadOrders();
    });

    async function loadOrders() {
      const wrap = container.querySelector("#orders-table-wrap");
      wrap.innerHTML = `<div class="empty-state">Chargement…</div>`;
      try {
        const qs = state.orderFilter ? `?status=${encodeURIComponent(state.orderFilter)}` : "";
        const orders = await Api.get(`/admin/orders${qs}`);
        if (orders.length === 0) {
          wrap.innerHTML = `<div class="empty-state">Aucune commande pour ce filtre.</div>`;
          return;
        }
        wrap.innerHTML = `
          <table>
            <thead><tr>
              <th>ID</th><th>Départ</th><th>Arrivée</th><th>Colis</th>
              <th>Distance</th><th>Prix</th><th>Statut</th><th>Paiement</th><th>Créée le</th>
            </tr></thead>
            <tbody>
              ${orders.map((o) => `
                <tr>
                  <td>${esc(o.id)}</td>
                  <td>${esc(o.pickup_address)}</td>
                  <td>${esc(o.dropoff_address)}</td>
                  <td>${esc(o.parcel_type)}</td>
                  <td>${o.distance_km} km</td>
                  <td>${fcfa(o.price)}</td>
                  <td>${orderBadge(o.status)}</td>
                  <td>${esc(o.payment_method || "—")}</td>
                  <td>${fdate(o.created_at)}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        `;
      } catch (err) {
        showError(wrap, err);
      }
    }

    loadOrders();
  }

  // --- Litiges ---

  async function renderDisputes(container, state) {
    container.innerHTML = `
      <div class="panel">
        <div class="toolbar">
          <label class="muted">Filtrer par statut :</label>
          <select id="dispute-status-filter">
            <option value="">Tous</option>
            <option value="open">Ouverts</option>
            <option value="in_review">En cours</option>
            <option value="resolved">Résolus</option>
          </select>
        </div>
        <div id="disputes-table-wrap"><div class="empty-state">Chargement…</div></div>
      </div>
    `;
    const select = container.querySelector("#dispute-status-filter");
    select.value = state.disputeFilter || "";
    select.addEventListener("change", () => {
      state.disputeFilter = select.value;
      loadDisputes();
    });

    async function loadDisputes() {
      const wrap = container.querySelector("#disputes-table-wrap");
      wrap.innerHTML = `<div class="empty-state">Chargement…</div>`;
      try {
        const qs = state.disputeFilter ? `?status=${encodeURIComponent(state.disputeFilter)}` : "";
        const disputes = await Api.get(`/admin/disputes${qs}`);
        if (disputes.length === 0) {
          wrap.innerHTML = `<div class="empty-state">Aucun litige pour ce filtre.</div>`;
          return;
        }
        wrap.innerHTML = `
          <table>
            <thead><tr>
              <th>Commande</th><th>Motif</th><th>Description</th><th>Statut</th>
              <th>Résolution</th><th>Créé le</th><th>Actions</th>
            </tr></thead>
            <tbody>
              ${disputes.map((d) => `
                <tr data-id="${esc(d.id)}">
                  <td>${esc(d.order_id)}</td>
                  <td>${esc(d.reason)}</td>
                  <td>${esc(d.description || "—")}</td>
                  <td>${disputeBadge(d.status)}</td>
                  <td>${esc(d.resolution_note || "—")}</td>
                  <td>${fdate(d.created_at)}</td>
                  <td>${d.status !== "resolved" ? `<button class="btn btn-success" data-action="resolve">Résoudre</button>` : ""}</td>
                </tr>
              `).join("")}
            </tbody>
          </table>
        `;

        wrap.querySelectorAll("button[data-action='resolve']").forEach((btn) => {
          btn.addEventListener("click", async () => {
            const id = btn.closest("tr").dataset.id;
            const note = prompt("Note de résolution (visible en interne) :");
            if (note === null) return;
            try {
              await Api.patch(`/admin/disputes/${encodeURIComponent(id)}/resolve`, { resolution_note: note });
              loadDisputes();
            } catch (err) {
              alert(err.message);
            }
          });
        });
      } catch (err) {
        showError(wrap, err);
      }
    }

    loadDisputes();
  }

  return { renderDashboard, renderCouriers, renderZones, renderOrders, renderDisputes };
})();
