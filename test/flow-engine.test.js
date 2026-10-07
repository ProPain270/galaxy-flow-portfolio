import test from "node:test";
import assert from "node:assert/strict";
import { createInitialState, EXECUTION_STATUS, POSTURES, reduce } from "../src/flow-engine.js";

function finish(state) {
  let next = reduce(state, { type: "ADVANCE" });
  while (next.execution.status !== EXECUTION_STATUS.COMPLETED) next = reduce(next, { type: "ADVANCE" });
  return next;
}

test("creates an inspectable Start Work flow", () => {
  const state = createInitialState();
  assert.equal(state.flow.name, "Start Work");
  assert.equal(state.posture, POSTURES.OPEN);
  assert.equal(state.flow.steps.length, 5);
  assert.equal(state.flow.steps.at(-1).requiresApproval, true);
});

test("selects a Fold posture without changing execution state", () => {
  const state = createInitialState();
  const next = reduce(state, { type: "SELECT_POSTURE", posture: POSTURES.FLEX });
  assert.equal(next.posture, POSTURES.FLEX);
  assert.equal(next.execution.status, EXECUTION_STATUS.IDLE);
});

test("preview completes without creating an active capsule", () => {
  const preview = finish(reduce(createInitialState(), { type: "START_PREVIEW" }));
  assert.equal(preview.execution.status, EXECUTION_STATUS.COMPLETED);
  assert.equal(preview.execution.mode, "preview");
  assert.equal(preview.execution.capsule, null);
  assert.equal(preview.flow.steps.every((step) => step.state === "complete"), true);
});

test("live simulation creates a resumable active capsule", () => {
  const live = finish(reduce(createInitialState(), { type: "START_LIVE" }));
  assert.equal(live.execution.status, EXECUTION_STATUS.COMPLETED);
  assert.equal(live.execution.capsule.status, "active");
  const paused = reduce(live, { type: "PAUSE" });
  assert.equal(paused.execution.status, EXECUTION_STATUS.COMPLETED);
});

test("pause and resume work while a live Flow is running", () => {
  let state = reduce(createInitialState(), { type: "START_LIVE" });
  state = reduce(state, { type: "ADVANCE" });
  state = reduce(state, { type: "PAUSE" });
  assert.equal(state.execution.status, EXECUTION_STATUS.PAUSED);
  state = reduce(state, { type: "RESUME" });
  assert.equal(state.execution.status, EXECUTION_STATUS.RUNNING);
});

test("undo restores a clean, non-executed state", () => {
  const live = finish(reduce(createInitialState(), { type: "START_LIVE" }));
  const undone = reduce(live, { type: "UNDO" });
  assert.equal(undone.execution.status, EXECUTION_STATUS.IDLE);
  assert.equal(undone.execution.capsule, null);
  assert.equal(undone.execution.completedStepIds.length, 0);
  assert.equal(undone.flow.steps.every((step) => step.state === "pending"), true);
});

test("failure is explicit and safely dismissible", () => {
  let state = reduce(createInitialState(), { type: "START_PREVIEW" });
  state = reduce(state, { type: "FAIL", message: "Notes cannot open in split view." });
  assert.equal(state.execution.status, EXECUTION_STATUS.FAILED);
  assert.equal(state.execution.error, "Notes cannot open in split view.");
  state = reduce(state, { type: "DISMISS_ERROR" });
  assert.equal(state.execution.status, EXECUTION_STATUS.IDLE);
  assert.equal(state.execution.error, null);
});
