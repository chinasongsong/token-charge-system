import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]

REQUIRED_FILES = [
    ROOT / "AGENTS.md",
    ROOT / "ARCHITECTURE.md",
    ROOT / "docs" / "architecture" / "LAYERS.md",
    ROOT / "docs" / "SECURITY.md",
]


def main() -> int:
    missing = [str(p.relative_to(ROOT)) for p in REQUIRED_FILES if not p.exists()]
    if missing:
        print("文档漂移告警：以下关键文件缺失")
        for item in missing:
            print(f"- {item}")
        return 1

    print("GC 扫描通过：关键工程文档齐全。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
