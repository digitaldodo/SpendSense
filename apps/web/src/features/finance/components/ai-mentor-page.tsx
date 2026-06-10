"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  BotMessageSquare,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Loader2,
  MessageSquareText,
  Mic,
  PiggyBank,
  ReceiptText,
  Send,
  ShieldCheck,
  Sparkles,
  Target,
  ThumbsDown,
  ThumbsUp,
  WalletCards,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAiConversation,
  useAiConversations,
  useAiInsightTimeline,
  useSendAiFeedback,
  useSendAiMessage,
} from "@/features/finance/hooks/use-finance";
import { formatMoney } from "@/features/finance/lib/format";
import type { AiChatPayload, AiInsightCard, AiMessage } from "@/features/finance/types";
import { cn } from "@/lib/utils";

const quickPrompts = [
  {
    label: "Overspend",
    prompt: "Why did I overspend this month?",
    intent: "OVERSPEND_EXPLANATION",
    icon: ReceiptText,
  },
  {
    label: "EMI safety",
    prompt: "How safe is this EMI?",
    intent: "EMI_SAFETY",
    icon: WalletCards,
  },
  {
    label: "Savings drag",
    prompt: "What category hurts my savings most?",
    intent: "CATEGORY_SAVINGS_IMPACT",
    icon: PiggyBank,
  },
  {
    label: "Health score",
    prompt: "How can I improve my financial health score?",
    intent: "HEALTH_SCORE_GUIDANCE",
    icon: ShieldCheck,
  },
  {
    label: "Last month",
    prompt: "What changed compared to last month?",
    intent: "MONTHLY_CHANGE",
    icon: Clock3,
  },
] as const;

export function AiMentorPage() {
  const conversationsQuery = useAiConversations();
  const timelineQuery = useAiInsightTimeline();
  const sendMessage = useSendAiMessage();
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [creatingNew, setCreatingNew] = useState(false);
  const [draft, setDraft] = useState("");
  const [localMessages, setLocalMessages] = useState<AiMessage[]>([]);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const conversations = conversationsQuery.data ?? [];
  const activeConversationId = creatingNew ? null : (selectedConversationId ?? conversations[0]?.id ?? null);
  const conversationQuery = useAiConversation(activeConversationId);
  const selectedConversation = conversationQuery.data;
  const displayedMessages = useMemo(
    () =>
      localMessages.length > 0 || sendMessage.isPending
        ? localMessages
        : (selectedConversation?.messages ?? []),
    [localMessages, selectedConversation?.messages, sendMessage.isPending]
  );

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: "end", behavior: "smooth" });
  }, [displayedMessages, sendMessage.isPending]);

  const activeCards = useMemo(() => {
    const latestAssistant = [...displayedMessages]
      .reverse()
      .find((message) => message.role === "ASSISTANT");
    return latestAssistant?.insightCards?.length
      ? latestAssistant.insightCards
      : (timelineQuery.data ?? []);
  }, [displayedMessages, timelineQuery.data]);

  function submit(payload?: AiChatPayload) {
    const prompt = payload?.prompt ?? draft.trim();
    if (!prompt || sendMessage.isPending) {
      return;
    }
    const optimisticUserMessage: AiMessage = {
      id: `local-${Date.now()}`,
      conversationId: activeConversationId ?? "new",
      role: "USER",
      intent: payload?.intent ?? "GENERAL_FINANCIAL_SUMMARY",
      content: prompt,
      insightCards: [],
      followUpPrompts: [],
      safetyFlags: [],
      provider: null,
      model: null,
      promptTokens: 0,
      completionTokens: 0,
      latencyMs: 0,
      createdAt: new Date().toISOString(),
    };
    setLocalMessages((current) => [
      ...(current.length > 0 ? current : displayedMessages),
      optimisticUserMessage,
    ]);
    setDraft("");
    sendMessage.mutate(
      {
        conversationId: activeConversationId ?? undefined,
        prompt,
        intent: payload?.intent,
        sourceBudgetId: payload?.sourceBudgetId,
        sourceGoalId: payload?.sourceGoalId,
        sourceTransactionId: payload?.sourceTransactionId,
      },
      {
        onSuccess(data) {
          setSelectedConversationId(data.conversation.id);
          setCreatingNew(false);
          setLocalMessages((current) => [
            ...current.filter((message) => !message.id.startsWith("local-")),
            data.userMessage,
            data.assistantMessage,
          ]);
        },
      }
    );
  }

  const latestAssistant = [...displayedMessages]
    .reverse()
    .find((message) => message.role === "ASSISTANT");
  const followUps = latestAssistant?.followUpPrompts?.length
    ? latestAssistant.followUpPrompts
    : quickPrompts.slice(0, 3).map((item) => item.prompt);

  return (
    <main className="grid gap-5">
      <section className="grid gap-4 rounded-lg border border-border bg-card p-4 shadow-raised sm:p-5 lg:grid-cols-[1fr_auto] lg:items-center">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant="outline" className="h-6 rounded-lg">
              <Sparkles className="size-3" aria-hidden />
              Ask SpendSense
            </Badge>
            <Badge variant="secondary" className="h-6 rounded-lg">
              Grounded in your ledger
            </Badge>
          </div>
          <h2 className="mt-3 text-2xl font-semibold leading-tight sm:text-3xl">
            Financial mentor
          </h2>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">
            Ask about spending, budgets, EMI pressure, goals, and monthly changes. Answers stay tied
            to SpendSense summaries and avoid investment recommendations.
          </p>
        </div>
        <div className="grid gap-2 rounded-lg border border-border/70 bg-muted/25 p-3 text-sm sm:min-w-64">
          <StatusLine label="Provider" value="Grounded mentor" />
          <StatusLine label="Mode" value="Deterministic first" />
          <StatusLine label="Boundary" value="No investment picks" />
        </div>
      </section>

      <section className="grid min-h-[calc(100vh-15rem)] gap-4 xl:grid-cols-[18rem_1fr_20rem]">
        <ConversationRail
          conversations={conversations}
          loading={conversationsQuery.isLoading}
          activeConversationId={activeConversationId}
          onSelect={(id) => {
            setCreatingNew(false);
            setSelectedConversationId(id);
          }}
          onBeforeSelect={() => setLocalMessages([])}
          onNew={() => {
            setCreatingNew(true);
            setSelectedConversationId(null);
            setLocalMessages([]);
          }}
        />

        <section className="flex min-h-[40rem] flex-col overflow-hidden rounded-lg border border-border bg-card shadow-raised">
          <div className="border-b border-border/70 px-4 py-3">
            <div className="flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="text-sm font-semibold">Mentor chat</p>
                <p className="truncate text-xs text-muted-foreground">
                  {selectedConversation?.conversation.title ?? "Start with a grounded question"}
                </p>
              </div>
              <Button size="icon" variant="outline" disabled title="Voice foundation">
                <Mic className="size-4" aria-hidden />
              </Button>
            </div>
          </div>

          <div className="flex-1 overflow-y-auto bg-[linear-gradient(180deg,var(--card),var(--muted)_480%)] px-3 py-4 sm:px-4">
            {conversationQuery.isLoading && selectedConversationId ? (
              <ChatLoading />
            ) : displayedMessages.length === 0 ? (
              <EmptyChat onPrompt={(item) => submit(item)} />
            ) : (
              <div className="grid gap-3">
                {displayedMessages.map((message, index) => (
                  <ChatBubble
                    key={message.id}
                    message={message}
                    animate={index === displayedMessages.length - 1 && message.role === "ASSISTANT"}
                  />
                ))}
                {sendMessage.isPending ? <TypingBubble /> : null}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          <div className="border-t border-border/70 bg-card p-3">
            <div className="mb-3 flex gap-2 overflow-x-auto pb-1">
              {followUps.map((prompt) => (
                <Button
                  key={prompt}
                  size="sm"
                  variant="outline"
                  className="shrink-0"
                  onClick={() => submit({ prompt })}
                  disabled={sendMessage.isPending}
                >
                  {prompt}
                </Button>
              ))}
            </div>
            <form
              className="grid gap-2 sm:grid-cols-[1fr_auto]"
              onSubmit={(event) => {
                event.preventDefault();
                submit();
              }}
            >
              <textarea
                className="min-h-12 resize-none rounded-lg border border-input bg-background px-3 py-2 text-sm leading-6 outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-3 focus:ring-ring/40"
                value={draft}
                placeholder="Ask about budgets, EMI, goals, or a spending change"
                onChange={(event) => setDraft(event.target.value)}
              />
              <Button className="h-12 sm:w-28" disabled={!draft.trim() || sendMessage.isPending}>
                {sendMessage.isPending ? (
                  <Loader2 className="size-4 animate-spin" aria-hidden />
                ) : (
                  <Send className="size-4" aria-hidden />
                )}
                Send
              </Button>
            </form>
            {sendMessage.isError ? (
              <p className="mt-2 text-sm text-destructive">
                Mentor response could not be generated. Please try again.
              </p>
            ) : null}
          </div>
        </section>

        <InsightRail
          cards={activeCards}
          loading={timelineQuery.isLoading}
          onAction={(card) =>
            submit({
              prompt: card.actionLabel,
              intent: card.actionIntent,
            })
          }
        />
      </section>
    </main>
  );
}

function ConversationRail({
  conversations,
  loading,
  activeConversationId,
  onSelect,
  onBeforeSelect,
  onNew,
}: {
  conversations: { id: string; title: string; lastMessagePreview?: string | null; lastMessageAt: string }[];
  loading: boolean;
  activeConversationId: string | null;
  onSelect: (id: string) => void;
  onBeforeSelect: () => void;
  onNew: () => void;
}) {
  return (
    <aside className="grid content-start gap-3 rounded-lg border border-border bg-card p-3 shadow-raised xl:max-h-[calc(100vh-15rem)] xl:overflow-y-auto">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-semibold">History</p>
        <Button size="sm" variant="outline" onClick={onNew}>
          New
        </Button>
      </div>
      {loading ? (
        <div className="grid gap-2">
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      ) : conversations.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border bg-muted/20 p-4 text-sm text-muted-foreground">
          Conversations appear after your first mentor question.
        </div>
      ) : (
        <div className="grid gap-2">
          {conversations.map((conversation) => (
            <button
              key={conversation.id}
              className={cn(
                "grid gap-1 rounded-lg border px-3 py-2 text-left transition-colors",
                activeConversationId === conversation.id
                  ? "border-primary bg-primary/8"
                  : "border-border/70 bg-background/60 hover:bg-muted/45"
              )}
              onClick={() => {
                onBeforeSelect();
                onSelect(conversation.id);
              }}
            >
              <span className="truncate text-sm font-medium">{conversation.title}</span>
              <span className="line-clamp-2 text-xs leading-5 text-muted-foreground">
                {conversation.lastMessagePreview || "No messages yet"}
              </span>
            </button>
          ))}
        </div>
      )}
    </aside>
  );
}

function EmptyChat({ onPrompt }: { onPrompt: (prompt: (typeof quickPrompts)[number]) => void }) {
  return (
    <div className="grid min-h-full place-items-center">
      <div className="mx-auto grid max-w-2xl gap-5 text-center">
        <div className="mx-auto grid size-12 place-items-center rounded-lg bg-primary text-primary-foreground">
          <BotMessageSquare className="size-5" aria-hidden />
        </div>
        <div>
          <h3 className="text-lg font-semibold">Ask from your SpendSense data</h3>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Choose a grounded prompt or ask in your own words.
          </p>
        </div>
        <div className="grid gap-2 sm:grid-cols-2">
          {quickPrompts.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.intent}
                aria-label={`Ask ${item.prompt}`}
                className="flex items-center justify-between gap-3 rounded-lg border border-border bg-background px-3 py-3 text-left text-sm transition-colors hover:bg-muted"
                onClick={() => onPrompt(item)}
              >
                <span className="flex min-w-0 items-center gap-2">
                  <Icon className="size-4 text-primary" aria-hidden />
                  <span className="truncate font-medium">{item.prompt}</span>
                </span>
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden />
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function ChatBubble({ message, animate }: { message: AiMessage; animate: boolean }) {
  const assistant = message.role === "ASSISTANT";
  return (
    <div className={cn("flex", assistant ? "justify-start" : "justify-end")}>
      <div
        className={cn(
          "max-w-[min(42rem,92%)] rounded-lg border px-3 py-2 text-sm leading-6 shadow-sm",
          assistant
            ? "border-border bg-background text-foreground"
            : "border-primary bg-primary text-primary-foreground"
        )}
      >
        <div className="mb-1 flex items-center gap-2 text-xs font-medium opacity-80">
          {assistant ? (
            <BotMessageSquare className="size-3.5" aria-hidden />
          ) : (
            <MessageSquareText className="size-3.5" aria-hidden />
          )}
          {assistant ? "SpendSense" : "You"}
        </div>
        {assistant && animate ? <StreamingText text={message.content} /> : <p>{message.content}</p>}
        {assistant && message.safetyFlags.length > 0 ? (
          <div className="mt-2 flex flex-wrap gap-1">
            {message.safetyFlags.map((flag) => (
              <Badge key={flag} variant="outline" className="rounded-lg">
                {flag.replaceAll("_", " ").toLowerCase()}
              </Badge>
            ))}
          </div>
        ) : null}
        {assistant ? <MessageFeedback messageId={message.id} /> : null}
      </div>
    </div>
  );
}

function StreamingText({ text }: { text: string }) {
  const words = useMemo(() => text.split(/(\s+)/), [text]);
  const [visible, setVisible] = useState(0);

  useEffect(() => {
    const interval = window.setInterval(() => {
      setVisible((current) => {
        if (current >= words.length) {
          window.clearInterval(interval);
          return current;
        }
        return current + 4;
      });
    }, 24);
    return () => window.clearInterval(interval);
  }, [words.length]);

  return <p>{words.slice(0, visible).join("")}</p>;
}

function TypingBubble() {
  return (
    <div className="flex justify-start">
      <div className="flex items-center gap-2 rounded-lg border border-border bg-background px-3 py-2 text-sm text-muted-foreground shadow-sm">
        <Loader2 className="size-4 animate-spin text-primary" aria-hidden />
        SpendSense is checking your summaries
      </div>
    </div>
  );
}

function ChatLoading() {
  return (
    <div className="grid gap-3">
      <Skeleton className="h-20 w-4/5" />
      <Skeleton className="ml-auto h-14 w-3/5" />
      <Skeleton className="h-24 w-5/6" />
    </div>
  );
}

function InsightRail({
  cards,
  loading,
  onAction,
}: {
  cards: AiInsightCard[];
  loading: boolean;
  onAction: (card: AiInsightCard) => void;
}) {
  return (
    <aside className="grid content-start gap-3 rounded-lg border border-border bg-card p-3 shadow-raised xl:max-h-[calc(100vh-15rem)] xl:overflow-y-auto">
      <div>
        <p className="text-sm font-semibold">Insight timeline</p>
        <p className="mt-1 text-xs leading-5 text-muted-foreground">
          Deterministic facts used by mentor replies.
        </p>
      </div>
      {loading ? (
        <div className="grid gap-2">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-28 w-full" />
        </div>
      ) : cards.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border bg-muted/20 p-4 text-sm text-muted-foreground">
          Insight cards appear after SpendSense has enough posted data.
        </div>
      ) : (
        <div className="grid gap-2">
          {cards.map((card) => (
            <div key={`${card.type}-${card.title}`} className="rounded-lg border border-border/70 bg-background/65 p-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold">{card.title}</p>
                  <p className="mt-1 text-xs leading-5 text-muted-foreground">{card.body}</p>
                </div>
                <StateIcon state={card.state} />
              </div>
              <div className="mt-3 grid grid-cols-2 gap-2">
                <MiniMetric label="Current" value={formatCardValue(card.primaryValue)} />
                <MiniMetric label="Compare" value={formatCardValue(card.comparisonValue)} />
              </div>
              <Button className="mt-3 w-full" size="sm" variant="outline" onClick={() => onAction(card)}>
                <Target className="size-4" aria-hidden />
                {card.actionLabel}
              </Button>
            </div>
          ))}
        </div>
      )}
    </aside>
  );
}

function MessageFeedback({ messageId }: { messageId: string }) {
  const positive = useSendAiFeedback(messageId);
  const negative = useSendAiFeedback(messageId);
  return (
    <div className="mt-3 flex items-center gap-1 border-t border-border/70 pt-2">
      <Button
        size="icon-xs"
        variant="ghost"
        title="Helpful"
        onClick={() => positive.mutate({ rating: 5, feedbackType: "HELPFUL" })}
        disabled={positive.isPending || negative.isPending}
      >
        <ThumbsUp className="size-3" aria-hidden />
      </Button>
      <Button
        size="icon-xs"
        variant="ghost"
        title="Not helpful"
        onClick={() => negative.mutate({ rating: 2, feedbackType: "NOT_HELPFUL" })}
        disabled={positive.isPending || negative.isPending}
      >
        <ThumbsDown className="size-3" aria-hidden />
      </Button>
      {positive.isSuccess || negative.isSuccess ? (
        <span className="ml-1 text-xs text-muted-foreground">Saved</span>
      ) : null}
    </div>
  );
}

function StatusLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/70 bg-muted/25 px-2 py-2">
      <p className="text-[0.7rem] text-muted-foreground">{label}</p>
      <p className="mt-1 truncate text-xs font-semibold tabular-nums">{value}</p>
    </div>
  );
}

function StateIcon({ state }: { state: string }) {
  const normalized = state.toUpperCase();
  return (
    <span
      className={cn(
        "grid size-7 shrink-0 place-items-center rounded-lg",
        normalized === "RISK"
          ? "bg-info/12 text-info"
          : normalized === "CAUTION"
            ? "bg-warning/18 text-warning"
            : "bg-success/12 text-success"
      )}
    >
      <CheckCircle2 className="size-4" aria-hidden />
    </span>
  );
}

function formatCardValue(value: number) {
  if (Math.abs(value) <= 100 && value % 1 !== 0) {
    return `${Math.round(value)}%`;
  }
  return formatMoney(value);
}
