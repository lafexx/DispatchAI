import { useEffect, useState } from "react";
import { useOperationsAgent } from "@/features/agents/hooks";
import { Icon, type IconName } from "./Icon";
import { Panel, Notice } from "./UI";
const prompts: { title: string; message: string; icon: IconName }[] = [
  {
    title: "Operational briefing",
    message: "Give me a summary of the current delivery operation.",
    icon: "overview",
  },
  {
    title: "Find the bottlenecks",
    message: "Are there any operational issues that need attention right now?",
    icon: "alert",
  },
  {
    title: "Understand performance",
    message: "What are the current completion, cancellation, and delay rates?",
    icon: "chart",
  },
];
interface Entry {
  id: string;
  question: string;
  answer?: string;
  error?: string;
}
function storedConversation(): Entry[] {
  try {
    const value = JSON.parse(
      sessionStorage.getItem("dispatchai-conversation") ?? "[]",
    );
    return Array.isArray(value)
      ? value
          .filter(
            (v: Entry) =>
              v &&
              typeof v.id === "string" &&
              typeof v.question === "string" &&
              (v.answer === undefined || typeof v.answer === "string") &&
              (v.error === undefined || typeof v.error === "string"),
          )
          .slice(-20)
      : [];
  } catch {
    return [];
  }
}
export function AgentPanel() {
  const [message, setMessage] = useState("");
  const [entries, setEntries] = useState<Entry[]>(storedConversation);
  const agent = useOperationsAgent();
  useEffect(() => {
    try {
      sessionStorage.setItem(
        "dispatchai-conversation",
        JSON.stringify(entries.filter((e) => e.answer || e.error).slice(-20)),
      );
    } catch {
      /* A blocked storage setting should not prevent a query. */
    }
  }, [entries]);
  function submit(value: string) {
    const trimmed = value.trim();
    if (!trimmed || trimmed.length > 4000 || agent.isPending) return;
    const id = crypto.randomUUID();
    setEntries((prev) => [...prev.slice(-19), { id, question: trimmed }]);
    setMessage("");
    agent.mutate(trimmed, {
      onSuccess: (data) =>
        setEntries((prev) =>
          prev.map((e) => (e.id === id ? { ...e, answer: data.response } : e)),
        ),
      onError: (error) =>
        setEntries((prev) =>
          prev.map((e) => (e.id === id ? { ...e, error: error.message } : e)),
        ),
    });
  }
  return (
    <Panel>
      <div className="assistant-welcome">
        <div className="flex justify-between">
          <div className="assistant-symbol">
            <Icon name="spark" size={25} />
          </div>
          {entries.length > 0 && (
            <button
              className="button button-small button-ghost self-start"
              disabled={agent.isPending}
              onClick={() => setEntries([])}
            >
              Clear conversation
            </button>
          )}
        </div>
        <h2>Your operations, in plain language.</h2>
        <p>
          Ask about delivery performance, resource availability, or the work
          that needs attention. The assistant looks up operational data to
          answer your question.
        </p>
      </div>
      <div className="prompt-grid">
        {prompts.map((p) => (
          <button
            key={p.title}
            className="prompt-card"
            disabled={agent.isPending}
            onClick={() => submit(p.message)}
          >
            <Icon name={p.icon} />
            <strong>{p.title}</strong>
            <span className="cell-secondary">{p.message}</span>
          </button>
        ))}
      </div>
      <div
        className="conversation"
        aria-live="polite"
        aria-busy={agent.isPending}
      >
        {entries.map((e) => (
          <div key={e.id} className="flex flex-col gap-3">
            <div className="chat-question">{e.question}</div>
            {e.error ? (
              <Notice tone="error">{e.error}</Notice>
            ) : e.answer ? (
              <div className="chat-answer">
                <div className="flex items-center gap-2 text-purple mb-3">
                  <Icon name="spark" size={14} />
                  <strong className="text-xs">DispatchAI assistant</strong>
                </div>
                {e.answer}
              </div>
            ) : (
              <div className="chat-answer muted flex items-center gap-3">
                <span className="spinner" />
                Checking operational data…
              </div>
            )}
          </div>
        ))}
      </div>
      <form
        className="assistant-form"
        onSubmit={(e) => {
          e.preventDefault();
          submit(message);
        }}
      >
        <textarea
          aria-label="Ask the operations assistant"
          placeholder="What would you like to know about your operation?"
          maxLength={4000}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          disabled={agent.isPending}
        />
        <button
          type="submit"
          className="button button-primary self-end"
          disabled={agent.isPending || !message.trim()}
        >
          <Icon name="send" size={15} />
          {agent.isPending ? "Thinking…" : "Send"}
        </button>
      </form>
      <p className="chart-footnote">
        Answers reflect the data returned when you ask. Review important
        decisions before acting.
      </p>
    </Panel>
  );
}
