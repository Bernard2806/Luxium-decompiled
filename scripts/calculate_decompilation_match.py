#!/usr/bin/env python3
"""Report source-file coverage against the original mod's top-level classes.

A class only counts as matched when its source file exists and contains no
method that the decompiler failed to reconstruct.
"""

import os
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CLASS_INDEX = ROOT / ".github" / "release" / "original-class-index.txt"
SOURCE_ROOT = ROOT / "src" / "main" / "java"
FAILURE_MARKER = "This method has failed to decompile"


def main() -> None:
    original_classes = {
        line.strip()
        for line in CLASS_INDEX.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }
    if not original_classes:
        raise SystemExit(f"No original classes listed in {CLASS_INDEX}")

    source_files = {}
    for path in SOURCE_ROOT.rglob("*.java"):
        class_path = path.relative_to(SOURCE_ROOT).with_suffix(".class").as_posix()
        source_files[class_path] = path.read_text(encoding="utf-8").count(FAILURE_MARKER)
    present = original_classes & source_files.keys()
    failed_methods = sum(source_files[name] for name in present)
    matched = sum(1 for name in present if source_files[name] == 0)
    total = len(original_classes)
    percentage = f"{matched / total * 100:.2f}"

    print(f"Decompiled source coverage: {matched}/{total} ({percentage}%)")
    print(f"Methods that failed to decompile: {failed_methods}")
    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as output:
            output.write(f"percentage={percentage}\n")
            output.write(f"matched={matched}\n")
            output.write(f"total={total}\n")
            output.write(f"failed_methods={failed_methods}\n")


if __name__ == "__main__":
    main()
