import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def test_architecture_boundaries() -> None:
    result = subprocess.run(
        [sys.executable, str(ROOT / "scripts" / "check_boundaries.py")],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + "\n" + result.stderr
