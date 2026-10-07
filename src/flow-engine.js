export const POSTURES = Object.freeze({
  COVER: "cover",
  OPEN: "open",
  FLEX: "flex",
  DEX: "dex",
});

export const EXECUTION_STATUS = Object.freeze({
  IDLE: "idle",
  EVALUATING: "evaluating",
  RUNNING: "running",
  PAUSED: "paused",
  COMPLETED: "completed",
  FAILED: "failed",
});

const initialSteps = [
  {
    id: "trigger",
    group: "Trigger",
    title: "Arrive at the office",
    detail: "Weekday + Work Wi-Fi + Fold opened between 8:00 and 10:00 AM",
    icon: "location",
    risk: "low",
  },
  {
    id: "workspace",
    group: "Workspace",
    title: "Open the Work stage",
    detail: "Arrange Calendar and Notes across the inner display",
    icon: "layout",
    risk: "low",
  },
  {
    id: "priorities",
    group: "Context",
    title: "Show today’s priorities",
    detail: "Surface the next meeting, due tasks, and the last edited brief",
    icon: "calendar",
    risk: "medium",
  },
  {
    id: "focus",
    group: "Device",
    title: "Enable Work focus",
    detail: "Allow starred contacts and silence nonessential alerts",
    icon: "focus",
    risk: "medium",
  },
  {
    id: "reminder",
    group: "Approval",
    title: "Prepare a status reminder",
    detail: "Create a local reminder for 4:00 PM; never send automatically",
    icon: "bell",
    risk: "high",
    requiresApproval: true,
  },
];

export function createInitialState() {
  return {
    flow: {
      id: "start-work",
      name: "Start Work",
      moment: "Work",
      enabled: true,
      steps: initialSteps.map((step) => ({ ...step, state: "pending" })),
    },
    posture: POSTURES.OPEN,
    selectedStepId: "workspace",
    execution: {
      mode: "preview",
      status: EXECUTION_STATUS.IDLE,
      currentStepIndex: -1,
      completedStepIds: [],
      skippedStepIds: [],
      error: null,
      capsule: null,
      startedAt: null,
    },
  };
}

function withStepStates(state, completedStepIds) {
  const completed = new Set(completedStepIds);
  return {
    ...state,
    flow: {
      ...state.flow,
      steps: state.flow.steps.map((step) => ({
        ...step,
        state: completed.has(step.id) ? "complete" : "pending",
      })),
    },
  };
}

function resetExecution(state) {
  return withStepStates(
    {
      ...state,
      execution: {
        mode: "preview",
        status: EXECUTION_STATUS.IDLE,
        currentStepIndex: -1,
        completedStepIds: [],
        skippedStepIds: [],
        error: null,
        capsule: null,
        startedAt: null,
      },
    },
    [],
  );
}

export function reduce(state, event) {
  switch (event.type) {
    case "SELECT_POSTURE":
      return { ...state, posture: event.posture };
    case "SELECT_STEP":
      return { ...state, selectedStepId: event.stepId };
    case "START_PREVIEW":
    case "START_LIVE":
      return withStepStates(
        {
          ...state,
          execution: {
            mode: event.type === "START_LIVE" ? "live-simulation" : "preview",
            status: EXECUTION_STATUS.EVALUATING,
            currentStepIndex: -1,
            completedStepIds: [],
            skippedStepIds: [],
            error: null,
            capsule: null,
            startedAt: Date.now(),
          },
        },
        [],
      );
    case "ADVANCE": {
      if (![EXECUTION_STATUS.EVALUATING, EXECUTION_STATUS.RUNNING].includes(state.execution.status)) return state;
      const nextIndex = state.execution.currentStepIndex + 1;
      const nextStep = state.flow.steps[nextIndex];
      if (!nextStep) {
        const isLive = state.execution.mode === "live-simulation";
        return withStepStates(
          {
            ...state,
            execution: {
              ...state.execution,
              status: EXECUTION_STATUS.COMPLETED,
              currentStepIndex: state.flow.steps.length - 1,
              capsule: isLive
                ? { name: state.flow.name, status: "active", startedAt: state.execution.startedAt, posture: state.posture }
                : null,
            },
          },
          state.flow.steps.map((step) => step.id),
        );
      }
      const completedStepIds = [...state.execution.completedStepIds, nextStep.id];
      return withStepStates(
        {
          ...state,
          execution: {
            ...state.execution,
            status: EXECUTION_STATUS.RUNNING,
            currentStepIndex: nextIndex,
            completedStepIds,
          },
        },
        completedStepIds,
      );
    }
    case "PAUSE":
      return state.execution.status === EXECUTION_STATUS.RUNNING
        ? { ...state, execution: { ...state.execution, status: EXECUTION_STATUS.PAUSED, capsule: state.execution.capsule ? { ...state.execution.capsule, status: "paused" } : null } }
        : state;
    case "RESUME":
      return state.execution.status === EXECUTION_STATUS.PAUSED
        ? { ...state, execution: { ...state.execution, status: EXECUTION_STATUS.RUNNING, capsule: state.execution.capsule ? { ...state.execution.capsule, status: "active" } : null } }
        : state;
    case "FAIL":
      return { ...state, execution: { ...state.execution, status: EXECUTION_STATUS.FAILED, error: event.message ?? "The action could not be completed." } };
    case "UNDO":
      return resetExecution(state);
    case "DISMISS_ERROR":
      return { ...state, execution: { ...state.execution, status: EXECUTION_STATUS.IDLE, error: null } };
    default:
      return state;
  }
}

export function selectedStep(state) {
  return state.flow.steps.find((step) => step.id === state.selectedStepId) ?? state.flow.steps[0];
}
