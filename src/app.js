import { createInitialState, EXECUTION_STATUS, POSTURES, reduce, selectedStep } from "./flow-engine.js";

const app = document.querySelector("#app");
let state = createInitialState();
let timer = null;

const icons = {
  location: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21s7-6.1 7-12a7 7 0 1 0-14 0c0 5.9 7 12 7 12Z"/><circle cx="12" cy="9" r="2.2"/></svg>',
  layout: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="4" width="17" height="16" rx="3"/><path d="M12 4v16M12 9h8.5"/></svg>',
  calendar: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="5" width="17" height="15" rx="3"/><path d="M7 3.5v4M17 3.5v4M3.5 9h17M7 13h.1M11.9 13h.1M16.8 13h.1M7 16.5h.1M11.9 16.5h.1"/></svg>',
  focus: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="7"/><path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3"/></svg>',
  bell: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 17h12l-1.5-2.2V10a4.5 4.5 0 0 0-9 0v4.8L6 17ZM10 20h4"/></svg>',
  shield: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m12 3 7 3v5c0 4.8-3 8.3-7 10-4-1.7-7-5.2-7-10V6l7-3Z"/><path d="m8.5 12 2.2 2.2 4.8-5"/></svg>',
  play: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 6 9 6-9 6V6Z"/></svg>',
  pause: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 6v12M16 6v12"/></svg>',
  undo: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 7 4 12l5 5M5 12h8a6 6 0 0 1 6 6"/></svg>',
};

function icon(name) { return icons[name] ?? icons.layout; }

function postureLabel(posture) {
  return {
    [POSTURES.COVER]: "Cover screen",
    [POSTURES.OPEN]: "Inner screen",
    [POSTURES.FLEX]: "Flex mode",
    [POSTURES.DEX]: "DeX display",
  }[posture];
}

function statusLabel(execution) {
  if (execution.status === EXECUTION_STATUS.EVALUATING) return "Checking conditions";
  if (execution.status === EXECUTION_STATUS.RUNNING) return `Running step ${execution.currentStepIndex + 1} of ${state.flow.steps.length}`;
  if (execution.status === EXECUTION_STATUS.PAUSED) return "Paused";
  if (execution.status === EXECUTION_STATUS.COMPLETED) return execution.mode === "preview" ? "Preview complete" : "Workspace active";
  if (execution.status === EXECUTION_STATUS.FAILED) return "Needs attention";
  return "Ready to preview";
}

function render() {
  const step = selectedStep(state);
  const execution = state.execution;
  const activeStep = state.flow.steps[execution.currentStepIndex];
  const isLive = execution.mode === "live-simulation";
  const hasRun = execution.status !== EXECUTION_STATUS.IDLE;

  app.innerHTML = `
    <div class="app-shell">
      <header class="topbar">
        <div class="brand"><span class="brand-mark"><i></i></span><span>Galaxy Flow<small>Make your moment</small></span></div>
        <div class="topbar-right"><span class="sync-dot"></span><span>Local prototype</span><span class="topbar-divider"></span><span>Help</span><span class="avatar">GF</span></div>
      </header>
      <div class="app-layout">
        <aside class="rail">
          <div class="rail-label">Workspace</div>
          <button class="rail-item" type="button">${icon("calendar")}<span>Today</span></button>
          <button class="rail-item active" type="button">${icon("focus")}<span>Flows</span><b>1</b></button>
          <button class="rail-item" type="button">${icon("bell")}<span>Suggestions</span></button>
          <div class="rail-space"></div>
          <div class="rail-label">Connected</div>
          <div class="device-list"><div class="device-list-title">Your Galaxy</div><div class="device-row"><i></i><span>Galaxy Z Fold</span><small>ready</small></div><div class="device-row"><i></i><span>Galaxy Watch</span><small>ready</small></div><div class="device-row"><i></i><span>DeX display</span><small>offline</small></div></div>
          <div class="rail-foot"><span class="mini-shield">${icon("shield")}</span><span>Trust center</span></div>
        </aside>

        <main class="composer">
          <div class="breadcrumb">Flows <span>/</span> Work <span>/</span> Start Work</div>
          <div class="hero-row"><div><div class="eyebrow"><i></i> Adaptive workspace</div><h1>Start <em>Work</em></h1><p class="hero-copy">Arrive at work. Your Fold sets up the day.</p></div><div class="active-badge"><i></i><span><b>Active</b><small>Runs automatically</small></span></div></div>

          <section class="context-card"><div class="context-icon">${icon("location")}</div><div><span class="context-label">This Flow starts when</span><strong>you arrive at the office</strong><small>Weekdays · 8:00–10:00 AM · Simulated Wi-Fi trigger · Layout preview</small></div><button class="more-button" type="button" aria-label="Edit trigger">•••</button></section>

          <div class="section-heading"><div><span class="section-kicker">Flow rail</span><h2>Then do these things</h2></div><span class="step-count">${state.flow.steps.length - 1} actions</span></div>
          <section class="flow-rail" aria-label="Flow steps">
            ${state.flow.steps.map((flowStep, index) => {
              const completed = execution.completedStepIds.includes(flowStep.id);
              const current = activeStep?.id === flowStep.id;
              const selected = step.id === flowStep.id;
              return `<button class="flow-step ${completed ? "complete" : ""} ${current ? "current" : ""} ${selected ? "selected" : ""}" type="button" data-step="${flowStep.id}"><span class="step-marker">${completed ? "✓" : index + 1}</span><span class="step-icon">${icon(flowStep.icon)}</span><span class="step-copy"><span class="step-group">${flowStep.group}</span><strong>${flowStep.title}</strong><small>${flowStep.detail}</small></span>${flowStep.requiresApproval ? '<span class="approval-badge">Ask first</span>' : ""}<span class="step-chevron">›</span></button>`;
            }).join("")}
          </section>

          <section class="detail-panel"><div class="detail-icon">${icon(step.icon)}</div><div class="detail-copy"><span>${step.group} · ${step.risk} risk</span><strong>${step.title}</strong><p>${step.detail}</p></div><button class="edit-action" type="button">Edit</button></section>

          <section class="trust-drawer"><div class="trust-summary"><span class="trust-icon">${icon("shield")}</span><span><strong>Rehearsal boundaries</strong><small>Illustrative workflow · no services or settings connected</small></span><button id="trust-toggle" class="trust-toggle" type="button">${state.trustOpen ? "Hide details" : "View details"}</button></div>${state.trustOpen ? '<div class="trust-details"><div><b>Can use</b><span>Mock Wi-Fi · Mock Calendar · Mock Tasks · Mock Focus</span></div><div><b>Will always ask</b><span>Messages · File changes · Sharing · Purchases</span></div></div>' : ""}</section>

          <div class="composer-actions"><button id="preview-button" class="primary-button" type="button">${icon("play")} Preview workspace</button><button id="run-button" class="secondary-button" type="button">Rehearse flow</button><span class="execution-status">${statusLabel(execution)}</span></div>
          ${execution.error ? `<div class="error-banner"><span>${icon("bell")}</span><div><strong>Flow paused safely</strong><small>${execution.error}</small></div><button id="dismiss-error" type="button">Dismiss</button></div>` : ""}
        </main>

        <section class="stage" aria-label="Fold stage preview">
          <div class="stage-header"><div><span class="stage-kicker">Stage preview</span><h2>${postureLabel(state.posture)}</h2><small>${state.posture === POSTURES.OPEN ? "Open 178° · 2-pane workspace" : state.posture === POSTURES.FLEX ? "90° posture · lower controls" : state.posture === POSTURES.COVER ? "Closed · glanceable state" : "External display · desktop layout"}</small></div><span class="preview-pill">${isLive ? "Live simulation" : "Preview only"}</span></div>
          <div class="posture-tabs">${Object.values(POSTURES).map((posture) => `<button class="posture-tab ${state.posture === posture ? "active" : ""}" type="button" data-posture="${posture}">${posture === POSTURES.COVER ? "Cover" : posture === POSTURES.OPEN ? "Open" : posture === POSTURES.FLEX ? "Flex" : "DeX"}</button>`).join("")}</div>
          <div class="device-stage"><div class="stage-glow"></div><div class="fold-device ${state.posture}" id="fold-device"><div class="device-screen left-screen"><div class="screen-status"><b>8:42</b><span>▰ ◔ 92%</span></div><div class="screen-content"><span class="screen-eyebrow">Good morning</span><strong>Your day,<br>in focus.</strong><small>Sample day · Mock office · Focus preview</small><div class="screen-card"><b>Next up</b><strong>Client planning</strong><small>10:00 AM · 45 min</small></div><div class="screen-card green"><b>Priority</b><strong>Finish launch brief</strong></div></div></div><div class="device-hinge"></div><div class="device-screen right-screen"><div class="screen-status"><b>Notes</b><span>•••</span></div><div class="screen-content"><span class="screen-eyebrow">Continue here</span><strong>Launch<br>brief.</strong><small>Last edited yesterday · 4:18 PM</small><div class="screen-card purple"><b>Workspace link</b><strong>Calendar + Notes mockup</strong><small>Illustrative preview</small></div><div class="screen-card"><b>Later</b><strong>Status reminder</strong><small>Today · 4:00 PM</small></div></div><div class="screen-dock"><span>Work</span><b>${execution.status === EXECUTION_STATUS.COMPLETED && isLive ? "Active" : "Ready"}</b><span>⌕</span></div></div><div class="cover-preview"><small>Cover · before</small><div><b>8:42</b><span>Work workspace ready</span></div></div></div></div>
          <div class="stage-result"><span class="result-check">✓</span><div><strong>${hasRun && execution.status === EXECUTION_STATUS.COMPLETED ? isLive ? "Simulated stage is active" : "Rehearsal complete" : "Illustrative workspace includes"}</strong><small>Calendar + Notes · Priorities visible · Work focus ${hasRun && execution.status === EXECUTION_STATUS.COMPLETED ? "enabled" : "prepared"}</small></div></div>
          ${execution.status === EXECUTION_STATUS.COMPLETED && isLive ? `<div class="capsule"><div class="capsule-top"><span class="capsule-live"><i></i> Rehearsal Scene Capsule</span><span>Started just now</span></div><strong>Start Work</strong><small>Calendar · Notes · Work focus</small><div class="capsule-actions"><button id="pause-button" type="button">${icon("pause")} Pause</button><button id="undo-button" type="button">${icon("undo")} Undo</button></div></div>` : ""}
          <div class="stage-footer"><span><i></i> ${isLive ? "Simulation only · no device settings changed" : "Preview only · nothing changes on your device"}</span></div>
        </section>
      </div>
    </div>`;

  app.querySelectorAll("[data-posture]").forEach((button) => button.addEventListener("click", () => dispatch({ type: "SELECT_POSTURE", posture: button.dataset.posture })));
  app.querySelectorAll("[data-step]").forEach((button) => button.addEventListener("click", () => dispatch({ type: "SELECT_STEP", stepId: button.dataset.step })));
  app.querySelector("#trust-toggle")?.addEventListener("click", () => { state = { ...state, trustOpen: !state.trustOpen }; render(); });
  app.querySelector("#preview-button")?.addEventListener("click", () => startExecution("preview"));
  app.querySelector("#run-button")?.addEventListener("click", () => startExecution("live"));
  app.querySelector("#pause-button")?.addEventListener("click", () => { dispatch({ type: "PAUSE" }); clearInterval(timer); });
  app.querySelector("#undo-button")?.addEventListener("click", () => dispatch({ type: "UNDO" }));
  app.querySelector("#dismiss-error")?.addEventListener("click", () => dispatch({ type: "DISMISS_ERROR" }));
}

function dispatch(event) {
  state = reduce(state, event);
  render();
}

function startExecution(mode) {
  clearInterval(timer);
  dispatch({ type: mode === "live" ? "START_LIVE" : "START_PREVIEW" });
  let ticks = 0;
  timer = window.setInterval(() => {
    if (state.execution.status === EXECUTION_STATUS.PAUSED) return;
    if (ticks === 0) dispatch({ type: "ADVANCE" });
    else dispatch({ type: "ADVANCE" });
    ticks += 1;
    if (state.execution.status === EXECUTION_STATUS.COMPLETED || state.execution.status === EXECUTION_STATUS.FAILED) clearInterval(timer);
  }, 680);
}

render();
