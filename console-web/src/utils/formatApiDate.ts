/** 兼容 Jackson 写出为 ISO 字符串或 LocalDateTime 数组的日期 */
export function formatApiDate(v: unknown): string {
  if (v == null || v === "") return "—";
  if (typeof v === "string") {
    return v.replace("T", " ").slice(0, 19);
  }
  if (Array.isArray(v) && v.length >= 6) {
    const y = v[0];
    const m = String(v[1]).padStart(2, "0");
    const d = String(v[2]).padStart(2, "0");
    const h = String(v[3] ?? 0).padStart(2, "0");
    const mi = String(v[4] ?? 0).padStart(2, "0");
    const s = String(Math.floor(Number(v[5])))?.padStart?.(2, "0") ?? "00";
    return `${y}-${m}-${d} ${h}:${mi}:${s}`;
  }
  return String(v);
}
