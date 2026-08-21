"""Deterministic Phase1 SHA-256 provenance verifier.

Recipe (confirmed to reproduce both documented expected hashes exactly):
  1. Take the GENERATED WebSquare output XML (not the source .xfdl).
  2. Parse with an XML parser; collect the text content (itertext, concatenated)
     of every <script> element in document order.
  3. Join the per-element text with a single "\n" separator.
  4. Append exactly one trailing "\n" (final-newline-if-missing normalization).
  5. Encode as UTF-8, no BOM.
  6. SHA-256 the resulting bytes.

Usage: python phase1_sha_verifier.py <manifest.json>
manifest.json: {"cases": [{"name": "...", "xml": "path/to/output.xml", "expected": "hexhash"}, ...]}
"""
import hashlib
import json
import os
import sys
import xml.etree.ElementTree as ET


def extract_script_hash(xml_path):
    root = ET.parse(xml_path).getroot()
    parts = []
    for e in root.iter():
        if e.tag.split("}")[-1].lower() == "script":
            parts.append("".join(e.itertext()))
    content = "\n".join(parts)
    content = content.rstrip("\r\n") + "\n"
    data = content.encode("utf-8")
    return hashlib.sha256(data).hexdigest(), len(data)


def main():
    if len(sys.argv) != 2:
        print("usage: phase1_sha_verifier.py <manifest.json>")
        sys.exit(2)
    manifest_path = sys.argv[1]
    # Resolve relative "xml" entries against the manifest file's own directory, not the
    # process's current working directory, so the result is deterministic regardless of
    # where verify-offline.bat/.sh is invoked from.
    manifest_dir = os.path.dirname(os.path.abspath(manifest_path))
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)
    all_pass = True
    for case in manifest["cases"]:
        xml_path = case["xml"]
        if not os.path.isabs(xml_path):
            xml_path = os.path.join(manifest_dir, xml_path)
        actual, byte_count = extract_script_hash(xml_path)
        expected = case["expected"]
        status = "PASS" if actual == expected else "FAIL"
        if status == "FAIL":
            all_pass = False
        print(f"[{status}] {case['name']}: expected={expected} actual={actual} bytes={byte_count} xml={xml_path}")
    sys.exit(0 if all_pass else 1)


if __name__ == "__main__":
    main()
