import os
import xml.etree.ElementTree as ET

NS_XHTML = "http://www.w3.org/1999/xhtml"

def qn(ns, local):
    return "{%s}%s" % (ns, local)

OUT_DIR = "build/page-scripts"
os.makedirs(OUT_DIR, exist_ok=True)

count = 0
for dirpath, dirnames, filenames in os.walk("build/sample-output"):
    for fn in filenames:
        if not fn.endswith(".xml"):
            continue
        path = os.path.join(dirpath, fn)
        rel = os.path.relpath(path, "build/sample-output")
        root = ET.parse(path).getroot()
        head = root.find(qn(NS_XHTML, "head"))
        if head is None:
            continue
        script = head.find(qn(NS_XHTML, "script"))
        if script is None or script.text is None or script.text.strip() == "":
            continue
        out_name = rel.replace(os.sep, "__").replace(".xml", ".js")
        with open(os.path.join(OUT_DIR, out_name), "w", encoding="utf-8") as f:
            f.write(script.text)
        count += 1

print("extracted", count, "page scripts")
