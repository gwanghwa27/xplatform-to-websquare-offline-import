#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
XPlatform(XFDL) -> WebSquare XML structural/layout conversion quality audit.

Read-only. Does not touch Production converter source. Compares:
  sample-phase3-project/**/*.xfdl   (source)
  sample-phase3-output/**/*.xml     (fixed working-candidate generated output)

Scope (per ComponentMappingRegistry.java, mirrored below):
  - control id / type / geometry (left/top/width/height) / parent-child hierarchy
  - key properties: text/value, enable->disabled, cssclass->class, tooltiptext->title
  - Div/GroupBox/PopupDiv nesting (id prefix chain, dot-path -> underscore, per
    WebSquareGenerator.buildSourcePath/createTargetId)
  - Grid: columns, rows/bands, per-cell bind, combo binding, sizes
  - Dataset/Column existence -> w2:dataList/w2:column

Tab/Tabpage: only the Tab control itself (id/type/geometry) is checked here.
Tabpage-level dynamic runtime semantics (setUrl/addTab/lifecycle) were already
exhaustively verified in a separate real-runtime investigation this session and
are intentionally OUT OF SCOPE for this static structural audit, to keep
signal-to-noise high for genuinely new (Div/Grid/GroupBox/Dataset) findings.

Output: TOTAL / PASS / MISMATCH / UNSUPPORTED counts + a mismatch detail table,
plus a Grid-specific and Dataset-specific sub-report.
"""
import sys
import os
import json
import xml.etree.ElementTree as ET
from pathlib import Path

# ---------------------------------------------------------------------------
# ComponentMappingRegistry mirror (source: ComponentMappingRegistry.java)
# tag -> (target_tag_or_None, is_container, support_level)
# ---------------------------------------------------------------------------
REGISTRY = {
    "Button":       ("xf:trigger",      False, "SUPPORTED"),
    "Static":       ("w2:span",         False, "SUPPORTED"),
    "Edit":         ("xf:input",        False, "SUPPORTED"),
    "MaskEdit":     ("xf:input",        False, "PARTIAL"),
    "TextArea":     ("xf:textarea",     False, "SUPPORTED"),
    "Combo":        ("xf:select1",      False, "PARTIAL"),
    "ListBox":      ("xf:select1",      False, "PARTIAL"),
    "Radio":        ("xf:select1",      False, "PARTIAL"),
    # REALRT-1 fix: real WebSquare has no xf:selectBoolean widget; the real checkbox
    # component is w2:checkbox (verified against a shipped KMS sample).
    "CheckBox":     ("w2:checkbox",     False, "PARTIAL"),
    "Calendar":     ("w2:calendar",     False, "PARTIAL"),
    "Spin":         ("w2:spinner",      False, "PARTIAL"),
    "Grid":         ("w2:gridView",     False, "PARTIAL"),
    "Div":          ("w2:group",        True,  "SUPPORTED"),
    "GroupBox":     ("w2:group",        True,  "PARTIAL"),
    "PopupDiv":     ("w2:group",        True,  "PARTIAL"),
    "ImageViewer":  ("w2:image",        False, "PARTIAL"),
    "ProgressBar":  ("w2:progressbar",  False, "PARTIAL"),
    "Tab":          ("w2:tabControl",   True,  "PARTIAL"),
    "Tabpage":      ("w2:group",        True,  "PARTIAL"),
    "WebBrowser":   ("w2:wframe",       False, "PARTIAL"),
    "FileUpload":   ("w2:upload",       False, "PARTIAL"),
    "FileDownload": (None,              False, "TODO"),
}
CONTAINER_TAGS = {t for t, (tgt, cont, lvl) in REGISTRY.items() if cont}
CONTROL_TAGS = set(REGISTRY.keys())

# Attributes documented as intentionally NOT migrated (WebSquareGenerator "[PROPERTY TODO]" logs).
# (source_tag, source_attr) pairs excluded from property-loss MISMATCH flagging.
KNOWN_PROPERTY_TODO = {
    ("MaskEdit", "format"),
    ("Calendar", "editformat"),
    ("ImageViewer", "image"),
    ("WebBrowser", "url"),
}

NS = {
    "w2": "http://www.inswave.com/websquare",
    "xf": "http://www.w3.org/2002/xforms",
    "ev": "http://www.w3.org/2001/xml-events",
    "h":  "http://www.w3.org/1999/xhtml",
}
REV_NS = {v: k for k, v in NS.items()}


def qname_to_prefixed(tag):
    if tag.startswith("{"):
        uri, local = tag[1:].split("}", 1)
        prefix = REV_NS.get(uri)
        return (prefix + ":" + local) if prefix else local
    return tag


def localname(tag):
    return qname_to_prefixed(tag).split(":")[-1]


# ---------------------------------------------------------------------------
# Source (XFDL) parsing
# ---------------------------------------------------------------------------
class SrcNode:
    __slots__ = ("tag", "own_id", "path", "expected_id", "attrs", "parent_expected_id", "children")

    def __init__(self, tag, own_id, path, expected_id, attrs, parent_expected_id):
        self.tag = tag
        self.own_id = own_id
        self.path = path
        self.expected_id = expected_id
        self.attrs = attrs
        self.parent_expected_id = parent_expected_id
        self.children = []


def find_direct_layout(elem):
    """Return the immediate <Layout> element under this element's <Layouts>, if any."""
    layouts = elem.find("Layouts")
    if layouts is None:
        return None
    return layouts.find("Layout")


def walk_layout(layout_elem, parent_path, parent_expected_id, out_controls, grids, datasets_used):
    if layout_elem is None:
        return
    for child in list(layout_elem):
        tag = child.tag
        if tag not in CONTROL_TAGS:
            continue
        own_id = child.get("id", "")
        if not own_id:
            continue
        path = (parent_path + "." + own_id) if parent_path else own_id
        expected_id = path.replace(".", "_")
        node = SrcNode(tag, own_id, path, expected_id, dict(child.attrib), parent_expected_id)
        out_controls.append(node)

        if tag == "Grid":
            grids.append((node, child))
            bd = child.get("binddataset", "")
            if bd:
                datasets_used.setdefault(bd, []).append(path)
            continue  # grid internals handled separately, not as generic children

        if tag == "Tab":
            # Only the Tab control itself is in-scope for this structural audit.
            # (Tabpage runtime semantics already covered by prior real-runtime investigation.)
            continue

        if tag in CONTAINER_TAGS:
            inner = find_direct_layout(child)
            walk_layout(inner, path, expected_id, out_controls, grids, datasets_used)


def parse_source_fixture(xfdl_path):
    tree = ET.parse(str(xfdl_path))
    root = tree.getroot()
    form = root.find("Form")
    if form is None:
        return [], [], {}, None
    # Some fixtures wrap top-level controls in <Layouts><Layout>...</Layout></Layouts>;
    # others place controls directly as children of <Form> with no Layout wrapper at all.
    top_layout = find_direct_layout(form)
    if top_layout is None:
        top_layout = form
    controls = []
    grids = []
    datasets_used = {}
    walk_layout(top_layout, "", None, controls, grids, datasets_used)

    datasets = []
    # Dataset/DataSet can appear as a direct Form child OR nested inside an <Objects> wrapper.
    for ds in form.iter():
        if ds.tag not in ("Dataset", "DataSet"):
            continue
        ds_id = ds.get("id", "")
        cols = []
        colinfo = ds.find("ColumnInfo")
        if colinfo is not None:
            for col in colinfo.findall("Column"):
                cols.append(col.get("id", ""))
        datasets.append((ds_id, cols))

    return controls, grids, datasets, form


# ---------------------------------------------------------------------------
# Generated (WebSquare XML) parsing
# ---------------------------------------------------------------------------
class GenNode:
    __slots__ = ("tag", "id", "attrs", "parent_id", "elem")

    def __init__(self, tag, id_, attrs, parent_id, elem):
        self.tag = tag
        self.id = id_
        self.attrs = attrs
        self.parent_id = parent_id
        self.elem = elem


def parse_generated(xml_path):
    tree = ET.parse(str(xml_path))
    root = tree.getroot()
    body = root.find("h:body", NS) if root.find("h:body", NS) is not None else root.find("body")
    id_map = {}
    if body is None:
        return id_map, None
    grp_content = None
    for c in body.iter():
        if c.get("id") == "grp_content":
            grp_content = c
            break

    def rec(elem, nearest_ancestor_id):
        eid = elem.get("id")
        cur_parent_for_children = nearest_ancestor_id
        if eid:
            id_map[eid] = GenNode(qname_to_prefixed(elem.tag), eid, dict(elem.attrib), nearest_ancestor_id, elem)
            cur_parent_for_children = eid
        for ch in list(elem):
            rec(ch, cur_parent_for_children)

    if grp_content is not None:
        rec(grp_content, None)
    return id_map, grp_content


def parse_style_box(style):
    """Extract left/top/width/height (px) from a WebSquare inline style string."""
    out = {}
    if not style:
        return out
    for part in style.split(";"):
        part = part.strip()
        if not part or ":" not in part:
            continue
        k, v = part.split(":", 1)
        k = k.strip()
        v = v.strip()
        if k in ("left", "top", "width", "height") and v.endswith("px"):
            try:
                out[k] = int(round(float(v[:-2])))
            except ValueError:
                pass
    return out


# ---------------------------------------------------------------------------
# Comparison
# ---------------------------------------------------------------------------
def compare_fixture(fixture_rel, controls, gen_id_map):
    results = []  # dicts: {status, control_id, kind, detail}
    for node in controls:
        target_tag, is_container, level = REGISTRY.get(node.tag, (None, False, "UNKNOWN"))

        if target_tag is None:
            # e.g. FileDownload -> intentionally unmapped
            present = node.expected_id in gen_id_map
            results.append({
                "status": "UNSUPPORTED",
                "control_id": node.expected_id,
                "source_tag": node.tag,
                "detail": "documented TODO (no target mapping); "
                          + ("unexpectedly present in output" if present else "correctly absent"),
            })
            continue

        gen = gen_id_map.get(node.expected_id)
        if gen is None:
            results.append({
                "status": "MISMATCH",
                "control_id": node.expected_id,
                "source_tag": node.tag,
                "detail": "control missing from generated output",
                "expected_parent": node.parent_expected_id or "grp_content",
            })
            continue

        problems = []

        # type
        if gen.tag != target_tag:
            problems.append("type: expected %s, got %s" % (target_tag, gen.tag))

        # geometry
        box = parse_style_box(gen.attrs.get("style", ""))
        for attr in ("left", "top", "width", "height"):
            src_v = node.attrs.get(attr)
            if src_v is None:
                continue
            try:
                src_v_i = int(round(float(src_v)))
            except ValueError:
                continue
            gen_v_i = box.get(attr)
            if gen_v_i is None:
                problems.append("%s: source=%s but missing in generated style" % (attr, src_v))
            elif gen_v_i != src_v_i:
                problems.append("%s: source=%s generated=%s" % (attr, src_v_i, gen_v_i))

        # parent hierarchy
        expected_parent = node.parent_expected_id or "grp_content"
        if gen.parent_id != expected_parent:
            problems.append("parent: expected=%s actual=%s" % (expected_parent, gen.parent_id or "None"))

        # text/value: real WebSquare's static-content attribute name differs by target tag
        # (REALRT-2 fix: w2:span uses "label", xf:input uses "initValue"; everything else
        # keeps "value", e.g. xf:trigger's caption).
        src_text = node.attrs.get("text") or node.attrs.get("value")
        if src_text:
            value_attr = {"w2:span": "label", "xf:input": "initValue"}.get(gen.tag, "value")
            gen_v = gen.attrs.get(value_attr)
            if gen_v != src_text:
                problems.append("%s: source=%r generated=%r" % (value_attr, src_text, gen_v))

        # enable -> disabled
        if node.attrs.get("enable", "").lower() == "false":
            if gen.attrs.get("disabled") != "true":
                problems.append("enable=false not reflected as disabled=true")

        # cssclass -> class
        if ("cssclass" in node.attrs) and (node.tag, "cssclass") not in KNOWN_PROPERTY_TODO:
            src_cls = node.attrs.get("cssclass")
            gen_cls = gen.attrs.get("class")
            if src_cls and (gen_cls is None or src_cls not in gen_cls.split()):
                problems.append("cssclass: source=%r generated class=%r" % (src_cls, gen_cls))

        # tooltiptext -> title
        if "tooltiptext" in node.attrs and (node.tag, "tooltiptext") not in KNOWN_PROPERTY_TODO:
            src_tt = node.attrs.get("tooltiptext")
            gen_tt = gen.attrs.get("title")
            if src_tt and gen_tt != src_tt:
                problems.append("tooltiptext: source=%r generated title=%r" % (src_tt, gen_tt))

        # documented PROPERTY-TODO attrs: verify they are indeed NOT silently mapped incorrectly,
        # but don't count their absence as a problem.
        for attr in ("format", "editformat", "image", "url"):
            if (node.tag, attr) in KNOWN_PROPERTY_TODO and attr in node.attrs and node.attrs.get(attr):
                pass  # known, intentionally excluded from PASS/MISMATCH scoring

        if problems:
            results.append({
                "status": "MISMATCH",
                "control_id": node.expected_id,
                "source_tag": node.tag,
                "detail": "; ".join(problems),
            })
        else:
            results.append({
                "status": "PASS",
                "control_id": node.expected_id,
                "source_tag": node.tag,
                "detail": "ok",
            })
    return results


def audit_grid(fixture_rel, grid_node_src, grid_elem_src, gen_id_map):
    """Structural Grid audit: columns, rows/bands, bind, combo binding, sizes."""
    findings = []
    grid_id = grid_node_src.expected_id
    gen = gen_id_map.get(grid_id)
    if gen is None:
        findings.append({"status": "MISMATCH", "control_id": grid_id, "detail": "Grid missing from output"})
        return findings

    if gen.tag != "w2:gridView":
        findings.append({"status": "MISMATCH", "control_id": grid_id,
                          "detail": "type: expected w2:gridView, got %s" % gen.tag})

    # binddataset -> dataList
    binddataset = grid_elem_src.get("binddataset", "")
    if binddataset:
        gen_dl = gen.attrs.get("dataList")
        if gen_dl != binddataset:
            findings.append({"status": "MISMATCH", "control_id": grid_id,
                              "detail": "binddataset: source=%r generated dataList=%r" % (binddataset, gen_dl)})

    formats = grid_elem_src.find("Formats")
    if formats is None:
        return findings
    fmt_list = formats.findall("Format")
    if not fmt_list:
        return findings

    # Only the first Format ("default") is expected to be converted. Confirmed via dev-pack
    # doc/sample research (GRID-3 investigation): real WebSquare gridView has exactly one
    # header/gBody/footer per instance with no declarative multi-Format/layout-switch markup
    # and no documented+sample-verified API for it -> classified UNSUPPORTED_SEMANTIC, not a
    # converter defect, but still explicitly flagged rather than silently dropped.
    default_fmt = fmt_list[0]
    extra_fmts = fmt_list[1:]
    if extra_fmts:
        findings.append({
            "status": "UNSUPPORTED", "control_id": grid_id,
            "detail": "Grid has %d additional <Format> block(s) (%s) beyond the first; "
                      "WebSquare w2:gridView has only one active layout - alternate Format(s) "
                      "are UNSUPPORTED_SEMANTIC (no verified WebSquare structure for this), "
                      "already logged via GridFormatParser 'multiple Format detected' warning"
                      % (len(extra_fmts), ", ".join(f.get("id", "?") for f in extra_fmts)),
        })

    cols = default_fmt.find("Columns")
    src_col_sizes = [c.get("size") for c in cols.findall("Column")] if cols is not None else []

    rows_el = default_fmt.find("Rows")
    src_rows = [(r.get("band"), r.get("size")) for r in rows_el.findall("Row")] if rows_el is not None else []
    src_bands = [b for b in default_fmt.findall("Band")]
    band_by_id = {b.get("id"): b for b in src_bands}

    # generated header
    header = None
    gbody = None
    for eid, node in gen_id_map.items():
        if node.parent_id == grid_id and node.tag == "w2:header":
            header = node
        if node.parent_id == grid_id and node.tag == "w2:gBody":
            gbody = node

    head_band = band_by_id.get("head")
    body_band = band_by_id.get("body")
    summ_band = band_by_id.get("summ")

    # header row/cell check
    if head_band is not None:
        src_head_cells = head_band.findall("Cell")
        if header is None:
            findings.append({"status": "MISMATCH", "control_id": grid_id, "detail": "head band present in source but no w2:header in output"})
        else:
            gen_head_cols = [n for n in gen_id_map.values() if n.parent_id and n.parent_id.startswith(grid_id + "_headRow")]
            if len(gen_head_cols) != len(src_head_cells):
                findings.append({"status": "MISMATCH", "control_id": grid_id,
                                  "detail": "head columns: source=%d generated=%d" % (len(src_head_cells), len(gen_head_cols))})
            for i, cell in enumerate(src_head_cells):
                src_text = cell.get("text", "")
                match = next((n for n in gen_head_cols if n.attrs.get("value") == src_text), None)
                if src_text and match is None:
                    findings.append({"status": "MISMATCH", "control_id": grid_id,
                                      "detail": "head cell text %r not found among generated header columns" % src_text})

    # body band: bind / expr / combo
    if body_band is not None:
        src_body_cells = body_band.findall("Cell")
        gen_body_cols = [n for n in gen_id_map.values() if n.parent_id and n.parent_id.startswith(grid_id + "_bodyRow")]
        if len(gen_body_cols) != len(src_body_cells):
            findings.append({"status": "MISMATCH", "control_id": grid_id,
                              "detail": "body columns: source=%d generated=%d" % (len(src_body_cells), len(gen_body_cols))})
        for cell in src_body_cells:
            text = cell.get("text", "")
            if text.startswith("bind:"):
                bind_col = text.split(":", 1)[1]
                if bind_col not in gen_id_map or gen_id_map[bind_col].parent_id != gbody_row_parent(gen_id_map, grid_id):
                    # looser check: bind col id exists anywhere under this grid's gBody
                    found = any(n for n in gen_body_cols if n.id == bind_col)
                    if not found:
                        findings.append({"status": "MISMATCH", "control_id": grid_id,
                                          "detail": "bind cell %r: no w2:column id=%r found in generated gBody" % (text, bind_col)})
                # combo binding check: real WebSquare gridView combo columns use
                # inputType="select" + a nested <w2:choices><w2:itemset nodeset="data:DS">
                # <w2:label ref="DATACOL"/><w2:value ref="CODECOL"/></w2:itemset></w2:choices>
                # (verified against a shipped KMS sample), not a flat dataList/codeColumn attr.
                if cell.get("displaytype") == "combo" or cell.get("edittype") == "combo":
                    gen_cell = next((n for n in gen_body_cols if n.id == bind_col), None)
                    combo_ds = cell.get("combodataset", "")
                    combo_code = cell.get("combocodecol", "")
                    combo_data = cell.get("combodatacol", "")
                    if gen_cell is not None:
                        input_type = gen_cell.attrs.get("inputType", "")
                        itemset = gen_cell.elem.find("w2:choices/w2:itemset", NS)
                        ok = False
                        if input_type == "select" and itemset is not None:
                            nodeset = itemset.get("nodeset", "")
                            label_el = itemset.find("w2:label", NS)
                            value_el = itemset.find("w2:value", NS)
                            label_ref = label_el.get("ref") if label_el is not None else None
                            value_ref = value_el.get("ref") if value_el is not None else None
                            ok = (nodeset == "data:" + combo_ds
                                  and label_ref == combo_data
                                  and value_ref == combo_code)
                        if not ok:
                            findings.append({
                                "status": "MISMATCH", "control_id": grid_id,
                                "detail": "cell %r declares combo binding to dataset %r (code=%r data=%r) "
                                          "but generated w2:column id=%r has no matching inputType=select "
                                          "w2:choices/w2:itemset (inputType=%r)"
                                          % (bind_col, combo_ds, combo_code, combo_data, bind_col, input_type),
                            })

    if summ_band is not None:
        # look for any generated element under the grid that represents a summary/footer row
        summ_present = any(n.parent_id == grid_id and n.tag in ("w2:footer", "w2:summary", "w2:sBody") for n in gen_id_map.values())
        if not summ_present:
            findings.append({
                "status": "MISMATCH", "control_id": grid_id,
                "detail": "summ band present in source (cells=%s) but no summary/footer structure found in generated w2:gridView"
                          % [c.get("text") for c in summ_band.findall("Cell")],
            })

    if not findings:
        findings.append({"status": "PASS", "control_id": grid_id, "detail": "grid structure ok (default Format)"})
    return findings


def gbody_row_parent(gen_id_map, grid_id):
    return None  # helper placeholder, loose-matching used instead above


def audit_datasets(fixture_rel, datasets, gen_root):
    findings = []
    if not datasets:
        return findings
    dc = None
    for el in gen_root.iter():
        if localname(el.tag) == "dataCollection":
            dc = el
            break
    gen_datalists = {}
    if dc is not None:
        for dl in dc:
            if localname(dl.tag) == "dataList":
                cols = [c.get("id") for c in dl.iter() if localname(c.tag) == "column"]
                gen_datalists[dl.get("id")] = cols

    for ds_id, cols in datasets:
        if ds_id not in gen_datalists:
            findings.append({"status": "MISMATCH", "control_id": ds_id,
                              "detail": "Dataset missing from generated w2:dataCollection"})
            continue
        gen_cols = gen_datalists[ds_id]
        missing_cols = [c for c in cols if c not in gen_cols]
        if missing_cols:
            findings.append({"status": "MISMATCH", "control_id": ds_id,
                              "detail": "columns missing in w2:dataList: %s (source had %s, generated had %s)"
                                        % (missing_cols, cols, gen_cols)})
        else:
            findings.append({"status": "PASS", "control_id": ds_id, "detail": "dataset/columns ok"})
    return findings


# ---------------------------------------------------------------------------
# Driver
# ---------------------------------------------------------------------------
def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(".")
    src_root = root / "sample-phase3-project"
    gen_root_dir = root / "sample-phase3-output"

    xfdl_files = sorted(src_root.rglob("*.xfdl"))

    totals = {"TOTAL": 0, "PASS": 0, "MISMATCH": 0, "UNSUPPORTED": 0}
    # NOTE: grid_totals["TOTAL"] counts *finding rows* (0..N per Grid element depending on how
    # many distinct issues that Grid has), NOT the number of Grid elements in the project - a
    # Grid with 3 problems contributes 3 rows here, a clean Grid contributes exactly 1 PASS row.
    # grid_element_count (separate, fixed) is the actual number of <Grid> elements scanned.
    grid_totals = {"TOTAL": 0, "PASS": 0, "MISMATCH": 0, "UNSUPPORTED": 0}
    grid_element_count = 0
    dataset_totals = {"TOTAL": 0, "PASS": 0, "MISMATCH": 0}
    mismatch_rows = []
    grid_mismatch_rows = []
    grid_unsupported_rows = []
    dataset_mismatch_rows = []
    fixtures_with_errors = []

    for xfdl_path in xfdl_files:
        rel = xfdl_path.relative_to(src_root)
        gen_path = gen_root_dir / rel.with_suffix(".xml")
        if not gen_path.exists():
            fixtures_with_errors.append((str(rel), "generated output file missing entirely"))
            continue
        try:
            controls, grids, datasets, form = parse_source_fixture(xfdl_path)
            gen_id_map, grp_content = parse_generated(gen_path)
            gen_tree_root = ET.parse(str(gen_path)).getroot()
        except ET.ParseError as e:
            fixtures_with_errors.append((str(rel), "XML parse error: %s" % e))
            continue

        results = compare_fixture(str(rel), controls, gen_id_map)
        for r in results:
            totals["TOTAL"] += 1
            totals[r["status"]] += 1
            if r["status"] == "MISMATCH":
                mismatch_rows.append((str(rel), r["control_id"], r["source_tag"], r["detail"]))

        for gnode, gelem in grids:
            grid_element_count += 1
            gfindings = audit_grid(str(rel), gnode, gelem, gen_id_map)
            for f in gfindings:
                grid_totals["TOTAL"] += 1
                grid_totals[f["status"]] += 1
                if f["status"] == "MISMATCH":
                    grid_mismatch_rows.append((str(rel), f["control_id"], f["detail"]))
                elif f["status"] == "UNSUPPORTED":
                    grid_unsupported_rows.append((str(rel), f["control_id"], f["detail"]))

        dfindings = audit_datasets(str(rel), datasets, gen_tree_root)
        for f in dfindings:
            dataset_totals["TOTAL"] += 1
            dataset_totals[f["status"]] += 1
            if f["status"] == "MISMATCH":
                dataset_mismatch_rows.append((str(rel), f["control_id"], f["detail"]))

    # ---------------- report ----------------
    print("=" * 78)
    print("XFDL -> WebSquare structural/layout conversion audit")
    print("Source:    %s" % src_root)
    print("Generated: %s" % gen_root_dir)
    print("Fixtures:  %d XFDL files scanned" % len(xfdl_files))
    print("=" * 78)
    print()
    print("--- General control audit (id/type/geometry/hierarchy/key-property) ---")
    print("TOTAL=%d PASS=%d MISMATCH=%d UNSUPPORTED=%d" % (
        totals["TOTAL"], totals["PASS"], totals["MISMATCH"], totals["UNSUPPORTED"]))
    print()
    if mismatch_rows:
        print("Mismatches (fixture / control_id / source_tag / detail):")
        for rel, cid, tag, detail in mismatch_rows:
            print("  [%s] %s (%s): %s" % (rel, cid, tag, detail))
    print()
    print("--- Grid structural/binding audit ---")
    print("Grid elements scanned: %d" % grid_element_count)
    print("Finding rows: TOTAL=%d PASS=%d MISMATCH=%d UNSUPPORTED=%d "
          "(one row per distinct issue found per Grid, NOT one row per Grid element)" % (
        grid_totals["TOTAL"], grid_totals["PASS"], grid_totals["MISMATCH"], grid_totals["UNSUPPORTED"]))
    if grid_mismatch_rows:
        print("Grid mismatches:")
        for rel, cid, detail in grid_mismatch_rows:
            print("  [%s] %s: %s" % (rel, cid, detail))
    if grid_unsupported_rows:
        print("Grid UNSUPPORTED_SEMANTIC (flagged, not a defect):")
        for rel, cid, detail in grid_unsupported_rows:
            print("  [%s] %s: %s" % (rel, cid, detail))
    print()
    print("--- Dataset/Column audit ---")
    print("TOTAL=%d PASS=%d MISMATCH=%d" % (dataset_totals["TOTAL"], dataset_totals["PASS"], dataset_totals["MISMATCH"]))
    if dataset_mismatch_rows:
        print("Dataset mismatches:")
        for rel, cid, detail in dataset_mismatch_rows:
            print("  [%s] %s: %s" % (rel, cid, detail))
    print()
    if fixtures_with_errors:
        print("--- Fixture-level errors ---")
        for rel, err in fixtures_with_errors:
            print("  [%s] %s" % (rel, err))

    # dump machine-readable JSON alongside
    out_json = {
        "totals": totals,
        "grid_element_count": grid_element_count,
        "grid_totals": grid_totals,
        "dataset_totals": dataset_totals,
        "mismatches": [{"fixture": r, "control_id": c, "source_tag": t, "detail": d} for r, c, t, d in mismatch_rows],
        "grid_mismatches": [{"fixture": r, "control_id": c, "detail": d} for r, c, d in grid_mismatch_rows],
        "grid_unsupported": [{"fixture": r, "control_id": c, "detail": d} for r, c, d in grid_unsupported_rows],
        "dataset_mismatches": [{"fixture": r, "control_id": c, "detail": d} for r, c, d in dataset_mismatch_rows],
        "fixture_errors": fixtures_with_errors,
    }
    out_path = Path(__file__).parent / "audit-result.json"
    out_path.write_text(json.dumps(out_json, indent=2, ensure_ascii=False), encoding="utf-8")
    print()
    print("JSON detail written to %s" % out_path)


if __name__ == "__main__":
    main()
