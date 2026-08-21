import os
import xml.etree.ElementTree as ET
import json
from collections import Counter

ROOT = "sample-phase3-project"

def local(tag):
    return tag.split("}")[-1] if "}" in tag else tag

# reason buckets requested by user
buckets = Counter()
single_child_tags = Counter()
zero_child_files = []
multi_child_details = []

def parse_num(v):
    if v is None:
        return None
    v = v.strip()
    if v == "":
        return None
    try:
        return float(v)
    except ValueError:
        return None

layout_records = []

for dirpath, dirnames, filenames in os.walk(ROOT):
    for fn in filenames:
        if not fn.endswith(".xfdl"):
            continue
        path = os.path.join(dirpath, fn)
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        for layouts in root.iter():
            if local(layouts.tag) != "Layouts":
                continue
            # first Layout child only (matches isFirstDirectLayout)
            layout_children = [c for c in list(layouts) if local(c.tag) == "Layout"]
            if not layout_children:
                continue
            layout = layout_children[0]
            children = [c for c in list(layout) if isinstance(c.tag, str)]
            rec = {
                "file": os.path.relpath(path, ROOT),
                "children_count": len(children),
                "children_tags": [local(c.tag) for c in children],
            }
            layout_records.append(rec)

            if len(children) == 0:
                buckets["no_children"] += 1
                zero_child_files.append(rec["file"])
            elif len(children) < 4:
                buckets["too_few_children(<4)"] += 1
                for c in children:
                    single_child_tags[local(c.tag)] += 1
                if len(children) >= 2:
                    multi_child_details.append(rec)
            else:
                # geometry check
                geoms = []
                ok = True
                for c in children:
                    left = parse_num(c.get("left"))
                    top = parse_num(c.get("top"))
                    width = parse_num(c.get("width"))
                    height = parse_num(c.get("height"))
                    if None in (left, top, width, height):
                        ok = False
                    geoms.append((left, top, width, height))
                if not ok:
                    buckets["geometry_missing_or_invalid"] += 1
                    continue
                tops = sorted(set(g[1] for g in geoms))
                lefts = sorted(set(g[0] for g in geoms))
                if len(tops) < 2 or len(lefts) < 2:
                    buckets["single_row_or_column"] += 1
                    continue
                # rectangular check
                by_top = {}
                for g in geoms:
                    by_top.setdefault(g[1], []).append(g[0])
                rectangular = all(sorted(v) == lefts for v in by_top.values())
                if not rectangular:
                    buckets["non_rectangular_or_incomplete_row"] += 1
                    continue
                # overlap check
                overlap = False
                for i in range(len(geoms)):
                    for j in range(i+1, len(geoms)):
                        a, b = geoms[i], geoms[j]
                        xov = a[0] < b[0]+b[2] and b[0] < a[0]+a[2]
                        yov = a[1] < b[1]+b[3] and b[1] < a[1]+a[3]
                        if xov and yov:
                            overlap = True
                if overlap:
                    buckets["overlap"] += 1
                else:
                    buckets["HIGH_CONFIDENCE"] += 1

result = {
    "total_first_layout_count": len(layout_records),
    "reason_buckets": dict(buckets),
    "single/few-child direct-tag distribution (<4 children, all tags across those layouts)": dict(single_child_tags),
    "zero_child_files_sample": zero_child_files[:10],
    "multi_child_2_or_3_details": multi_child_details,
}

with open("analysis/unresolved-layout-analysis.json", "w", encoding="utf-8") as f:
    json.dump({"summary": result, "all_records": layout_records}, f, indent=2, ensure_ascii=False)

print(json.dumps(result, indent=2, ensure_ascii=False))
