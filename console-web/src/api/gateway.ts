/**
 * 网关 OpenAI 兼容接口返回原始 JSON（非 ApiResponse 信封）。
 */

const EXPERIENCE_SK = "tokenhub_experience_sk";

const base = import.meta.env.VITE_API_BASE ?? "";

/** 模型体验页：填入的 sk_tokenhub_… 仅存 sessionStorage */
export function setExperienceSk(plaintext: string | null): void {
  if (plaintext == null || plaintext.trim() === "") {
    sessionStorage.removeItem(EXPERIENCE_SK);
    return;
  }
  sessionStorage.setItem(EXPERIENCE_SK, plaintext.trim());
}

export function clearExperienceSk(): void {
  sessionStorage.removeItem(EXPERIENCE_SK);
}

function resolveBearer(opts?: { preferApiKey?: boolean }): string {
  const sk =
    typeof sessionStorage !== "undefined" ? sessionStorage.getItem(EXPERIENCE_SK) : null;
  const jwt =
    typeof localStorage !== "undefined" ? localStorage.getItem("accessToken") : null;

  if (opts?.preferApiKey && sk) return sk;
  if (jwt) return jwt;
  if (sk) return sk;
  return "";
}

function bearerHeaders(opts?: { preferApiKey?: boolean }): HeadersInit {
  const token = resolveBearer(opts);
  const h: Record<string, string> = { "Content-Type": "application/json" };
  if (token) {
    h.Authorization = `Bearer ${token}`;
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

export async function fetchModels(opts?: { preferApiKey?: boolean }): Promise<OpenAiModel[]> {
  const res = await fetch(`${base}/v1/models`, { headers: bearerHeaders(opts) });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, text));
  }
  const j = JSON.parse(text) as { data?: OpenAiModel[] };
  return j.data ?? [];
}

export async function chatCompletions(body: unknown, opts?: { preferApiKey?: boolean }): Promise<unknown> {
  const res = await fetch(`${base}/v1/chat/completions`, {
    method: "POST",
    headers: bearerHeaders(opts),
    body: JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, text));
  }
  return JSON.parse(text);
}
