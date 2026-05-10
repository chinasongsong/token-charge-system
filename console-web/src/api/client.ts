const base = import.meta.env.VITE_API_BASE ?? "";

function authHeader(): Record<string, string> {
  const t = localStorage.getItem("accessToken");
  const h: Record<string, string> = { "Content-Type": "application/json" };
  if (t) {
    h.Authorization = `Bearer ${t}`;
  }
  return h;
}

export async function apiJson<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${base}${path}`, { ...init, headers: { ...authHeader(), ...(init?.headers as object) } });
  const j = (await res.json()) as {
    code: string;
    message: string;
    data: T;
  };
  if (j.code !== "0") {
    throw new Error(j.message || `HTTP ${res.status}`);
  }
  return j.data;
}

export function saveToken(accessToken: string) {
  localStorage.setItem("accessToken", accessToken);
}

export function clearToken() {
  localStorage.removeItem("accessToken");
}
