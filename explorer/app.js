const MODES = [
  ["overview", "Overview"],
  ["architecture", "Architecture"],
  ["folders", "Folders"],
  ["tests", "Test Explorer"],
  ["apis", "APIs"],
  ["ui", "UI / Telr"],
  ["flow", "Code Flow"],
  ["dependencies", "Dependencies"],
  ["insights", "Insights"]
];
const AUDIENCES = [
  ["business", "Business"],
  ["developer", "Developer"],
  ["architect", "Architect"]
];

const state = {
  model: null,
  mode: "overview",
  audience: "developer",
  selected: null,
  explain: "simple",
  flow: [],
  flowIndex: -1,
  playing: false,
  timer: null,
  graph: { nodes: [], edges: [] },
  hideUnrelated: false,
  pan: { x: 48, y: 64, scale: 1, drag: false, sx: 0, sy: 0 }
};

const $ = (id) => document.getElementById(id);

async function boot() {
  const res = await fetch("model.json");
  if (!res.ok) throw new Error("model.json missing");
  state.model = await res.json();
  const s = state.model.stats;
  $("meta").textContent = `${s.javaFiles} Java files · ${s.testMethods} tests · ${s.endpoints} APIs · ${state.model.project.java}`;
  renderChrome();
  renderLegend();
  bind();
  render();
}

function renderChrome() {
  $("modes").innerHTML = MODES.map(([id, label]) =>
    `<button type="button" role="tab" aria-selected="${state.mode === id}" data-mode="${id}" class="${state.mode === id ? "active" : ""}">${label}</button>`
  ).join("");
  $("audience").innerHTML = AUDIENCES.map(([id, label]) =>
    `<button type="button" role="tab" aria-selected="${state.audience === id}" data-aud="${id}" class="${state.audience === id ? "active" : ""}">${label}</button>`
  ).join("");
}

function renderLegend() {
  const items = [
    ["#6a9bc3", "solid", "API"],
    ["#8fb36a", "solid", "Test"],
    ["#c48a6a", "solid", "UI"],
    ["#c46d6d", "dash", "External"],
    ["#b9a46a", "solid", "Config / report"]
  ];
  $("legend").innerHTML = items.map(([c, style, t]) =>
    `<span><i class="dot" style="background:${c};${style === "dash" ? "border-radius:0" : ""}"></i>${t}</span>`
  ).join("");
}

function bind() {
  $("modes").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-mode]");
    if (!btn) return;
    state.mode = btn.dataset.mode;
    stopPlay();
    renderChrome();
    render();
  });
  $("audience").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-aud]");
    if (!btn) return;
    state.audience = btn.dataset.aud;
    renderChrome();
    render();
  });
  $("theme").addEventListener("click", () => {
    const light = document.documentElement.dataset.theme !== "light";
    document.documentElement.dataset.theme = light ? "light" : "";
    $("theme").textContent = light ? "Dark" : "Light";
  });
  $("openSearch").addEventListener("click", () => openPalette(""));
  $("simpleBtn").addEventListener("click", () => { state.explain = "simple"; renderPanel(); paintExplain(); });
  $("techBtn").addEventListener("click", () => { state.explain = "technical"; renderPanel(); paintExplain(); });
  $("openDetails").addEventListener("click", () => $("detailsPane").classList.toggle("open"));
  $("overlay").addEventListener("click", (e) => {
    if (e.target.id === "overlay") closePalette();
  });
  $("paletteInput").addEventListener("input", () => drawHits($("paletteInput").value));
  $("paletteInput").addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      const first = document.querySelector(".hit[data-id]");
      if (first) {
        closePalette();
        select(first.dataset.id);
      }
    }
  });
  document.addEventListener("keydown", (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
      e.preventDefault();
      openPalette("");
    }
    if (e.key === "/" && !inField(e.target)) {
      e.preventDefault();
      openPalette("");
    }
    if (e.key === "Escape") {
      closePalette();
      $("detailsPane").classList.remove("open");
    }
    if (e.key === "ArrowRight" && !inField(e.target)) step(1);
    if (e.key === "ArrowLeft" && !inField(e.target)) step(-1);
  });
  $("play").addEventListener("click", togglePlay);
  $("prev").addEventListener("click", () => step(-1));
  $("next").addEventListener("click", () => step(1));
  $("restart").addEventListener("click", () => {
    ensureFlow();
    state.flowIndex = 0;
    paintFlow();
  });
  $("zoomIn").addEventListener("click", () => { state.pan.scale = Math.min(2.6, state.pan.scale + 0.15); applyPan(); });
  $("zoomOut").addEventListener("click", () => { state.pan.scale = Math.max(0.35, state.pan.scale - 0.15); applyPan(); });
  $("fit").addEventListener("click", fitView);
  $("reset").addEventListener("click", () => {
    state.pan = { x: 48, y: 64, scale: 1, drag: false, sx: 0, sy: 0 };
    applyPan();
  });
  $("focusSel").addEventListener("click", () => {
    if (state.selected) select(state.selected);
  });
  $("hideUnrelated").addEventListener("change", (e) => {
    state.hideUnrelated = e.target.checked;
    paintFlow();
  });
  $("exportJson").addEventListener("click", exportJson);
  $("exportMermaid").addEventListener("click", exportMermaid);
  const svg = $("graph");
  svg.addEventListener("mousedown", (e) => {
    state.pan.drag = true;
    state.pan.sx = e.clientX - state.pan.x;
    state.pan.sy = e.clientY - state.pan.y;
  });
  window.addEventListener("mouseup", () => { state.pan.drag = false; });
  window.addEventListener("mousemove", (e) => {
    if (!state.pan.drag) return;
    state.pan.x = e.clientX - state.pan.sx;
    state.pan.y = e.clientY - state.pan.sy;
    applyPan();
  });
  svg.addEventListener("wheel", (e) => {
    e.preventDefault();
    state.pan.scale = Math.min(2.6, Math.max(0.35, state.pan.scale + (e.deltaY > 0 ? -0.08 : 0.08)));
    applyPan();
  }, { passive: false });
}

function inField(el) {
  return el && (el.tagName === "INPUT" || el.tagName === "TEXTAREA");
}

function applyPan() {
  const g = document.getElementById("viewport");
  if (g) g.setAttribute("transform", `translate(${state.pan.x},${state.pan.y}) scale(${state.pan.scale})`);
}

function fitView() {
  const svg = $("graph");
  const nodes = [...svg.querySelectorAll(".node rect")];
  if (!nodes.length) return;
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  nodes.forEach((r) => {
    const x = Number(r.getAttribute("x"));
    const y = Number(r.getAttribute("y"));
    const w = Number(r.getAttribute("width"));
    const h = Number(r.getAttribute("height"));
    minX = Math.min(minX, x);
    minY = Math.min(minY, y);
    maxX = Math.max(maxX, x + w);
    maxY = Math.max(maxY, y + h);
  });
  const vw = svg.clientWidth || 800;
  const vh = svg.clientHeight || 500;
  const scale = Math.min(1.2, Math.max(0.4, Math.min((vw - 80) / (maxX - minX + 1), (vh - 80) / (maxY - minY + 1))));
  state.pan.scale = scale;
  state.pan.x = 40 - minX * scale;
  state.pan.y = 70 - minY * scale;
  applyPan();
}

function classById(id) {
  return state.model.classes.find((c) => c.id === id);
}
function testById(id) {
  return state.model.tests.find((t) => t.id === id);
}
function methodOwner(methodId) {
  return state.model.classes.find((c) => c.methods.some((m) => m.id === methodId));
}
function storyOf(id) {
  return (state.model.storyTraces && state.model.storyTraces[id]) || state.model.traces[id] || [];
}

function render() {
  renderTree();
  const business = state.mode === "overview" && state.audience === "business";
  $("graph").hidden = business;
  $("business").hidden = !business;
  $("toolbar").hidden = business;
  if (business) renderBusiness();
  else renderGraph();
  renderPanel();
  paintExplain();
  if (state.mode === "insights") renderInsights();
}

function renderBusiness() {
  const m = state.model;
  $("business").innerHTML = `
    <div class="kicker">What this system does</div>
    <h2>${escapeHtml(m.project.purpose)}</h2>
    <p class="muted">A diner scans a QR, adds food, pays on Telr, and the kitchen OMS must show the same accepted order. These tests prove that path. They are not mocks.</p>
    <div class="flowline">
      ${["Guest", "QR menu", "Cart", "Telr payment", "Accepted order", "OMS kitchen"].map((s) =>
        `<div class="stepbox"><strong>${s}</strong></div><span class="muted">→</span>`
      ).join("")}
    </div>
    <div class="statgrid">
      <div class="stat"><b>${m.stats.testMethods}</b>Test methods</div>
      <div class="stat"><b>${m.stats.endpoints}</b>HTTP APIs used</div>
      <div class="stat"><b>2</b>Restaurants (test + staging)</div>
      <div class="stat"><b>${m.stats.pageObjects}</b>Browser step (Telr only)</div>
    </div>
    <p class="muted" style="margin-top:16px">Open <strong>Test Explorer</strong> and pick one test to see its whole story. ${escapeHtml(m.coverageNote)}</p>
    <div class="kicker">Start a run</div>
    ${m.entryPoints.map((e) => `<div class="card"><strong>${escapeHtml(e.label)}</strong><div class="muted">${e.kind} · ${escapeHtml(e.target)}</div></div>`).join("")}
  `;
}

function renderTree() {
  const m = state.model;
  if (state.mode === "tests") {
    $("treeTitle").textContent = "Test Explorer";
    const domains = { api: [], checkout: [], shared: [] };
    m.tests.forEach((t) => (domains[t.domain] || domains.shared).push(t));
    const blocks = [
      ["API (no Telr in this method)", domains.api],
      ["Checkout (API + Telr + OMS)", domains.checkout],
      ["Other", domains.shared]
    ];
    const groups = Object.entries(m.groups).map(([g, ids]) =>
      `<div class="kicker">Group ${g}</div><ul class="tree">${ids.map((id) => {
        const t = testById(id);
        return `<li><button type="button" data-sel="${id}" class="${state.selected === id ? "active" : ""}">${t ? t.name : id}</button></li>`;
      }).join("")}</ul>`
    ).join("");
    $("tree").innerHTML = blocks.map(([title, list]) =>
      `<div class="kicker">${title}</div><ul class="tree">${list.map((t) =>
        `<li><button type="button" data-sel="${t.id}" class="${state.selected === t.id ? "active" : ""}">${t.name}</button></li>`
      ).join("")}</ul>`
    ).join("") + groups;
  } else if (state.mode === "apis") {
    $("treeTitle").textContent = "HTTP paths";
    $("tree").innerHTML = `<ul class="tree">${m.endpoints.map((e) =>
      `<li><button type="button" data-sel="${e.class}">${e.method} ${e.path}</button></li>`
    ).join("")}</ul>`;
  } else if (state.mode === "folders") {
    $("treeTitle").textContent = "Packages";
    $("tree").innerHTML = packageTree();
  } else if (state.mode === "flow") {
    $("treeTitle").textContent = "Entry points";
    $("tree").innerHTML = `<ul class="tree">${m.entryPoints.map((e) =>
      `<li><button type="button" data-sel="${e.target}">${e.label}</button></li>`
    ).join("")}</ul>
    <div class="kicker">Tests</div>
    <ul class="tree">${m.tests.map((t) =>
      `<li><button type="button" data-sel="${t.id}">${t.name}</button></li>`
    ).join("")}</ul>`;
  } else {
    $("treeTitle").textContent = "Types";
    const layers = {};
    m.classes.forEach((c) => {
      layers[c.layer] = layers[c.layer] || [];
      layers[c.layer].push(c);
    });
    $("tree").innerHTML = Object.entries(layers).map(([layer, list]) =>
      `<div class="kicker">${layer}</div><ul class="tree">${list.map((c) =>
        `<li><button type="button" data-sel="${c.id}" class="${state.selected === c.id ? "active" : ""}">${c.name}</button></li>`
      ).join("")}</ul>`
    ).join("");
  }
  $("tree").onclick = (e) => {
    const btn = e.target.closest("[data-sel]");
    if (!btn) return;
    select(btn.dataset.sel);
  };
}

function packageTree() {
  const pkgs = {};
  state.model.classes.forEach((c) => {
    const p = c.package || "(default)";
    pkgs[p] = pkgs[p] || [];
    pkgs[p].push(c);
  });
  return Object.keys(pkgs).sort().map((p) =>
    `<details open><summary>${p}</summary><ul class="tree">${pkgs[p].map((c) =>
      `<li><button type="button" data-sel="${c.id}">${c.name}</button></li>`
    ).join("")}</ul></details>`
  ).join("");
}

function graphForMode() {
  const m = state.model;
  if (state.mode === "architecture" || state.mode === "overview" || state.mode === "insights") {
    return archGraph();
  }
  if (state.mode === "apis") return apiGraph();
  if (state.mode === "ui") return uiGraph();
  if (state.mode === "tests") {
    const focus = testById(state.selected) || m.tests.find((t) => t.id.includes("QrOrderFlow")) || m.tests[0];
    const steps = focus ? storyOf(focus.id) : [];
    return chain(steps.slice(0, 16));
  }
  if (state.mode === "flow") {
    const steps = state.flow.length ? state.flow : m.checkoutFlow;
    return chain(steps);
  }
  if (state.mode === "folders") return folderGraph();
  if (state.mode === "dependencies") return depGraph();
  return archGraph();
}

function chain(steps) {
  return {
    nodes: steps.map((id) => node(id, short(id), kindOf(id))),
    edges: steps.slice(1).map((id, i) => ({ from: steps[i], to: id, type: "CALLS" }))
  };
}

function archGraph() {
  if (state.audience === "business") {
    return {
      nodes: [
        node("Guest", "Guest", "test"),
        node("QR menu", "QR menu", "api"),
        node("Cart", "Cart", "api"),
        node("Telr payment", "Telr payment", "ui"),
        node("Accepted order", "Accepted order", "api"),
        node("OMS kitchen", "OMS kitchen", "ext")
      ],
      edges: [
        ["Guest", "QR menu"], ["QR menu", "Cart"], ["Cart", "Telr payment"],
        ["Telr payment", "Accepted order"], ["Accepted order", "OMS kitchen"]
      ].map(([from, to]) => ({ from, to, type: "CALLS" }))
    };
  }
  const nodes = [
    node("CLI / Jenkins / GitHub", "Start command", "cfg"),
    node("TestNG XML", "Suite + groups", "cfg"),
    node("ExtentReportListener", "Report listener", "test"),
    node("Test class", "Story", "test"),
    node("QrTestHelper / CheckoutFlow", "Shared checkout", "test"),
    node("API clients", "Rest Assured", "api"),
    node("Paytm QR API", "HTTP APIs", "ext"),
    node("TelrHostedPage", "Selenium", "ui"),
    node("Telr Hosted Page", "Card page", "ext"),
    node("OmsApi", "OMS client", "api"),
    node("OMS Back Office", "Kitchen order", "ext"),
    node("Extent report", "HTML report", "cfg")
  ];
  const edges = [
    ["CLI / Jenkins / GitHub", "TestNG XML"],
    ["TestNG XML", "ExtentReportListener"],
    ["TestNG XML", "Test class"],
    ["Test class", "QrTestHelper / CheckoutFlow"],
    ["QrTestHelper / CheckoutFlow", "API clients"],
    ["API clients", "Paytm QR API"],
    ["QrTestHelper / CheckoutFlow", "TelrHostedPage"],
    ["TelrHostedPage", "Telr Hosted Page"],
    ["QrTestHelper / CheckoutFlow", "OmsApi"],
    ["OmsApi", "OMS Back Office"],
    ["ExtentReportListener", "Extent report"]
  ].map(([from, to]) => ({ from, to, type: "CALLS" }));
  return { nodes, edges };
}

function apiGraph() {
  const m = state.model;
  return {
    nodes: [
      node("Test method", "Test", "test"),
      node("API client", "Rest Assured client", "api"),
      ...m.endpoints.map((e) => node(`HTTP ${e.method} ${e.path}`, `${e.method} ${e.path}`, "ext")),
      node("Paytm QR API", "Paytm QR API", "ext"),
      node("OMS Back Office", "OMS Back Office", "ext")
    ],
    edges: [
      { from: "Test method", to: "API client", type: "CALLS" },
      ...m.endpoints.map((e) => ({
        from: "API client",
        to: `HTTP ${e.method} ${e.path}`,
        type: "CONNECTS_TO"
      })),
      ...m.endpoints.map((e) => ({
        from: `HTTP ${e.method} ${e.path}`,
        to: e.path.includes("oms") ? "OMS Back Office" : "Paytm QR API",
        type: "CONNECTS_TO"
      }))
    ]
  };
}

function uiGraph() {
  const telr = classById("ui.TelrHostedPage");
  const locNodes = (state.model.locators || []).map((l, i) =>
    node(`loc-${i}`, `By.${l.strategy}`, "ui")
  );
  return {
    nodes: [
      node("CheckoutFlow", "CheckoutFlow.payAndConfirm", "test"),
      node("ui.TelrHostedPage", "TelrHostedPage", "ui"),
      ...(telr ? telr.methods.filter((x) => x.visibility !== "public" || x.name.includes("complete") || x.name.includes("wait")).slice(0, 6).map((x) => node(x.id, x.name, "ui")) : []),
      ...locNodes.slice(0, 6),
      node("Telr Hosted Page", "Telr card page", "ext")
    ],
    edges: [
      { from: "CheckoutFlow", to: "ui.TelrHostedPage", type: "CALLS" },
      { from: "ui.TelrHostedPage", to: "Telr Hosted Page", type: "USES" },
      ...(telr ? telr.methods.slice(0, 4).map((x) => ({ from: "ui.TelrHostedPage", to: x.id, type: "CONTAINS" })) : [])
    ]
  };
}

function folderGraph() {
  const sel = classById(state.selected);
  const folder = sel ? String(sel.file).split("/").slice(0, -1).join("/") : Object.keys(state.model.folders)[0];
  const ids = state.model.folders[folder] || [];
  return {
    nodes: ids.map((id) => node(id, short(id), kindOf(id))),
    edges: state.model.relationships
      .filter((r) => r.type === "IMPORTS" && ids.includes(r.source) && ids.includes(r.target))
      .map((r) => ({ from: r.source, to: r.target, type: "IMPORTS" }))
  };
}

function depGraph() {
  const imports = state.model.relationships.filter((r) => r.type === "IMPORTS");
  const ids = new Set();
  imports.forEach((r) => { ids.add(r.source); ids.add(r.target); });
  const focus = classById(state.selected);
  let keep = [...ids];
  if (focus) {
    keep = keep.filter((id) => id === focus.id || imports.some((r) =>
      (r.source === focus.id && r.target === id) || (r.target === focus.id && r.source === id)
    ));
  }
  if (keep.length > 36) keep = keep.slice(0, 36);
  return {
    nodes: keep.map((id) => node(id, short(id), kindOf(id))),
    edges: imports.filter((r) => keep.includes(r.source) && keep.includes(r.target))
      .map((r) => ({ from: r.source, to: r.target, type: "IMPORTS" }))
  };
}

function node(id, label, kind) {
  return { id, label, kind };
}
function short(id) {
  if (!id) return "";
  if (String(id).startsWith("HTTP ")) return String(id).replace("HTTP ", "");
  const hash = String(id).split("#");
  if (hash[1]) return hash[1];
  return String(id).split(".").pop();
}
function kindOf(id) {
  const s = String(id);
  if (s.startsWith("HTTP ") || ["Paytm QR API", "OMS Back Office", "Telr Hosted Page"].includes(s)) return "ext";
  if (s.includes("Telr") || s.startsWith("ui.")) return "ui";
  if (s.startsWith("api.") || s.includes("Api")) return "api";
  if (s.startsWith("pojo.")) return "model";
  if (s.includes("tests.") || testById(s)) return "test";
  return "cfg";
}

function renderGraph() {
  const { nodes, edges } = graphForMode();
  state.graph = { nodes, edges };
  const cols = layout(nodes, edges);
  const width = 250;
  const height = 58;
  const gapX = 64;
  const gapY = 16;
  const positions = {};
  cols.forEach((col, x) => {
    col.forEach((n, y) => {
      positions[n.id] = { x: x * (width + gapX), y: y * (height + gapY) };
    });
  });
  const fill = { api: "#1c2b38", test: "#22301f", ui: "#33261e", ext: "#351f1f", cfg: "#2c291c", model: "#26243a" };
  const svg = $("graph");
  svg.innerHTML = `
    <defs>
      <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-end">
        <path d="M0 0 L10 5 L0 10 z" fill="#6c6458"></path>
      </marker>
    </defs>
    <g id="viewport"></g>
  `;
  const g = svg.querySelector("#viewport");
  edges.forEach((e) => {
    const a = positions[e.from];
    const b = positions[e.to];
    if (!a || !b) return;
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("class", `edge ${(e.type || "calls").toLowerCase()}`);
    path.setAttribute("data-from", e.from);
    path.setAttribute("data-to", e.to);
    path.setAttribute("d", `M${a.x + width} ${a.y + height / 2} C${a.x + width + 36} ${a.y + height / 2}, ${b.x - 36} ${b.y + height / 2}, ${b.x} ${b.y + height / 2}`);
    g.appendChild(path);
  });
  nodes.forEach((n) => {
    const p = positions[n.id];
    if (!p) return;
    const wrap = document.createElementNS("http://www.w3.org/2000/svg", "g");
    wrap.setAttribute("class", `node ${n.kind}${state.selected === n.id ? " sel" : ""}`);
    wrap.dataset.id = n.id;
    wrap.innerHTML = `
      <rect x="${p.x}" y="${p.y}" rx="10" width="${width}" height="${height}" style="fill:${fill[n.kind] || "var(--bg2)"}"></rect>
      <text class="kind" x="${p.x + 12}" y="${p.y + 20}">${n.kind}</text>
      <text x="${p.x + 12}" y="${p.y + 40}">${escapeXml(n.label).slice(0, 30)}</text>
    `;
    wrap.addEventListener("click", (ev) => {
      ev.stopPropagation();
      select(n.id);
    });
    g.appendChild(wrap);
  });
  applyPan();
  paintFlow();
}

function layout(nodes, edges) {
  const indeg = {};
  nodes.forEach((n) => { indeg[n.id] = 0; });
  edges.forEach((e) => {
    if (indeg[e.to] != null) indeg[e.to] += 1;
  });
  const remaining = new Set(nodes.map((n) => n.id));
  const cols = [];
  while (remaining.size) {
    let ready = [...remaining].filter((id) => indeg[id] === 0);
    if (!ready.length) ready = [[...remaining][0]];
    cols.push(ready.map((id) => nodes.find((n) => n.id === id)).filter(Boolean));
    ready.forEach((id) => {
      remaining.delete(id);
      edges.filter((e) => e.from === id && remaining.has(e.to)).forEach((e) => { indeg[e.to] -= 1; });
    });
  }
  return cols;
}

function escapeXml(s) {
  return String(s).replace(/[<>&]/g, (c) => ({ "<": "&lt;", ">": "&gt;", "&": "&amp;" }[c]));
}

function select(id) {
  state.selected = id;
  const t = testById(id);
  if (t) {
    state.flow = storyOf(t.id);
    state.flowIndex = 0;
  } else if (id === "tests.QrOrderFlowTest#shouldCreateAcceptedOrderAfterTelrPayment" || (state.model.checkoutFlow || []).includes(id)) {
    if (!state.flow.length) state.flow = state.model.checkoutFlow;
  }
  if (state.mode === "folders" && classById(id)) {
    /* keep folders mode; graph uses selected class folder */
  }
  render();
  $("detailsPane").classList.add("open");
}

function renderPanel() {
  const id = state.selected;
  $("crumb").innerHTML = breadcrumb(id);
  if (!id) {
    $("panel").innerHTML = emptyHelp();
    return;
  }
  const cls = classById(id);
  const test = testById(id);
  if (cls) {
    $("panel").innerHTML = classPanel(cls);
    return;
  }
  if (test) {
    $("panel").innerHTML = testPanel(test);
    return;
  }
  const owner = methodOwner(id);
  if (owner) {
    const meth = owner.methods.find((m) => m.id === id);
    $("panel").innerHTML = methodPanel(owner, meth);
    return;
  }
  const ep = (state.model.entryPoints || []).find((e) => e.target === id || e.id === id);
  if (ep) {
    $("panel").innerHTML = `<h3>${escapeHtml(ep.label)}</h3><p class="muted">${ep.kind}</p><p>${escapeHtml(ep.target)}</p>
      <button type="button" class="icon-btn" onclick="playCheckout()">Play checkout flow</button>`;
    return;
  }
  $("panel").innerHTML = `<h3>${escapeHtml(short(id))}</h3><p class="muted">${escapeHtml(id)}</p><p>${escapeHtml(explain(id))}</p>${impactHtml(id)}`;
}

function emptyHelp() {
  if (state.audience === "business") {
    return `<h3>Start here</h3><p>This framework checks that a diner can pay and the restaurant sees the order. Open <strong>Test Explorer</strong> and pick a test.</p>`;
  }
  if (state.audience === "architect") {
    return `<h3>Architecture</h3><p>${state.model.stats.relationships} static relationships. ${state.model.insights.length} observations. Import cycles are listed only if detected.</p>
      <p class="muted">${escapeHtml(state.model.analysis.method)}</p>`;
  }
  return `<h3>Explore</h3><p>Select a class, search with Command-K or slash, or play the checkout flow.</p>
    <p class="muted">${escapeHtml(state.model.coverageNote)}</p>
    <p>TestNG lifecycle actually found: <code>${state.model.lifecycleFound.join(", ")}</code>. No @BeforeMethod in this repo.</p>`;
}

function breadcrumb(id) {
  const parts = [{ id: null, label: "Project" }];
  if (!id) return crumbButtons(parts);
  const cls = classById(id) || methodOwner(id);
  if (cls) {
    parts.push({ id: cls.package, label: cls.package || "default" });
    parts.push({ id: cls.id, label: cls.name });
    if (String(id).includes("#")) parts.push({ id, label: String(id).split("#")[1] });
  } else {
    parts.push({ id, label: short(id) });
  }
  return crumbButtons(parts);
}

function crumbButtons(parts) {
  return parts.map((p, i) => {
    const last = i === parts.length - 1;
    if (last || !p.id) return escapeHtml(p.label);
    return `<button type="button" data-crumb="${escapeHtml(p.id)}">${escapeHtml(p.label)}</button>`;
  }).join(" → ");
}

$("crumb").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-crumb]");
  if (!btn) return;
  const id = btn.dataset.crumb;
  if (classById(id) || testById(id) || methodOwner(id)) select(id);
});

function classPanel(cls) {
  const callers = inbound(cls.id).slice(0, 16);
  const tests = state.model.tests.filter((t) =>
    t.class === cls.id || storyOf(t.id).some((s) => s === cls.id || String(s).startsWith(cls.id + "#"))
  );
  return `
    <h3>${escapeHtml(cls.name)}</h3>
    <p class="muted">${cls.kind} · ${escapeHtml(cls.file)} · ${cls.lineCount} lines · ${cls.layer}</p>
    <p>${whyHere(cls, tests)}</p>
    ${cls.extends ? `<p>Extends <code>${escapeHtml(cls.extends)}</code></p>` : ""}
    ${cls.implements.length ? `<p>Implements <code>${escapeHtml(cls.implements.join(", "))}</code></p>` : ""}
    <div>${cls.methods.filter((m) => m.isTest).map((m) => `<span class="chip">@Test ${escapeHtml(m.name)}</span>`).join("")}</div>
    <div class="kicker">Methods</div>
    ${cls.methods.slice(0, 18).map((m) =>
      `<div class="card"><strong>${escapeHtml(m.name)}()</strong>
       <div class="muted">${escapeHtml(m.returnType)} · ${m.visibility}${m.isTest ? " · test" : ""}</div>
       <button type="button" onclick="select('${m.id}')">Open</button></div>`
    ).join("")}
    <div class="kicker">What uses this?</div>
    <p>${callers.length ? callers.map((c) => `<button type="button" onclick="select('${c.source}')">${short(c.source)}</button>`).join(" · ") : "No inbound repo calls detected."}</p>
    <div class="kicker">Tests touching this type</div>
    <p>${tests.length ? tests.map((t) => `<span class="chip">${escapeHtml(t.name)}</span>`).join("") : "No static test link detected."}</p>
    ${impactHtml(cls.id)}
    <div class="kicker">Source preview</div>
    <pre>${escapeHtml(cls.sourceExcerpt)}</pre>
  `;
}

function testPanel(test) {
  const story = storyOf(test.id);
  const full = state.model.traces[test.id] || [];
  return `
    <h3>${escapeHtml(test.name)}</h3>
    <p class="muted">${escapeHtml(test.class)} · ${test.domain || "test"} · groups: ${test.groups.join(", ") || "none"}</p>
    ${test.dataProvider ? `<p>DataProvider: <code>${escapeHtml(test.dataProvider)}</code></p>` : ""}
    <p>${state.explain === "simple"
      ? "This is a TestNG test: one guest story the suite can run."
      : "Detected from usage: method annotated with @Test."}</p>
    <button type="button" class="icon-btn" onclick="traceTest('${test.id}')">Trace this test</button>
    <div class="kicker">Story path (getters and reporter calls hidden)</div>
    <ol>${story.map((s) => `<li><button type="button" onclick="select('${s}')">${escapeHtml(short(s))}</button></li>`).join("")}</ol>
    <details><summary>Full static walk (${full.length} steps)</summary>
      <ol>${full.map((s) => `<li>${escapeHtml(short(s))}</li>`).join("")}</ol>
    </details>
    <div class="kicker">HTTP in this method body</div>
    <p>${test.http.length ? test.http.map((h) => escapeHtml(h)).join("<br>") : "Endpoints are usually reached through API client methods, not the test body."}</p>
    ${impactHtml(test.id)}
  `;
}

function methodPanel(owner, meth) {
  const calls = state.model.relationships.filter((r) => r.source === meth.id && r.type === "CALLS");
  const callers = inbound(meth.id);
  return `
    <h3>${escapeHtml(meth.name)}(${meth.params.map((p) => p.name).join(", ")})</h3>
    <p class="muted">${escapeHtml(owner.id)} · ${escapeHtml(owner.file)}:${meth.line} · returns ${escapeHtml(meth.returnType)}</p>
    ${meth.groups.length ? `<p>Groups: ${meth.groups.map((g) => `<span class="chip">${escapeHtml(g)}</span>`).join("")}</p>` : ""}
    <div class="kicker">What is this?</div>
    <p>${simpleMethod(meth, owner)}</p>
    <div class="kicker">Calls (HIGH confidence)</div>
    <p>${calls.length ? calls.map((c) => `<div><button type="button" onclick="select('${c.target}')">${escapeHtml(short(c.target))}</button> <span class="chip">${c.confidence}</span></div>`).join("") : "No in-repo calls detected in the scanned window."}</p>
    <div class="kicker">Called by</div>
    <p>${callers.length ? callers.map((c) => `<div><button type="button" onclick="select('${c.source}')">${escapeHtml(short(c.source))}</button></div>`).join("") : "No callers detected."}</p>
    ${impactHtml(meth.id)}
    <div class="kicker">Source preview</div>
    <pre>${escapeHtml(meth.excerpt || owner.sourceExcerpt)}</pre>
  `;
}

function inbound(id) {
  return state.model.relationships.filter((r) => r.target === id || r.target.startsWith(id + "#"));
}

function impactHtml(id) {
  const users = inbound(id).filter((r) => ["CALLS", "USES", "IMPORTS", "TESTS"].includes(r.type));
  const tests = state.model.tests.filter((t) => storyOf(t.id).includes(id) || t.id === id);
  return `
    <div class="kicker">What breaks if this changes?</div>
    <p class="muted">Based on detected dependents only. Not a compiler impact analysis.</p>
    <p>${users.slice(0, 12).map((u) => `<span class="chip">${escapeHtml(short(u.source))}</span>`).join("") || "No dependents detected."}</p>
    <p>Tests on this story path: ${tests.length ? tests.map((t) => `<span class="chip">${escapeHtml(t.name)}</span>`).join("") : "none detected"}</p>
  `;
}

function simpleMethod(meth, owner) {
  if (state.explain === "technical") {
    return `Declared in ${owner.file}. Annotations: ${(meth.annotations || []).map((a) => "@" + a.name).join(" ") || "none"}.`;
  }
  if (meth.isTest) return "A TestNG test. It is a guest story, not production application code.";
  if (owner.name === "CheckoutFlow" && meth.name === "payAndConfirm") return "Finishes a real Telr payment, then loads OMS for the same order id. Checkout fails if OMS tokens are missing.";
  if (owner.layer === "api") return "Talks to a Paytm or OMS HTTP API using Rest Assured.";
  if (owner.usesSelenium) return "Drives the Telr card page in Chrome.";
  if (owner.layer === "config") return "Reads environment or testdata. Secret values are not shown here.";
  if (owner.layer === "reporting") return "Writes the business report. Tokens and cards are masked in logs.";
  return `Inferred from naming and package (${owner.layer}). Confirm in source.`;
}

function whyHere(cls, tests) {
  const detected = (text) => state.explain === "simple" ? text : `Detected from usage. ${text}`;
  if (cls.isListener) return detected("TestNG listener that writes the Extent / console report.");
  if (cls.isTestClass) return detected(`Contains ${cls.methods.filter((m) => m.isTest).length} @Test methods.`);
  if (cls.isApiClient) return detected("Rest Assured client. Tests call this instead of building HTTP by hand.");
  if (cls.usesSelenium) return detected("Selenium page for Telr. Only UI in this repo.");
  if (cls.isPojo) return "Inferred from package: request/response shape. Fields come from live API payloads.";
  if (cls.layer === "config") return detected("Loads properties. Credentials stay in env or gitignored files.");
  if (tests.length) return detected(`Referenced by ${tests.length} test(s).`);
  return "Inferred from naming and folder. No test annotation on this type.";
}

function explain(id) {
  if (String(id).startsWith("HTTP ")) return "HTTP path detected from Rest Assured .post/.get string. Path variables were collapsed.";
  const ext = state.model.external.find((x) => x.id === id);
  return ext ? ext.note : "Node from the architecture map or configuration.";
}

function escapeHtml(s) {
  return String(s || "").replace(/[&<>]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" }[c]));
}

function paintExplain() {
  $("simpleBtn").classList.toggle("active", state.explain === "simple");
  $("techBtn").classList.toggle("active", state.explain === "technical");
}

function renderInsights() {
  const cards = state.model.insights.map((i) =>
    `<div class="card"><strong>${escapeHtml(i.title)}</strong><p class="muted">${escapeHtml(i.detail)}</p>
     ${i.node ? `<button type="button" onclick="select('${i.node}')">Open</button>` : ""}</div>`
  ).join("");
  $("panel").innerHTML = `
    <h3>Architecture observations</h3>
    <p class="muted">Heuristic only. ${escapeHtml(state.model.coverageNote)}</p>
    ${cards}
    <div class="card"><strong>TestNG lifecycle actually found</strong>
      <p>${state.model.lifecycleFound.join(", ") || "none"}</p>
      <p class="muted">No @BeforeMethod / @AfterMethod in this repo. Browser setup lives inside TelrHostedPage, not a BaseTest.</p></div>
    <div class="card"><strong>Retry</strong><p>No IRetryAnalyzer detected.</p></div>
    <div class="card"><strong>Analysis limits</strong><ul>${state.model.analysis.knownLimits.map((l) => `<li>${escapeHtml(l)}</li>`).join("")}</ul></div>
    ${configCards()}
  `;
}

function configCards() {
  return Object.entries(state.model.configs).map(([file, rows]) =>
    `<div class="card"><strong>${escapeHtml(file)}</strong>
     ${rows.map((r) => `<div><code>${escapeHtml(r.key)}</code> = ${escapeHtml(r.value)}${r.masked ? ' <span class="chip warn">masked</span>' : ""}</div>`).join("")}
    </div>`
  ).join("");
}

window.select = select;
window.traceTest = function (id) {
  const t = testById(id);
  if (!t) return;
  state.mode = "flow";
  state.flow = storyOf(id);
  state.flowIndex = 0;
  state.selected = id;
  renderChrome();
  render();
  paintFlow();
};
window.playCheckout = function () {
  state.mode = "flow";
  state.flow = state.model.checkoutFlow;
  state.flowIndex = 0;
  state.selected = state.flow[0];
  renderChrome();
  render();
  paintFlow();
};

function openPalette(q) {
  $("overlay").hidden = false;
  $("overlay").classList.add("open");
  $("paletteInput").value = q;
  $("paletteInput").focus();
  drawHits(q);
}
function closePalette() {
  $("overlay").classList.remove("open");
  $("overlay").hidden = true;
}

function drawHits(q) {
  const query = (q || "").toLowerCase();
  const hits = [];
  state.model.classes.forEach((c) => {
    if (!query || c.name.toLowerCase().includes(query) || c.id.toLowerCase().includes(query) || c.package.toLowerCase().includes(query)) {
      hits.push({ id: c.id, label: c.id, kind: "class" });
    }
    c.methods.forEach((m) => {
      if (query && m.name.toLowerCase().includes(query)) hits.push({ id: m.id, label: m.id, kind: m.isTest ? "test" : "method" });
    });
  });
  state.model.tests.forEach((t) => {
    if (!query || t.name.toLowerCase().includes(query) || t.groups.some((g) => g.includes(query))) {
      hits.push({ id: t.id, label: t.id, kind: "test" });
    }
  });
  state.model.endpoints.forEach((e) => {
    if (!query || e.path.toLowerCase().includes(query) || e.method.toLowerCase().includes(query)) {
      hits.push({ id: e.class, label: `${e.method} ${e.path}`, kind: "api" });
    }
  });
  (state.model.locators || []).forEach((l) => {
    if (query && l.value.toLowerCase().includes(query)) {
      hits.push({ id: l.class, label: `By.${l.strategy}(${l.value})`, kind: "locator" });
    }
  });
  $("hits").innerHTML = hits.slice(0, 32).map((h) =>
    `<div class="hit" data-id="${h.id}"><strong>${escapeHtml(h.label)}</strong><div class="muted">${h.kind}</div></div>`
  ).join("") || `<div class="hit">No matches</div>`;
  $("hits").onclick = (e) => {
    const row = e.target.closest("[data-id]");
    if (!row) return;
    closePalette();
    select(row.dataset.id);
  };
}

function ensureFlow() {
  if (!state.flow.length) {
    state.flow = state.model.checkoutFlow;
    state.mode = "flow";
    renderChrome();
    render();
  }
}

function togglePlay() {
  ensureFlow();
  if (state.flowIndex < 0) state.flowIndex = 0;
  state.playing = !state.playing;
  $("play").textContent = state.playing ? "Pause" : "Play";
  if (state.playing) state.timer = setInterval(() => step(1), 1100);
  else if (state.timer) clearInterval(state.timer);
}

function stopPlay() {
  state.playing = false;
  $("play").textContent = "Play";
  if (state.timer) clearInterval(state.timer);
}

function step(dir) {
  ensureFlow();
  const next = (state.flowIndex < 0 ? 0 : state.flowIndex) + dir;
  state.flowIndex = Math.max(0, Math.min(state.flow.length - 1, next));
  if (state.flowIndex === state.flow.length - 1) stopPlay();
  paintFlow();
}

function paintFlow() {
  if (!state.flow.length || state.flowIndex < 0) {
    $("stepLabel").textContent = "Flow idle";
    highlightSelection();
    return;
  }
  const current = state.flow[state.flowIndex];
  $("stepLabel").textContent = `Step ${state.flowIndex + 1} / ${state.flow.length} · ${short(current)}`;
  document.querySelectorAll(".node").forEach((n) => {
    const on = n.dataset.id === current;
    const near = neighbor(current, n.dataset.id);
    n.classList.toggle("sel", on || n.dataset.id === state.selected);
    n.classList.toggle("dim", state.hideUnrelated ? !on && !near : (!on && !near && n.dataset.id !== state.selected));
  });
  document.querySelectorAll(".edge").forEach((e) => {
    e.classList.toggle("active", e.dataset.from === current || e.dataset.to === current);
  });
}

function highlightSelection() {
  const id = state.selected;
  document.querySelectorAll(".node").forEach((n) => {
    const near = id && neighborGraph(id, n.dataset.id);
    n.classList.toggle("sel", n.dataset.id === id);
    n.classList.toggle("dim", state.hideUnrelated && id && n.dataset.id !== id && !near);
  });
}

function neighbor(id, other) {
  return state.flow.includes(other) && Math.abs(state.flow.indexOf(other) - state.flow.indexOf(id)) === 1;
}
function neighborGraph(id, other) {
  return state.graph.edges.some((e) => (e.from === id && e.to === other) || (e.to === id && e.from === other));
}

function exportJson() {
  const blob = new Blob([JSON.stringify({ graph: state.graph, selected: state.selected, flow: state.flow }, null, 2)], { type: "application/json" });
  download(blob, "explorer-view.json");
}
function exportMermaid() {
  const lines = ["flowchart LR"];
  state.graph.edges.forEach((e) => {
    lines.push(`  ${safeId(e.from)}["${short(e.from)}"] --> ${safeId(e.to)}["${short(e.to)}"]`);
  });
  download(new Blob([lines.join("\n")], { type: "text/plain" }), "explorer-view.mmd");
}
function safeId(id) {
  return String(id).replace(/[^A-Za-z0-9]/g, "_").slice(0, 40);
}
function download(blob, name) {
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = name;
  a.click();
  URL.revokeObjectURL(a.href);
}

boot().catch((err) => {
  document.body.innerHTML = `<p style="padding:24px">Could not load model.json. From this folder run <code>python3 -m http.server 8765</code> and open http://localhost:8765 — do not open the HTML file directly.<br>${escapeHtml(err.message || err)}</p>`;
});
