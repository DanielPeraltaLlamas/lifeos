const q = new URLSearchParams(location.search);
const url = q.get("url") || "";
const reason = q.get("reason");
const until = Number(q.get("until") || 0);
const title = q.get("title");
const $ = (id) => document.getElementById(id);

let host = "";
try { host = new URL(url).hostname.replace(/^www\./, ""); } catch (e) {}
$("site").textContent = host;

if (reason === "always") {
  $("title").textContent = host + " está bloqueada en el ordenador";
  $("text").textContent = "TikTok e Instagram solo se usan en el móvil, con su límite de hora y media al día.";
} else {
  $("label").textContent = "Life OS · modo enfoque";
  $("title").textContent = "Estás en un bloque de enfoque";
  $("text").textContent = title ? "Ahora toca: " + title : "Vuelve a lo tuyo; esto sigue aquí luego.";
  const tick = () => {
    const left = Math.max(0, until - Date.now());
    const s = Math.ceil(left / 1000);
    $("clock").textContent = String(Math.floor(s / 60)).padStart(2, "0") + ":" + String(s % 60).padStart(2, "0");
    if (until && left === 0) {
      $("clock").textContent = "Bloque terminado";
      const back = $("back");
      back.href = url;
      back.hidden = false;
      return;
    }
    setTimeout(tick, 500);
  };
  if (until) tick();
}
