// Webs bloqueadas (DECISIONES, Entrega 4). Se comprueba el dominio y sus subdominios.

// Valores por defecto; las listas de verdad llegan del agente (Ajustes de Life OS).

/** Siempre bloqueadas en el ordenador (el límite diario es del móvil). */
let ALWAYS = ["tiktok.com", "instagram.com"];

/** Bloqueadas solo durante un bloque de enfoque. */
let DURING_FOCUS = [
  "youtube.com", "youtu.be",
  "reddit.com", "redd.it",
  "twitch.tv",
  "discord.com", "discord.gg", "discordapp.com",
];

/** Cambia las listas (las manda el agente). */
function setLists(always, duringFocus) {
  if (Array.isArray(always)) ALWAYS = always;
  if (Array.isArray(duringFocus)) DURING_FOCUS = duringFocus;
}

function matches(host, list) {
  host = host.toLowerCase().replace(/\.$/, "");
  return list.some((d) => host === d || host.endsWith("." + d));
}

/** Devuelve "always", "focus" o null para una URL. */
function blockReason(url, focusActive) {
  let host;
  try {
    const u = new URL(url);
    if (u.protocol !== "http:" && u.protocol !== "https:") return null;
    host = u.hostname;
  } catch (e) {
    return null;
  }
  if (matches(host, ALWAYS)) return "always";
  if (focusActive && matches(host, DURING_FOCUS.concat(ALWAYS))) return "focus";
  return null;
}

if (typeof module !== "undefined") module.exports = { blockReason, setLists };
