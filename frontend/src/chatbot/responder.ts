import type { WorkOrder, WorkOrderStatus } from '../types';

// SLA windows mirror keystone.sla.hours.* in the backend's application.yml —
// keep these two in sync if that config ever changes.
const SLA_HOURS: Record<string, number> = { CRITICAL: 4, HIGH: 24, MEDIUM: 72, LOW: 168 };

const STATUS_LABEL: Record<WorkOrderStatus, string> = {
  NEW: 'received, not yet assigned',
  ASSIGNED: 'assigned to a technician',
  IN_PROGRESS: 'being worked on',
  ON_HOLD: 'on hold',
  COMPLETED: 'completed, pending close-out',
  CLOSED: 'closed',
  CANCELLED: 'cancelled'
};

export interface ChatContext {
  workOrders: WorkOrder[];
  firstName: string;
}

function slaWindowText(priority: string): string {
  const hours = SLA_HOURS[priority];
  if (!hours) return '';
  if (hours < 24) return `${hours} hours`;
  const days = hours / 24;
  return `${days} ${days === 1 ? 'day' : 'days'}`;
}

function describeWorkOrder(wo: WorkOrder): string {
  const due = new Date(wo.slaDueAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
  const slaLine =
    wo.slaState === 'BREACHED'
      ? "This one's past its SLA window — it's been escalated."
      : wo.slaState === 'AT_RISK'
        ? "It's getting close to its SLA window."
        : "It's on track against its SLA.";
  const assignee = wo.assignedToName ? `assigned to ${wo.assignedToName}` : 'not yet assigned to a technician';
  return `${wo.code} — "${wo.title}"\nStatus: ${STATUS_LABEL[wo.status]}, ${assignee}.\nPriority: ${wo.priority.toLowerCase()} (SLA due ${due}). ${slaLine}`;
}

const GREETING = /^\s*(hi|hello|hey|yo)\b/i;
const THANKS = /^\s*(thanks|thank you|thankyou|cheers|ty)\b/i;
const CODE = /\bwo[\s-]?(\d{3,6})\b/i;
const MY_REQUESTS = /\b(my|open|pending|active|current)\s+(requests?|tickets?|jobs?)\b|how many (requests?|tickets?|jobs?)/i;
const RAISE = /\b(raise|submit|create|open|file|report|log)\b.*\b(request|ticket|issue|job|problem)\b|how do i (raise|submit|report)/i;
const SLA = /\bslas?\b|\bpriorit(y|ies)\b|how (fast|long|quick|soon)/i;
const LIFECYCLE = /what happens|next step|how (does|do) (it|this|the process) work|lifecycle|what.?s the process/i;
const HUMAN = /\b(human|person|agent|someone|representative|call|phone)\b/i;

/** Pure rule-based responder — no external API, so it works offline and costs
 * nothing to run. Deliberately narrow: it knows the KEYSTONE domain (request
 * lifecycle, SLA windows, and the caller's own work orders) and says so
 * plainly when a question falls outside that. */
export function respond(message: string, ctx: ChatContext): string {
  const q = message.trim();
  if (!q) return "Go ahead — ask me about a request, or how to raise one.";

  if (GREETING.test(q)) {
    return `Hi ${ctx.firstName}! I can help you raise a request, check on an existing one, or explain how priorities and SLAs work. What do you need?`;
  }
  if (THANKS.test(q)) return "You're welcome! Anything else I can help with?";

  const codeMatch = q.match(CODE);
  if (codeMatch) {
    const code = `WO-${codeMatch[1]}`;
    const wo = ctx.workOrders.find((w) => w.code.toUpperCase() === code.toUpperCase());
    return wo
      ? describeWorkOrder(wo)
      : `I can't find ${code} under your account. Double-check the code on "My Requests" — I can only see requests raised by your own organisation.`;
  }

  if (MY_REQUESTS.test(q)) {
    const open = ctx.workOrders.filter((w) => w.status !== 'CLOSED' && w.status !== 'CANCELLED');
    if (open.length === 0) return "You don't have any open requests right now. Want to raise one?";
    const lines = open.slice(0, 5).map((w) => `• ${w.code} — ${w.title} (${STATUS_LABEL[w.status]})`).join('\n');
    const more = open.length > 5 ? `\n…and ${open.length - 5} more on "My Requests".` : '';
    return `You have ${open.length} open request${open.length === 1 ? '' : 's'}:\n${lines}${more}`;
  }

  if (RAISE.test(q)) {
    return 'Click "Raise a Request" in the left sidebar, describe the issue, pick your site and a priority, then submit. It enters our system immediately and you can track it from "My Requests" right away.';
  }

  if (SLA.test(q)) {
    return `Response targets by priority: Critical — ${slaWindowText('CRITICAL')}, High — ${slaWindowText('HIGH')}, Medium — ${slaWindowText('MEDIUM')}, Low — ${slaWindowText('LOW')}. Each request's page shows its exact due time and whether it's on track, at risk, or past due.`;
  }

  if (LIFECYCLE.test(q)) {
    return 'Every request follows the same path: New → Assigned to a technician → In Progress → Completed → Closed. You\'ll see the status change in real time on "My Requests", with a full history of who did what and when.';
  }

  if (HUMAN.test(q)) {
    return "I'll get you there — email your account team or call the number on your service agreement, and mention your request code (e.g. WO-1002) so they can pull it up instantly.";
  }

  return "I'm not sure about that one. I can help with: raising a new request, checking on an existing one (give me its code, like WO-1002), or explaining priorities and SLAs. For anything else, ask to speak to a human and I'll point you the right way.";
}

export const SUGGESTIONS = ['How do I raise a request?', 'What are my open requests?', 'How do SLAs work?'];
