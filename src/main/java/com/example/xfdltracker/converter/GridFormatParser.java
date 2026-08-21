package com.example.xfdltracker.converter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * XPlatform Grid/Formats/Format 구조를 WebSquare 생성에 사용하기 쉬운 모델로 읽는다.
 * Phase 2 전용이며 DOM을 변경하지 않는다.
 */
public class GridFormatParser {

    public GridFormat parse(Element grid) {
        if (grid == null) {
            return null;
        }

        Element formats = firstDirectChild(grid, "Formats");
        if (formats == null) {
            return null;
        }

        List<Element> formatElements = directChildren(formats, "Format");
        if (formatElements.isEmpty()) {
            return null;
        }

        Element format = selectFormat(formatElements);
        GridFormat result = new GridFormat(trim(format.getAttribute("id")));
        if (formatElements.size() > 1) {
            result.warnings.add("multiple Format detected: count=" + formatElements.size()
                    + ", selected=" + (result.id.length() == 0 ? "(first)" : result.id));
        }

        parseColumns(format, result);
        parseRows(format, result);
        parseBands(format, result);

        return result;
    }

    private Element selectFormat(List<Element> formats) {
        for (Element format : formats) {
            if ("default".equalsIgnoreCase(trim(format.getAttribute("id")))) {
                return format;
            }
        }
        return formats.get(0);
    }

    private void parseColumns(Element format, GridFormat result) {
        Element columns = firstDirectChild(format, "Columns");
        if (columns == null) {
            return;
        }

        List<Element> columnElements = directChildren(columns, "Column");
        for (Element column : columnElements) {
            String size = trim(column.getAttribute("size"));
            if (size.length() == 0) {
                size = trim(column.getAttribute("width"));
            }
            result.columnWidths.add(size);
        }
    }

    private void parseRows(Element format, GridFormat result) {
        Element rows = firstDirectChild(format, "Rows");
        if (rows == null) {
            return;
        }

        List<Element> rowElements = directChildren(rows, "Row");
        for (Element row : rowElements) {
            String band = trim(row.getAttribute("band")).toLowerCase();
            String size = trim(row.getAttribute("size"));
            if (size.length() == 0) {
                size = trim(row.getAttribute("height"));
            }
            result.rows.add(new RowDef(band, size));
        }
    }

    private void parseBands(Element format, GridFormat result) {
        List<Element> bands = directChildren(format, "Band");
        for (Element band : bands) {
            String bandId = trim(band.getAttribute("id")).toLowerCase();
            List<CellDef> cells = parseCells(band, result, bandId);

            if ("head".equals(bandId) || "header".equals(bandId)) {
                result.headCells.addAll(cells);
            } else if ("body".equals(bandId)) {
                result.bodyCells.addAll(cells);
            } else if ("summ".equals(bandId) || "summary".equals(bandId) || "footer".equals(bandId)) {
                // WebSquare gridView has a real, sample-verified <w2:footer> region
                // (row/column siblings of <w2:header>/<w2:gBody>) used for a static
                // summary row; map XPlatform's "summ" band there instead of dropping it.
                result.summCells.addAll(cells);
            } else {
                result.unsupportedBands.add(bandId.length() == 0 ? "(id 없음)" : bandId);
            }
        }
    }

    private List<CellDef> parseCells(
            Element band,
            GridFormat result,
            String bandId) {

        List<Element> cellElements = directChildren(band, "Cell");
        List<CellDef> cells = new ArrayList<CellDef>();

        for (int index = 0; index < cellElements.size(); index++) {
            Element cell = cellElements.get(index);
            String rawCol = trim(cell.getAttribute("col"));
            String rawRow = trim(cell.getAttribute("row"));
            String rawColSpan = trim(cell.getAttribute("colspan"));
            String rawRowSpan = trim(cell.getAttribute("rowspan"));

            if ((rawCol.length() > 0 && !isNonNegativeInt(rawCol))
                    || (rawRow.length() > 0 && !isNonNegativeInt(rawRow))
                    || (rawColSpan.length() > 0 && !isPositiveInt(rawColSpan))
                    || (rawRowSpan.length() > 0 && !isPositiveInt(rawRowSpan))) {
                result.warnings.add(
                        "잘못된 Cell 좌표/병합값으로 Cell 건너뜀: band="
                                + safeBandId(bandId)
                                + ", index=" + index
                                + ", col=" + rawCol
                                + ", row=" + rawRow
                                + ", colspan=" + rawColSpan
                                + ", rowspan=" + rawRowSpan);
                continue;
            }

            CellDef def = new CellDef();
            def.col = rawCol.length() == 0 ? 0 : Integer.parseInt(rawCol);
            def.row = rawRow.length() == 0 ? 0 : Integer.parseInt(rawRow);
            def.colSpan = rawColSpan.length() == 0 ? 1 : Integer.parseInt(rawColSpan);
            def.rowSpan = rawRowSpan.length() == 0 ? 1 : Integer.parseInt(rawRowSpan);
            def.text = trim(cell.getAttribute("text"));
            def.displayType = trim(cell.getAttribute("displaytype"));
            def.editType = trim(cell.getAttribute("edittype"));
            def.align = trim(cell.getAttribute("align"));
            def.readOnly = "true".equalsIgnoreCase(trim(cell.getAttribute("readonly")));
            def.comboDataset = trim(cell.getAttribute("combodataset"));
            def.comboCodeColumn = trim(cell.getAttribute("combocodecol"));
            def.comboDataColumn = trim(cell.getAttribute("combodatacol"));
            if (trim(cell.getAttribute("suppress")).length() > 0) {
                result.warnings.add("Cell suppress TODO: band=" + safeBandId(bandId) + ", index=" + index);
            }
            if (def.comboDataset.length() > 0 || def.comboCodeColumn.length() > 0 || def.comboDataColumn.length() > 0) {
                result.warnings.add("Grid combo itemset TODO: band=" + safeBandId(bandId) + ", index=" + index
                        + ", dataset=" + def.comboDataset + ", code=" + def.comboCodeColumn + ", data=" + def.comboDataColumn);
            }
            cells.add(def);
        }

        return cells;
    }

    private boolean isNonNegativeInt(String raw) {
        try {
            return Integer.parseInt(raw) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPositiveInt(String raw) {
        try {
            return Integer.parseInt(raw) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String safeBandId(String bandId) {
        return trim(bandId).length() == 0 ? "(id 없음)" : bandId;
    }

    private Element firstDirectChild(Element parent, String tagName) {
        List<Element> children = directChildren(parent, tagName);
        return children.isEmpty() ? null : children.get(0);
    }

    private List<Element> directChildren(Element parent, String tagName) {
        if (parent == null) {
            return Collections.emptyList();
        }

        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if (tagName.equals(getTagName(element))) {
                result.add(element);
            }
        }
        return result;
    }

    private String getTagName(Element element) {
        String localName = element.getLocalName();
        if (localName != null && localName.length() > 0) {
            return localName;
        }
        String tagName = element.getTagName();
        int colon = tagName.indexOf(':');
        return colon >= 0 ? tagName.substring(colon + 1) : tagName;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static class GridFormat {
        private final String id;
        private final List<String> columnWidths = new ArrayList<String>();
        private final List<RowDef> rows = new ArrayList<RowDef>();
        private final List<CellDef> headCells = new ArrayList<CellDef>();
        private final List<CellDef> bodyCells = new ArrayList<CellDef>();
        private final List<CellDef> summCells = new ArrayList<CellDef>();
        private final List<String> unsupportedBands = new ArrayList<String>();
        private final List<String> warnings = new ArrayList<String>();

        GridFormat(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public List<String> getColumnWidths() {
            return Collections.unmodifiableList(columnWidths);
        }

        public List<RowDef> getRows() {
            return Collections.unmodifiableList(rows);
        }

        public List<CellDef> getHeadCells() {
            return Collections.unmodifiableList(headCells);
        }

        public List<CellDef> getBodyCells() {
            return Collections.unmodifiableList(bodyCells);
        }

        public List<CellDef> getSummCells() {
            return Collections.unmodifiableList(summCells);
        }

        public List<String> getUnsupportedBands() {
            return Collections.unmodifiableList(unsupportedBands);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
    }

    public static class RowDef {
        private final String band;
        private final String size;

        RowDef(String band, String size) {
            this.band = band;
            this.size = size;
        }

        public String getBand() {
            return band;
        }

        public String getSize() {
            return size;
        }
    }

    public static class CellDef {
        private int col;
        private int row;
        private int colSpan;
        private int rowSpan;
        private String text;
        private String displayType;
        private String editType;
        private String align;
        private boolean readOnly;
        private String comboDataset;
        private String comboCodeColumn;
        private String comboDataColumn;

        public int getCol() {
            return col;
        }

        public int getRow() {
            return row;
        }

        public int getColSpan() {
            return colSpan;
        }

        public int getRowSpan() {
            return rowSpan;
        }

        public String getText() {
            return text;
        }

        public String getDisplayType() {
            return displayType;
        }

        public String getEditType() {
            return editType;
        }

        public String getAlign() {
            return align;
        }

        public boolean isReadOnly() { return readOnly; }
        public String getComboDataset() { return comboDataset == null ? "" : comboDataset; }
        public String getComboCodeColumn() { return comboCodeColumn == null ? "" : comboCodeColumn; }
        public String getComboDataColumn() { return comboDataColumn == null ? "" : comboDataColumn; }
    }
}
