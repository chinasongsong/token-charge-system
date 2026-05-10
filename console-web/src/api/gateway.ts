/**
 * 网关 OpenAI 兼容接口返回原始 JSON（非 ApiResponse 信封）。
 */

const base = import.meta.env.VITE_API_BASE ?? "";

function bearerHeaders(): HeadersInit {
  const t = typeof localStorage !== "undefined" ? localStorage.getItem("accessToken") : null;
  const h: Record<string, string> = { "Content-Type": "application/json" };
  if (t) {
    h.Authorization = `Bearer ${t}`;
  }
  return h;
}

async function readErrorMessage(res: Response, text: string): Promise<string> {
  try {
    const j = JSON.parse(text) as { code?: string; message?: string };
    if (j.message) {
      return j.message;
    }
  } catch {
    /* ignore */
  }
  return text || `HTTP ${res.status}`;
}

export type OpenAiModel = { id: string };

export async function fetchModels(): Promise<OpenAiModel[]> {
  const res = await fetch(`${base}/v1/models`, { headers: bearerHeaders() });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, text));
  }
  const j = JSON.parse(text) as { data?: OpenAiModel[] };
  return j.data ?? [];
}

export async function chatCompletions(body: unknown): Promise<unknown> {
  const res = await fetch(`${base}/v1/chat/completions`, {
    method: "POST",
    headers: bearerHeaders(),
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, text));
  }
  return JSON.parse(text);
}
