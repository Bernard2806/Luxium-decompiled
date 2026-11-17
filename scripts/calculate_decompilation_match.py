#!/usr/bin/env python3
"""Report how closely this decompilation matches the original mod JAR.

The original 2.8.4 JAR is a Forgix merge of a Forge build (classes under
forge/com/vinlanx/) and a Fabric build (classes under fabric/com/vinlanx/).
Both copies are measured. Their sources are common/src/main/java plus the
loader's own <loader>/src/main/java.

Three measurements are reported:

- Source coverage: a top-level class counts as matched when its source file
  exists and contains no method that the decompiler failed to reconstruct.
- Bytecode match (with --built-jar): every method of the original JAR is
  compared, instruction by instruction, against the same method in the JAR
  built from this repository. Constant pool indices, branch offsets and the
  numeric suffix of lambda names are ignored, since they change on every
  recompilation without changing behaviour.
- Byte-identical classes (with --built-jar): every .class file of the original
  JAR is compared byte for byte with the same entry of the built JAR.

The original JAR is not part of the repository, so its classes and methods are
stored as lists and hashes in .github/release/. Regenerate those files with
--write-index <original.jar>.

With --map <file.svg>, the method and byte-identical class measurements are
also drawn as a treemap with one cell per method and per class, grouped by
loader and package.
"""

import argparse
import glob
import hashlib
import os
import re
import struct
import zipfile
from collections import Counter
from pathlib import Path

from decompilation_map import render


ROOT = Path(__file__).resolve().parents[1]
CLASS_INDEX = ROOT / ".github" / "release" / "original-class-index.txt"
METHOD_INDEX = ROOT / ".github" / "release" / "original-method-index.txt"
CLASS_HASH_INDEX = ROOT / ".github" / "release" / "original-class-hashes.txt"
LOADERS = ("forge", "fabric")
VERSION = "2.8.4-pre-alpha"
FAILURE_MARKER = "This method has failed to decompile"
LAMBDA_SUFFIX = re.compile(r"^(lambda\$.*)\$\d+$")
PACKAGE_PREFIX = "com/vinlanx/luxium/"
CLASS_PREFIXES = tuple(f"{loader}/com/vinlanx/" for loader in LOADERS)


def source_roots(loader):
    """Source folders compiled into one loader's copy, in the order Gradle adds them."""
    return [ROOT / loader / "src" / "main" / "java", ROOT / "common" / "src" / "main" / "java"]

# Operand sizes for fixed-length opcodes; opcodes not listed take no operands.
CP_U1 = {18}
CP_U2 = {19, 20, 178, 179, 180, 181, 182, 183, 184, 187, 189, 192, 193}
LOCAL_U1 = {21, 22, 23, 24, 25, 54, 55, 56, 57, 58, 169}
BRANCH_U2 = set(range(153, 169)) | {198, 199}
BRANCH_U4 = {200, 201}


def read_constant_pool(data):
    count = struct.unpack_from(">H", data, 8)[0]
    raw = [None] * count
    pos, index = 10, 1
    while index < count:
        tag = data[pos]
        if tag == 1:
            length = struct.unpack_from(">H", data, pos + 1)[0]
            raw[index] = (tag, data[pos + 3:pos + 3 + length].decode("utf-8", "replace"))
            pos += 3 + length
        elif tag in (3, 4):
            raw[index] = (tag, data[pos + 1:pos + 5].hex())
            pos += 5
        elif tag in (5, 6):
            raw[index] = (tag, data[pos + 1:pos + 9].hex())
            pos += 9
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            raw[index] = (tag, struct.unpack_from(">H", data, pos + 1)[0])
            pos += 3
        elif tag == 15:
            raw[index] = (tag, struct.unpack_from(">H", data, pos + 2)[0])
            pos += 4
        elif tag in (9, 10, 11, 12, 17, 18):
            raw[index] = (tag, struct.unpack_from(">HH", data, pos + 1))
            pos += 5
        else:
            raise ValueError(f"unknown constant pool tag {tag}")
        index += 1

    def resolve(i):
        tag, value = raw[i]
        if tag == 1 or tag in (3, 4, 5, 6):
            return value
        if tag in (7, 8, 16, 19, 20):
            return resolve(value)
        if tag == 15:
            return resolve(value)
        if tag == 12:
            return f"{resolve(value[0])}:{resolve(value[1])}"
        if tag in (9, 10, 11):
            return f"{resolve(value[0])}.{resolve(value[1])}"
        # Dynamic / InvokeDynamic: skip the bootstrap method, which names the lambda.
        return resolve(value[1])

    return pos, resolve


def normalize_code(code, resolve):
    tokens = []
    pos = 0
    while pos < len(code):
        op = code[pos]
        start = pos
        pos += 1
        if op in CP_U1:
            tokens.append(f"{op} {resolve(code[pos])}")
            pos += 1
        elif op in CP_U2:
            tokens.append(f"{op} {resolve(struct.unpack_from('>H', code, pos)[0])}")
            pos += 2
        elif op in LOCAL_U1 or op in (16, 188):
            tokens.append(f"{op} {code[pos]}")
            pos += 1
        elif op == 17:
            tokens.append(f"{op} {struct.unpack_from('>h', code, pos)[0]}")
            pos += 2
        elif op == 132:
            tokens.append(f"{op} {code[pos]} {struct.unpack_from('>b', code, pos + 1)[0]}")
            pos += 2
        elif op in BRANCH_U2:
            tokens.append(str(op))
            pos += 2
        elif op in BRANCH_U4:
            tokens.append(str(op))
            pos += 4
        elif op in (185, 186):
            tokens.append(f"{op} {resolve(struct.unpack_from('>H', code, pos)[0])}")
            pos += 4
        elif op == 197:
            tokens.append(f"{op} {resolve(struct.unpack_from('>H', code, pos)[0])} {code[pos + 2]}")
            pos += 3
        elif op in (170, 171):
            pos += (4 - pos % 4) % 4
            if op == 170:
                low, high = struct.unpack_from(">ii", code, pos + 4)
                pos += 12 + 4 * (high - low + 1)
                tokens.append(f"{op} {low} {high}")
            else:
                pairs = struct.unpack_from(">i", code, pos + 4)[0]
                keys = [struct.unpack_from(">i", code, pos + 8 + 8 * i)[0] for i in range(pairs)]
                pos += 8 + 8 * pairs
                tokens.append(f"{op} {keys}")
        elif op == 196:
            inner = code[pos]
            if inner == 132:
                tokens.append(f"{op} {inner} {struct.unpack_from('>Hh', code, pos + 1)}")
                pos += 5
            else:
                tokens.append(f"{op} {inner} {struct.unpack_from('>H', code, pos + 1)[0]}")
                pos += 3
        else:
            tokens.append(str(op))
        assert pos > start
    return "\n".join(tokens)


def class_methods(data):
    """Yield (method name, descriptor, hash) for every method with a Code attribute."""
    pos, resolve = read_constant_pool(data)
    pos += 6
    interfaces = struct.unpack_from(">H", data, pos)[0]
    pos += 2 + 2 * interfaces

    def skip_attributes(p):
        count = struct.unpack_from(">H", data, p)[0]
        p += 2
        for _ in range(count):
            p += 6 + struct.unpack_from(">I", data, p + 2)[0]
        return p

    fields = struct.unpack_from(">H", data, pos)[0]
    pos += 2
    for _ in range(fields):
        pos = skip_attributes(pos + 6)
    methods = struct.unpack_from(">H", data, pos)[0]
    pos += 2
    for _ in range(methods):
        name_index, descriptor_index, attribute_count = struct.unpack_from(">HHH", data, pos + 2)
        pos += 8
        name, descriptor = resolve(name_index), resolve(descriptor_index)
        for _ in range(attribute_count):
            attribute_name = resolve(struct.unpack_from(">H", data, pos)[0])
            length = struct.unpack_from(">I", data, pos + 2)[0]
            if attribute_name == "Code":
                code_length = struct.unpack_from(">I", data, pos + 10)[0]
                code = data[pos + 14:pos + 14 + code_length]
                digest = hashlib.sha1(normalize_code(code, resolve).encode("utf-8")).hexdigest()[:16]
                match = LAMBDA_SUFFIX.match(name)
                yield (match.group(1) if match else name), descriptor, digest
            pos += 6 + length


def jar_classes(jar_path):
    """Return {class entry: bytes} for the mod's classes of both loaders."""
    with zipfile.ZipFile(jar_path) as jar:
        return {
            entry: jar.read(entry)
            for entry in jar.namelist()
            if entry.startswith(CLASS_PREFIXES) and entry.endswith(".class")
        }


def jar_methods(classes):
    """Return a Counter of (class entry, method name, descriptor, hash)."""
    methods = Counter()
    for entry, data in classes.items():
        for name, descriptor, digest in class_methods(data):
            methods[(entry, name, descriptor, digest)] += 1
    return methods


def class_hashes(classes):
    return {entry: hashlib.sha256(data).hexdigest() for entry, data in classes.items()}


def write_lines(path, header, rows):
    path.write_text("\n".join(header + rows) + "\n", encoding="utf-8")
    print(f"Wrote {len(rows)} entries to {path.relative_to(ROOT)}")


def write_index(jar_path):
    classes = jar_classes(jar_path)
    regenerate = "# Regenerate with: python3 scripts/calculate_decompilation_match.py --write-index <original.jar>"
    write_lines(CLASS_INDEX, [
        f"# Top-level .class entries from the original Luxium {VERSION} JAR (forge/ and fabric/ copies).",
        "# Nested classes share the source file of their enclosing top-level class.",
        regenerate,
    ], sorted(entry for entry in classes if "$" not in entry.rpartition("/")[2]))
    write_lines(CLASS_HASH_INDEX, [
        f"# SHA-256 of every .class file of the original Luxium {VERSION} JAR.",
        "# Format: class entry<TAB>sha256.",
        regenerate,
    ], [f"{entry}\t{digest}" for entry, digest in sorted(class_hashes(classes).items())])
    write_lines(METHOD_INDEX, [
        f"# Normalized bytecode hashes for every method of the original Luxium {VERSION} JAR.",
        "# Format: class entry<TAB>method name<TAB>descriptor<TAB>hash. Lambda names drop their numeric suffix.",
        regenerate,
    ], ["\t".join(key) for key in sorted(jar_methods(classes).elements())])


def read_index(path):
    return [
        line.split("\t")
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.startswith("#")
    ]


def bytecode_match(built_classes):
    original = Counter(tuple(row) for row in read_index(METHOD_INDEX))
    if not original:
        raise SystemExit(f"No original methods listed in {METHOD_INDEX}")
    identical = original & jar_methods(built_classes)
    return package_groups(identical, original - identical)


def class_match(built_classes):
    original = dict(read_index(CLASS_HASH_INDEX))
    if not original:
        raise SystemExit(f"No original classes listed in {CLASS_HASH_INDEX}")
    built = class_hashes(built_classes)
    identical = Counter(entry for entry, digest in original.items() if built.get(entry) == digest)
    return package_groups(identical, Counter(original.keys() - identical.keys()))


def package_label(class_entry):
    loader, _, rest = class_entry.partition("/")
    package = rest.rpartition("/")[0] + "/"
    return f"{loader}: " + (package.removeprefix(PACKAGE_PREFIX).rstrip("/").replace("/", ".") or "luxium")


def package_groups(done, todo):
    """Count Counter entries (keyed by class entry first) per package."""
    groups = {}
    for state, items in (("done", done), ("todo", todo)):
        for key, count in items.items():
            entry = key[0] if isinstance(key, tuple) else key
            name = package_label(entry)
            label = name.partition(": ")[2].rpartition(".")[2]
            group = groups.setdefault(name, {"name": name, "label": label, "done": 0, "todo": 0})
            group[state] += count
    return sorted(groups.values(), key=lambda g: g["name"])


def totals(groups):
    done = sum(g["done"] for g in groups)
    return done, done + sum(g["todo"] for g in groups)


def find_built_jar(pattern):
    matches = [path for path in glob.glob(str(ROOT / pattern)) if not path.endswith(("-sources.jar", "-slim.jar"))]
    if len(matches) != 1:
        raise SystemExit(f"Expected exactly one built JAR matching {pattern}, found {matches}")
    return matches[0]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--built-jar", help="glob, relative to the repository root, of the JAR built from this repository")
    parser.add_argument("--write-index", metavar="ORIGINAL_JAR", help="regenerate the original method index and exit")
    parser.add_argument("--map", type=Path, metavar="SVG", help="write a treemap of both measurements (requires --built-jar)")
    args = parser.parse_args()

    if args.write_index:
        write_index(args.write_index)
        return
    if args.map and not args.built_jar:
        parser.error("--map requires --built-jar")

    original_classes = {
        line.strip()
        for line in CLASS_INDEX.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }
    if not original_classes:
        raise SystemExit(f"No original classes listed in {CLASS_INDEX}")

    source_files = {}
    for loader in LOADERS:
        for root in source_roots(loader):
            for path in root.rglob("*.java"):
                class_path = f"{loader}/" + path.relative_to(root).with_suffix(".class").as_posix()
                source_files.setdefault(class_path, path.read_text(encoding="utf-8").count(FAILURE_MARKER))
    present = original_classes & source_files.keys()
    failed_methods = sum(source_files[name] for name in present)
    decompiled = Counter(name for name in present if source_files[name] == 0)
    class_groups = package_groups(decompiled, Counter(original_classes - decompiled.keys()))
    matched, total = totals(class_groups)
    percentage = f"{matched / total * 100:.2f}"

    print(f"Decompiled source coverage: {matched}/{total} ({percentage}%)")
    print(f"Methods that failed to decompile: {failed_methods}")
    outputs = {
        "percentage": percentage,
        "matched": matched,
        "total": total,
        "failed_methods": failed_methods,
    }

    if args.built_jar:
        built_classes = jar_classes(find_built_jar(args.built_jar))
        method_groups = bytecode_match(built_classes)
        identical, method_total = totals(method_groups)
        bytecode_percentage = f"{identical / method_total * 100:.2f}"
        print(f"Bytecode match: {identical}/{method_total} methods identical ({bytecode_percentage}%)")
        byte_groups = class_match(built_classes)
        byte_identical, class_total = totals(byte_groups)
        class_percentage = f"{byte_identical / class_total * 100:.2f}"
        print(f"Byte-identical classes: {byte_identical}/{class_total} ({class_percentage}%)")
        outputs.update(
            bytecode_percentage=bytecode_percentage,
            bytecode_identical=identical,
            bytecode_total=method_total,
            class_percentage=class_percentage,
            class_identical=byte_identical,
            class_total=class_total,
        )
        if args.map:
            args.map.write_text(
                render("Bytecode-identical methods", method_groups, "Byte-identical classes", byte_groups),
                encoding="utf-8",
            )
            print(f"Wrote {args.map}")

    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as output:
            for key, value in outputs.items():
                output.write(f"{key}={value}\n")


if __name__ == "__main__":
    main()
