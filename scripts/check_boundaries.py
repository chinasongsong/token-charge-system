import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
KNOWN_VIOLATIONS = ROOT / "tests" / "architecture" / "known_violations.json"

JAVA_FILE_PATTERN = "**/*.java"

# 基于目录名的轻量规则，适合初始化阶段。
LAYER_PATTERNS = {
    "presentation": re.compile(r"(\\|/)presentation(\\|/)"),
    "application": re.compile(r"(\\|/)application(\\|/)"),
    "domain": re.compile(r"(\\|/)domain(\\|/)"),
    "infrastructure": re.compile(r"(\\|/)infrastructure(\\|/)"),
}

FORBIDDEN_IMPORTS = {
    "domain": [
        "application",
        "presentation",
        "infrastructure",
        "org.springframework",
        "jakarta.persistence",
    ],
    "application": ["presentation"],
}


def detect_layer(path_str: str) -> str | None:
    for layer, pattern in LAYER_PATTERNS.items():
        if pattern.search(path_str):
            return layer
    return None


def load_known_violations() -> set[str]:
    if not KNOWN_VIOLATIONS.exists():
        return set()
    try:
        payload = json.loads(KNOWN_VIOLATIONS.read_text(encoding="utf-8"))
        return set(payload.get("violations", []))
    except Exception:
        return set()


def scan_files() -> list[str]:
    violations = []
    for file_path in ROOT.glob(JAVA_FILE_PATTERN):
        normalized = str(file_path).replace("\\", "/")
        if "/target/" in normalized or "/build/" in normalized:
            continue

        layer = detect_layer(str(file_path))
        if not layer or layer not in FORBIDDEN_IMPORTS:
            continue

        text = file_path.read_text(encoding="utf-8", errors="ignore")
        imports = [line.strip() for line in text.splitlines() if line.strip().startswith("import ")]
        for imp in imports:
            for keyword in FORBIDDEN_IMPORTS[layer]:
                if keyword in imp:
                    violations.append(f"{normalized}: {layer} 发现非法依赖 -> {imp}")
    return violations


def main() -> int:
    current = set(scan_files())
    known = load_known_violations()
    new_violations = sorted(current - known)

    if new_violations:
        print("发现新增架构边界违规：")
        for v in new_violations:
            print(f"- {v}")
        print("\n修复建议：")
        print("1) 将依赖下沉到 application/domain 接口。")
        print("2) 用 infrastructure 适配器实现外部依赖。")
        print("3) 若是历史遗留，请登记到 tests/architecture/known_violations.json。")
        return 1

    print("边界检查通过：未发现新增违规。")
    if current:
        print(f"当前已登记违规数：{len(current)}（历史基线）。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
