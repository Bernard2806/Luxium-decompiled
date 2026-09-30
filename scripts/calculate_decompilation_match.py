#!/usr/bin/env python3
"""Report source-file coverage against the original mod's top-level classes."""

import os
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CLASS_INDEX = ROOT / ".github" / "release" / "original-class-index.txt"
SOURCE_ROOT = ROOT / "src" / "main" / "java"


def main() -> None:
    original_classes = {
        line.strip()
        for line in CLASS_INDEX.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }
    if not original_classes:
        raise SystemExit(f"No original classes listed in {CLASS_INDEX}")

    source_files = {
        path.relative_to(SOURCE_ROOT).with_suffix(".class").as_posix()
        for path in SOURCE_ROOT.rglob("*.java")
    }
    matched = len(original_classes & source_files)
    total = len(original_classes)
    percentage = f"{matched / total * 100:.2f}"

    print(f"Decompiled source coverage: {matched}/{total} ({percentage}%)")
    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as output:
            output.write(f"percentage={percentage}\n")
            output.write(f"matched={matched}\n")
            output.write(f"total={total}\n")


if __name__ == "__main__":
    main()
