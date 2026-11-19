// Consulta al agente de Life OS (127.0.0.1:47613) si hay un bloque en marcha y
// redirige a la página de bloqueo las webs de rules.js.

const AGENT = "http://127.0.0.1:47613/status";
let status = { focus: false };

async function refresh() {
  let next = { focus: false };
  try {
    const r = await fetch(AGENT, { cache: "no-store" });
    if (r.ok) next = await r.json();
  } catch (e) {
    // Agente apagado: solo cuentan las webs bloqueadas siempre.
  }
  const started = next.focus && !status.focus;
  status = next;
  if (started) closeOpenTabs();
}

function blockedPage(url, reason) {
  const page = new URL(browser.runtime.getURL("blocked.html"));
  page.searchParams.set("url", url);
  page.searchParams.set("reason", reason);
  if (status.endsAt) page.searchParams.set("until", String(status.endsAt));
  if (status.title) page.searchParams.set("title", status.title);
  return page.toString();
}

/** Al empezar un bloque, las pestañas que ya estaban abiertas también se bloquean. */
async function closeOpenTabs() {
  const tabs = await browser.tabs.query({});
  for (const t of tabs) {
    const reason = t.url && blockReason(t.url, true);
    if (reason) browser.tabs.update(t.id, { url: blockedPage(t.url, reason) });
  }
}

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    const reason = blockReason(details.url, status.focus);
    return reason ? { redirectUrl: blockedPage(details.url, reason) } : {};
  },
  { urls: ["http://*/*", "https://*/*"], types: ["main_frame"] },
  ["blocking"],
);

// Las webs que cambian de página sin recargar (YouTube, Reddit) también se vigilan.
browser.tabs.onUpdated.addListener((tabId, change) => {
  if (!change.url) return;
  const reason = blockReason(change.url, status.focus);
  if (reason) browser.tabs.update(tabId, { url: blockedPage(change.url, reason) });
});

// Cada 15 s mientras el navegador está abierto (y una alarma por si el fondo se duerme).
setInterval(refresh, 15000);
browser.alarms.create("refresh", { periodInMinutes: 0.5 });
browser.alarms.onAlarm.addListener(refresh);
browser.runtime.onStartup.addListener(refresh);
refresh();
