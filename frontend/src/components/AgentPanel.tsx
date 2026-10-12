import { useState } from "react";
import { useOperationsAgent } from "@/features/agents/hooks";

const presets = [
  "Give me a summary of the current delivery operation.",
  "Are there any operational issues that need attention right now?",
  "What are the current completion, cancellation, and delay rates?",
];

export function AgentPanel() {
  const [message, setMessage] = useState("");

  const agent = useOperationsAgent();

  function submit(value: string) {
    const trimmed = value.trim();

    if (!trimmed) {
      return;
    }

    agent.mutate(trimmed);
  }

  return (
    <div className="rounded-xl border border-zinc-800 bg-zinc-900">
      <div className="border-b border-zinc-800 px-5 py-4">
        <h2 className="font-semibold text-zinc-100">
          Operations assistant
        </h2>

        <p className="mt-1 text-sm text-zinc-500">
          Queries live operational data through the agent
          service.
        </p>
      </div>

      <div className="space-y-4 p-5">
        <div className="flex flex-wrap gap-2">
          {presets.map((preset) => (
            <button
              key={preset}
              type="button"
              disabled={agent.isPending}
              onClick={() => {
                setMessage(preset);
                submit(preset);
              }}
              className="
                rounded-lg border border-zinc-700
                bg-zinc-800 px-3 py-2
                text-xs text-zinc-300
                transition hover:bg-zinc-700
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              {preset}
            </button>
          ))}
        </div>

        <form
          className="flex gap-2"
          onSubmit={(event) => {
            event.preventDefault();
            submit(message);
          }}
        >
          <input
            value={message}
            onChange={(event) =>
              setMessage(event.target.value)
            }
            placeholder="Ask about current operations..."
            className="
              min-w-0 flex-1 rounded-lg
              border border-zinc-700
              bg-zinc-950 px-4 py-2.5
              text-sm text-zinc-100
              outline-none
              placeholder:text-zinc-600
              focus:border-zinc-500
            "
          />

          <button
            type="submit"
            disabled={
              agent.isPending ||
              message.trim().length === 0
            }
            className="
              rounded-lg bg-zinc-100
              px-4 py-2.5
              text-sm font-medium text-zinc-950
              transition hover:bg-white
              disabled:cursor-not-allowed
              disabled:opacity-50
            "
          >
            {agent.isPending ? "Thinking..." : "Ask"}
          </button>
        </form>

        {agent.isError && (
          <div className="rounded-lg border border-red-900 bg-red-950/40 p-4 text-sm text-red-300">
            Failed to query the operations agent.
          </div>
        )}

        {agent.data && (
          <div className="rounded-lg border border-zinc-800 bg-zinc-950 p-4">
            <p className="text-xs font-medium uppercase tracking-wide text-zinc-500">
              Response
            </p>

            <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-zinc-300">
              {agent.data.response}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
