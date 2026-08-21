package com.example.xfdltracker.converter;

import com.example.xfdltracker.converter.GridFormatParser.CellDef;
import com.example.xfdltracker.converter.GridFormatParser.GridFormat;
import com.example.xfdltracker.converter.GridFormatParser.RowDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * XPlatform Grid Format을 WebSquare GridView header/gBody/row/column 구조로 변환한다.
 * Phase 2 전용 변환기이다.
 */
public class GridFormatConverter {

    private static final String NS_W2 = "http://www.inswave.com/websquare";
    private static final int MAX_SAFE_AXIS_COUNT = 10000;
    /** column width 합계가 source Grid width를 이 값(px) 이상 초과하면 horizontal-scroll
     * semantic 가능성으로 보고 percentage 변환을 하지 않는다(rounding tolerance, magic 보정
     * 상수 아님 -- 정수 px 입력 간 부동소수 오차만 흡수). */
    private static final double GRID_COLUMN_SUM_TOLERANCE_PX = 0.5;

    private final GridFormatParser parser = new GridFormatParser();
    /** GRID_COLUMN_WIDTH candidate: 기존 percentage formatter(formatPercent) 재사용 목적으로만
     * 사용한다(중복 formatter 생성 금지, 12번 규칙). */
    private final ComponentLayoutConverter percentFormatter = new ComponentLayoutConverter();

    /**
     * @return Format을 찾아 상세 변환했으면 true, 없으면 false.
     */
    public boolean convert(Document out, Element sourceGrid, Element targetGridView) {
        GridFormat format = parser.parse(sourceGrid);
        String gridId = trim(targetGridView.getAttribute("id"));

        if (format == null) {
            System.out.println("[GRID TODO] Formats/Format 없음: " + gridId);
            return false;
        }

        if (!format.getUnsupportedBands().isEmpty()) {
            System.out.println(
                    "[GRID TODO] 미지원 Band 존재: " + gridId + " -> "
                            + format.getUnsupportedBands());
        }
        for (String warning : format.getWarnings()) {
            System.out.println("[GRID TODO] " + gridId + " : " + warning);
        }
        logCellOverlaps(gridId, "head", format.getHeadCells());
        logCellOverlaps(gridId, "body", format.getBodyCells());

        String dataListId = normalizeDatasetId(sourceGrid.getAttribute("binddataset"));
        Set<String> datasetColumns = findDatasetColumns(sourceGrid, dataListId);
        if (dataListId.length() > 0 && datasetColumns.isEmpty()) {
            System.out.println(
                    "[GRID TODO] binddataset 정의를 찾지 못했거나 ColumnInfo가 비어 있음: "
                            + gridId + " -> " + dataListId);
        }

        double[] columnPercents = resolveColumnPercents(sourceGrid, format.getColumnWidths(), gridId);

        appendHeader(out, targetGridView, gridId, format, columnPercents);
        appendBody(out, targetGridView, gridId, format, datasetColumns, columnPercents);
        appendFooter(out, targetGridView, gridId, format, columnPercents);
        applyDefaultCellHeight(targetGridView, format);

        System.out.println(
                "[GRID 변환] " + gridId
                        + " format=" + safeFormatId(format.getId())
                        + ", columns=" + format.getColumnWidths().size()
                        + ", headCells=" + format.getHeadCells().size()
                        + ", bodyCells=" + format.getBodyCells().size());
        return true;
    }

    private void appendHeader(
            Document out,
            Element gridView,
            String gridId,
            GridFormat format,
            double[] columnPercents) {

        if (format.getHeadCells().isEmpty()) {
            return;
        }

        Element header = createW2(out, "w2:header");
        header.setAttribute("id", safeIdentifier(gridId + "_header"));

        List<String> rowHeights = getBandRowHeights(format, "head");
        int rowCount = calculateRowCount(format.getHeadCells(), rowHeights.size());
        int columnCount = calculateColumnCount(format.getHeadCells(), format.getColumnWidths().size());

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Element row = createW2(out, "w2:row");
            row.setAttribute("id", safeIdentifier(gridId + "_headRow_" + rowIndex));
            applyRowHeight(row, rowHeights, rowIndex, gridId, "head");

            int colIndex = 0;
            while (colIndex < columnCount) {
                CellDef cell = findCellStartingAt(format.getHeadCells(), rowIndex, colIndex);
                if (cell != null) {
                    if (isCoveredByPriorRowSpan(format.getHeadCells(), rowIndex, colIndex)) {
                        System.out.println(
                                "[GRID TODO] head Cell 좌표 중첩: "
                                        + gridId + " row=" + rowIndex + " col=" + colIndex);
                    }
                    Element column = createW2(out, "w2:column");
                    column.setAttribute(
                            "id",
                            safeIdentifier(gridId + "_head_r" + rowIndex + "_c" + cell.getCol()));
                    if (cell.getText().length() > 0) {
                        if (isExpression(cell.getText())) {
                            System.out.println("[GRID TODO] head expr 자동 변환 보류: " + gridId
                                    + " text=" + cell.getText());
                        } else {
                            column.setAttribute("value", cell.getText());
                        }
                    }
                    applyCellGeometry(column, cell, format.getColumnWidths(), gridId, "head", columnPercents);
                    applyCellPresentation(column, cell, gridId, "head");
                    row.appendChild(column);
                    colIndex += Math.max(cell.getColSpan(), 1);
                    continue;
                }

                if (isCoveredByPriorRowSpan(format.getHeadCells(), rowIndex, colIndex)) {
                    colIndex++;
                    continue;
                }

                appendPlaceholderColumn(
                        out, row, gridId, "head", rowIndex, colIndex,
                        format.getColumnWidths(), false, columnPercents);
                colIndex++;
            }
            header.appendChild(row);
        }

        gridView.appendChild(header);
    }

    private void appendBody(
            Document out,
            Element gridView,
            String gridId,
            GridFormat format,
            Set<String> datasetColumns,
            double[] columnPercents) {

        List<CellDef> bodyCells = format.getBodyCells();
        if (bodyCells.isEmpty() && datasetColumns.isEmpty()) {
            System.out.println("[GRID TODO] body Cell과 Dataset Column이 모두 없음: " + gridId);
            return;
        }

        Element gBody = createW2(out, "w2:gBody");
        gBody.setAttribute("id", safeIdentifier(gridId + "_gBody"));

        List<String> rowHeights = getBandRowHeights(format, "body");

        if (bodyCells.isEmpty()) {
            appendSynthesizedDatasetBody(
                    out, gBody, gridId, format.getColumnWidths(), rowHeights, datasetColumns,
                    columnPercents);
            gridView.appendChild(gBody);
            System.out.println(
                    "[GRID TODO] body Band가 없어 Dataset 컬럼으로 body를 보완 생성: " + gridId);
            return;
        }

        int rowCount = calculateRowCount(bodyCells, rowHeights.size());
        int columnCount = calculateColumnCount(bodyCells, format.getColumnWidths().size());
        Set<String> usedBodyIds = new LinkedHashSet<String>();

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Element row = createW2(out, "w2:row");
            row.setAttribute("id", safeIdentifier(gridId + "_bodyRow_" + rowIndex));
            applyRowHeight(row, rowHeights, rowIndex, gridId, "body");

            int colIndex = 0;
            while (colIndex < columnCount) {
                CellDef cell = findCellStartingAt(bodyCells, rowIndex, colIndex);
                if (cell != null) {
                    if (isCoveredByPriorRowSpan(bodyCells, rowIndex, colIndex)) {
                        System.out.println(
                                "[GRID TODO] body Cell 좌표 중첩: "
                                        + gridId + " row=" + rowIndex + " col=" + colIndex);
                    }
                    Element column = createW2(out, "w2:column");
                    String bindColumn = extractBindColumn(cell.getText());
                    String bodyId = createBodyColumnId(
                            gridId, rowIndex, cell.getCol(), bindColumn, usedBodyIds);
                    column.setAttribute("id", bodyId);

                    if (bindColumn.length() > 0) {
                        if (!datasetColumns.isEmpty() && !datasetColumns.contains(bindColumn)) {
                            System.out.println(
                                    "[GRID TODO] Dataset에 없는 bind 컬럼: "
                                            + gridId + " -> " + bindColumn);
                        }
                    } else if (isExpression(cell.getText())) {
                        System.out.println("[GRID TODO] body expr 자동 변환 보류: " + gridId
                                + " text=" + cell.getText());
                        column.setAttribute("readOnly", "true");
                    } else if (cell.getText().length() > 0) {
                        column.setAttribute("value", cell.getText());
                        column.setAttribute("readOnly", "true");
                    }

                    applyCellGeometry(column, cell, format.getColumnWidths(), gridId, "body", columnPercents);
                    applyCellPresentation(column, cell, gridId, "body");
                    row.appendChild(column);
                    colIndex += Math.max(cell.getColSpan(), 1);
                    continue;
                }

                if (isCoveredByPriorRowSpan(bodyCells, rowIndex, colIndex)) {
                    colIndex++;
                    continue;
                }

                appendPlaceholderColumn(
                        out, row, gridId, "body", rowIndex, colIndex,
                        format.getColumnWidths(), true, columnPercents);
                colIndex++;
            }
            gBody.appendChild(row);
        }

        gridView.appendChild(gBody);
    }

    /**
     * XPlatform's "summ" Band -> WebSquare's real <w2:footer> region (row/column siblings
     * of <w2:header>/<w2:gBody>, verified against a shipped KMS sample: inputType="text"
     * value="..." displayMode="label" per static footer cell). Static text only; XPlatform's
     * "summ" band in this project carries no aggregate/expression cells, so inputType="expression"
     * (WebSquare's real sum()/avg() footer mechanism) is intentionally not used here.
     */
    private void appendFooter(
            Document out,
            Element gridView,
            String gridId,
            GridFormat format,
            double[] columnPercents) {

        if (format.getSummCells().isEmpty()) {
            return;
        }

        Element footer = createW2(out, "w2:footer");
        footer.setAttribute("id", safeIdentifier(gridId + "_footer"));

        List<String> rowHeights = getBandRowHeights(format, "summ");
        List<CellDef> summCells = format.getSummCells();
        int rowCount = calculateRowCount(summCells, rowHeights.size());
        int columnCount = calculateColumnCount(summCells, format.getColumnWidths().size());

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Element row = createW2(out, "w2:row");
            row.setAttribute("id", safeIdentifier(gridId + "_summRow_" + rowIndex));
            applyRowHeight(row, rowHeights, rowIndex, gridId, "summ");

            int colIndex = 0;
            while (colIndex < columnCount) {
                CellDef cell = findCellStartingAt(summCells, rowIndex, colIndex);
                if (cell != null) {
                    Element column = createW2(out, "w2:column");
                    column.setAttribute(
                            "id",
                            safeIdentifier(gridId + "_summ_r" + rowIndex + "_c" + cell.getCol()));
                    if (cell.getText().length() > 0) {
                        if (isExpression(cell.getText())) {
                            System.out.println("[GRID TODO] summ expr 자동 변환 보류: " + gridId
                                    + " text=" + cell.getText());
                        } else {
                            column.setAttribute("inputType", "text");
                            column.setAttribute("displayMode", "label");
                            column.setAttribute("value", cell.getText());
                        }
                    }
                    applyCellGeometry(column, cell, format.getColumnWidths(), gridId, "summ", columnPercents);
                    row.appendChild(column);
                    colIndex += Math.max(cell.getColSpan(), 1);
                    continue;
                }
                colIndex++;
            }
            footer.appendChild(row);
        }

        gridView.appendChild(footer);
    }

    private void appendSynthesizedDatasetBody(
            Document out,
            Element gBody,
            String gridId,
            List<String> columnWidths,
            List<String> rowHeights,
            Set<String> datasetColumns,
            double[] columnPercents) {

        Element row = createW2(out, "w2:row");
        row.setAttribute("id", safeIdentifier(gridId + "_bodyRow_0"));
        applyRowHeight(row, rowHeights, 0, gridId, "body");

        int index = 0;
        for (String datasetColumn : datasetColumns) {
            Element column = createW2(out, "w2:column");
            column.setAttribute("id", datasetColumn);
            column.setAttribute("inputType", "text");
            String width = getSingleColumnWidth(columnWidths, index, gridId, columnPercents);
            if (width.length() > 0) {
                column.setAttribute("width", width);
            }
            row.appendChild(column);
            index++;
        }
        gBody.appendChild(row);
    }

    private void applyCellGeometry(
            Element target,
            CellDef cell,
            List<String> columnWidths,
            String gridId,
            String band,
            double[] columnPercents) {

        if (cell.getColSpan() > 1) {
            target.setAttribute("colSpan", String.valueOf(cell.getColSpan()));
        }
        if (cell.getRowSpan() > 1) {
            target.setAttribute("rowSpan", String.valueOf(cell.getRowSpan()));
        }

        String width = calculateCellWidth(
                columnWidths, cell.getCol(), cell.getColSpan(), gridId, band, columnPercents);
        if (width.length() > 0) {
            target.setAttribute("width", width);
        }
    }

    private void applyCellPresentation(
            Element target,
            CellDef cell,
            String gridId,
            String band) {

        String inputType = resolveInputType(cell, gridId, band);
        if (inputType.length() > 0) {
            target.setAttribute("inputType", inputType);
        }
        if ("select".equals(inputType)) {
            appendComboChoices(target, cell);
        }

        if (cell.isReadOnly() || isReadOnlyEditType(cell.getEditType())) {
            target.setAttribute("readOnly", "true");
        }

        String textAlign = mapHorizontalAlign(cell.getAlign());
        if (textAlign.length() > 0) {
            target.setAttribute("textAlign", textAlign);
        } else if (cell.getAlign().length() > 0) {
            System.out.println(
                    "[GRID TODO] align 수동 확인 필요: "
                            + gridId + " band=" + band + " align=" + cell.getAlign());
        }
    }

    /**
     * XPlatform combodataset/combocodecol/combodatacol -> WebSquare column-level
     * &lt;w2:choices&gt;&lt;w2:itemset nodeset="data:DATASET"&gt;&lt;w2:label ref="DATACOL"/&gt;
     * &lt;w2:value ref="CODECOL"/&gt;&lt;/w2:itemset&gt;&lt;/w2:choices&gt;, matching the real
     * shipped KMS sample's declarative combo-column structure (see class-level comment).
     */
    private void appendComboChoices(Element column, CellDef cell) {
        Document out = column.getOwnerDocument();
        Element choices = createW2(out, "w2:choices");
        Element itemset = createW2(out, "w2:itemset");
        itemset.setAttribute("nodeset", "data:" + cell.getComboDataset());
        Element label = createW2(out, "w2:label");
        label.setAttribute("ref", cell.getComboDataColumn());
        Element value = createW2(out, "w2:value");
        value.setAttribute("ref", cell.getComboCodeColumn());
        itemset.appendChild(label);
        itemset.appendChild(value);
        choices.appendChild(itemset);
        column.appendChild(choices);
    }

    private String resolveInputType(CellDef cell, String gridId, String band) {
        String editType = normalizeType(cell.getEditType());
        String displayType = normalizeType(cell.getDisplayType());
        boolean unsupportedEditType = false;
        if ("combo".equals(editType) || "combobox".equals(editType) || "select".equals(editType)
                || "combo".equals(displayType) || "combobox".equals(displayType) || "select".equals(displayType)) {
            if (cell.getComboDataset().length() > 0
                    && cell.getComboCodeColumn().length() > 0
                    && cell.getComboDataColumn().length() > 0) {
                // Real WebSquare gridView column combo binding, verified against a shipped
                // KMS sample: inputType="select" + <w2:choices><w2:itemset nodeset="data:X">
                // <w2:label ref="Y"/><w2:value ref="Z"/></w2:itemset></w2:choices>.
                return "select";
            }
            System.out.println("[GRID TODO] combo/select cell itemset 구조 미생성 (dataset/code/data 속성 불완전): "
                    + gridId + " band=" + band + " dataset=" + cell.getComboDataset()
                    + " code=" + cell.getComboCodeColumn() + " data=" + cell.getComboDataColumn());
            return "";
        }

        if (editType.length() > 0
                && !"none".equals(editType)
                && !"readonly".equals(editType)
                && !"normal".equals(editType)) {
            String mapped = mapInputType(editType);
            if (mapped != null) {
                logLossyTextTypeIfNeeded(gridId, band, "edittype", cell.getEditType(), editType);
                return mapped;
            }
            logUnsupportedType(gridId, band, "edittype", cell.getEditType());
            unsupportedEditType = true;
        }

        if (displayType.length() > 0 && !"normal".equals(displayType)) {
            String mapped = mapInputType(displayType);
            if (mapped != null) {
                logLossyTextTypeIfNeeded(gridId, band, "displaytype", cell.getDisplayType(), displayType);
                return mapped;
            }
            logUnsupportedType(gridId, band, "displaytype", cell.getDisplayType());
            return "";
        }

        return unsupportedEditType ? "" : "text";
    }

    private String mapInputType(String type) {
        if ("text".equals(type)
                || "normal".equals(type)
                || "number".equals(type)
                || "currency".equals(type)
                || "mask".equals(type)
                || "maskedit".equals(type)) {
            return "text";
        }
        if ("checkbox".equals(type) || "check".equals(type)) {
            return "checkbox";
        }
        if ("radio".equals(type)) {
            return "radio";
        }
        if ("button".equals(type)) {
            return "button";
        }
        if ("image".equals(type)) {
            return "image";
        }
        if ("combo".equals(type)
                || "combobox".equals(type)
                || "select".equals(type)) {
            return null;
        }
        if ("date".equals(type) || "calendar".equals(type)) {
            return "calendar";
        }
        if ("textarea".equals(type) || "multiline".equals(type)) {
            return "textarea";
        }
        return null;
    }

    private void logLossyTextTypeIfNeeded(
            String gridId, String band, String attr, String original, String normalized) {
        if ("number".equals(normalized)
                || "currency".equals(normalized)
                || "mask".equals(normalized)
                || "maskedit".equals(normalized)) {
            System.out.println(
                    "[GRID TODO] " + attr + " 상세 포맷 변환 필요: "
                            + gridId + " band=" + band + " value=" + original
                            + " (현재 inputType=text 1차 변환)");
        }
    }

    private boolean isReadOnlyEditType(String rawEditType) {
        String editType = normalizeType(rawEditType);
        return "none".equals(editType) || "readonly".equals(editType);
    }

    private String mapHorizontalAlign(String rawAlign) {
        String value = trim(rawAlign).toLowerCase();
        if (value.length() == 0) {
            return "";
        }
        String[] parts = value.split("[\\s,]+", -1);
        for (String part : parts) {
            if ("left".equals(part) || "center".equals(part) || "right".equals(part)) {
                return part;
            }
        }
        return "";
    }

    private void applyDefaultCellHeight(Element gridView, GridFormat format) {
        List<String> bodyHeights = getBandRowHeights(format, "body");
        if (bodyHeights.isEmpty()) {
            return;
        }
        String height = normalizeSize(bodyHeights.get(0));
        if (height.length() > 0) {
            gridView.setAttribute("defaultCellHeight", height);
        }
    }

    private void applyRowHeight(
            Element row,
            List<String> rowHeights,
            int rowIndex,
            String gridId,
            String band) {

        if (rowIndex >= rowHeights.size()) {
            return;
        }
        String raw = rowHeights.get(rowIndex);
        String height = normalizeSize(raw);
        if (height.length() > 0) {
            row.setAttribute("height", height);
        } else if (trim(raw).length() > 0) {
            System.out.println(
                    "[GRID TODO] Row size 수동 확인 필요: "
                            + gridId + " band=" + band + " size=" + raw);
        }
    }

    private List<String> getBandRowHeights(GridFormat format, String band) {
        List<String> result = new ArrayList<String>();
        for (RowDef row : format.getRows()) {
            String rowBand = trim(row.getBand()).toLowerCase();
            if ("head".equals(band)) {
                if ("head".equals(rowBand) || "header".equals(rowBand)) {
                    result.add(row.getSize());
                }
            } else if ("body".equals(band)) {
                if (rowBand.length() == 0 || "body".equals(rowBand)) {
                    result.add(row.getSize());
                }
            } else if ("summ".equals(band)) {
                if ("summ".equals(rowBand) || "summary".equals(rowBand) || "footer".equals(rowBand)) {
                    result.add(row.getSize());
                }
            }
        }
        return result;
    }

    private int calculateRowCount(List<CellDef> cells, int definedRowCount) {
        long max = definedRowCount;
        for (CellDef cell : cells) {
            long end = (long) cell.getRow() + (long) Math.max(cell.getRowSpan(), 1);
            max = Math.max(max, end);
        }
        return checkedAxisCount(max, "row");
    }

    private int calculateColumnCount(List<CellDef> cells, int definedColumnCount) {
        long max = definedColumnCount;
        for (CellDef cell : cells) {
            long end = (long) cell.getCol() + (long) Math.max(cell.getColSpan(), 1);
            max = Math.max(max, end);
        }
        return checkedAxisCount(max, "column");
    }

    private int checkedAxisCount(long count, String axis) {
        long normalized = Math.max(count, 1L);
        if (normalized > MAX_SAFE_AXIS_COUNT) {
            throw new IllegalStateException(
                    "Grid " + axis + " 좌표/병합 범위가 안전 한도를 초과했습니다: "
                            + normalized + " > " + MAX_SAFE_AXIS_COUNT);
        }
        return (int) normalized;
    }

    private CellDef findCellStartingAt(List<CellDef> cells, int rowIndex, int colIndex) {
        for (CellDef cell : cells) {
            if (cell.getRow() == rowIndex && cell.getCol() == colIndex) {
                return cell;
            }
        }
        return null;
    }

    private boolean isCoveredByPriorRowSpan(
            List<CellDef> cells,
            int rowIndex,
            int colIndex) {

        for (CellDef cell : cells) {
            if (cell.getRow() >= rowIndex) {
                continue;
            }
            long lastRowExclusive = (long) cell.getRow() + Math.max(cell.getRowSpan(), 1);
            long lastColExclusive = (long) cell.getCol() + Math.max(cell.getColSpan(), 1);
            if ((long) rowIndex < lastRowExclusive
                    && colIndex >= cell.getCol()
                    && (long) colIndex < lastColExclusive) {
                return true;
            }
        }
        return false;
    }

    private void logCellOverlaps(String gridId, String band, List<CellDef> cells) {
        for (int i = 0; i < cells.size(); i++) {
            CellDef a = cells.get(i);
            for (int j = i + 1; j < cells.size(); j++) {
                CellDef b = cells.get(j);
                if (rectanglesOverlap(a, b)) {
                    System.out.println(
                            "[GRID TODO] Cell 영역 중첩: " + gridId
                                    + " band=" + band
                                    + " first=(r" + a.getRow() + ",c" + a.getCol()
                                    + ",rs" + a.getRowSpan() + ",cs" + a.getColSpan() + ")"
                                    + " second=(r" + b.getRow() + ",c" + b.getCol()
                                    + ",rs" + b.getRowSpan() + ",cs" + b.getColSpan() + ")");
                }
            }
        }
    }

    private boolean rectanglesOverlap(CellDef a, CellDef b) {
        long aRowEnd = (long) a.getRow() + Math.max(a.getRowSpan(), 1);
        long aColEnd = (long) a.getCol() + Math.max(a.getColSpan(), 1);
        long bRowEnd = (long) b.getRow() + Math.max(b.getRowSpan(), 1);
        long bColEnd = (long) b.getCol() + Math.max(b.getColSpan(), 1);
        return (long) a.getRow() < bRowEnd
                && (long) b.getRow() < aRowEnd
                && (long) a.getCol() < bColEnd
                && (long) b.getCol() < aColEnd;
    }

    private void appendPlaceholderColumn(
            Document out,
            Element row,
            String gridId,
            String band,
            int rowIndex,
            int colIndex,
            List<String> columnWidths,
            boolean readOnly,
            double[] columnPercents) {

        Element placeholder = createW2(out, "w2:column");
        placeholder.setAttribute(
                "id",
                safeIdentifier(
                        gridId + "_" + band + "_blank_r" + rowIndex + "_c" + colIndex));
        placeholder.setAttribute("inputType", "text");
        if (readOnly) {
            placeholder.setAttribute("readOnly", "true");
        }
        String width = getSingleColumnWidth(columnWidths, colIndex, gridId, columnPercents);
        if (width.length() > 0) {
            placeholder.setAttribute("width", width);
        }
        row.appendChild(placeholder);
    }

    /**
     * GRID_COLUMN_WIDTH candidate(실험적, native evidence 없음 -- 폐쇄망 실검증 목적):
     * {@code columnPercents}가 있으면(NORMALIZED_TO_CONTAINER) 그 span의 percent 합을
     * 반환하고, 없으면(PIXEL_FALLBACK/UNRESOLVED) 기존 px 합산 로직을 그대로 사용한다 --
     * columnPercents가 null인 Grid는 이 함수의 동작이 기존과 완전히 동일하다(무변경 보장).
     */
    private String calculateCellWidth(
            List<String> widths,
            int startCol,
            int colSpan,
            String gridId,
            String band,
            double[] columnPercents) {

        if (startCol < 0 || startCol >= widths.size()) {
            return "";
        }

        if (columnPercents != null) {
            double sum = 0.0;
            for (int i = startCol; i < startCol + colSpan; i++) {
                if (i >= columnPercents.length) {
                    return "";
                }
                sum += columnPercents[i];
            }
            return percentFormatter.formatPercent(sum);
        }

        double sum = 0.0;
        for (int i = startCol; i < startCol + colSpan; i++) {
            if (i >= widths.size()) {
                System.out.println(
                        "[GRID TODO] Cell colspan이 Columns 범위를 초과: "
                                + gridId + " band=" + band
                                + " col=" + startCol + " colspan=" + colSpan);
                return "";
            }
            String normalized = normalizeSize(widths.get(i));
            if (normalized.length() == 0) {
                if (trim(widths.get(i)).length() > 0) {
                    System.out.println(
                            "[GRID TODO] Column size 수동 확인 필요: "
                                    + gridId + " col=" + i + " size=" + widths.get(i));
                }
                return "";
            }
            try {
                sum += Double.parseDouble(normalized);
            } catch (NumberFormatException ex) {
                return "";
            }
        }
        return formatNumber(sum);
    }

    /**
     * GRID_COLUMN_WIDTH candidate: {@link #calculateCellWidth}와 동일 원칙 --
     * {@code columnPercents}가 있으면 percent, 없으면 기존 px 로직 그대로.
     */
    private String getSingleColumnWidth(
            List<String> widths, int index, String gridId, double[] columnPercents) {
        if (index < 0 || index >= widths.size()) {
            return "";
        }
        if (columnPercents != null) {
            return index < columnPercents.length
                    ? percentFormatter.formatPercent(columnPercents[index])
                    : "";
        }
        String normalized = normalizeSize(widths.get(index));
        if (normalized.length() == 0 && trim(widths.get(index)).length() > 0) {
            System.out.println(
                    "[GRID TODO] Column size 수동 확인 필요: "
                            + gridId + " col=" + index + " size=" + widths.get(index));
        }
        return normalized;
    }

    /**
     * GRID_COLUMN_WIDTH candidate(신규 함수, 실험적 -- native v6 evidence 없음,
     * `GRID_COLUMN_NATIVE_EVIDENCE = EVIDENCE_INSUFFICIENT`, `GRID_COLUMN_WIDTH_SEMANTIC =
     * EXPERIMENTAL`). column width의 px 합계가 source Grid 자신의 declared width를 넘지 않는
     * 경우에만(즉 column sum &lt;= Grid width, horizontal-scroll 의도가 없다고 판단할 수 있는
     * 경우) 각 column을 그 Grid width 기준 percentage로 정규화한다
     * (`NORMALIZED_TO_CONTAINER`). 다음 경우 전부 null(기존 px 경로 완전 무변경,
     * `GRID_COLUMN_PIXEL_FALLBACK`/`GRID_COLUMN_UNRESOLVED` 판정용 별도 로그로 구분):
     * column 정의가 없거나(UNRESOLVED), 하나라도 숫자로 읽을 수 없거나(UNRESOLVED), source
     * Grid 자신의 width 속성이 없거나 유효하지 않거나(UNRESOLVED -- column 합계 자체를 자기
     * 기준으로 삼지 않는다, 6번 규칙), column 합계가 Grid width를 초과하면(PIXEL_FALLBACK --
     * horizontal-scroll semantic 가능성, 8번 규칙). border/scrollbar 등 임의 보정 상수는
     * 사용하지 않는다(6번 규칙) -- {@link #GRID_COLUMN_SUM_TOLERANCE_PX}는 정수 px 입력 간
     * 부동소수 오차만 흡수하는 rounding tolerance다.
     */
    private double[] resolveColumnPercents(Element sourceGrid, List<String> widths, String gridId) {
        if (widths == null || widths.isEmpty()) {
            System.out.println(
                    "[GRID COLUMN] UNRESOLVED(컬럼 정의 없음) id=" + gridId);
            return null;
        }

        double[] raw = new double[widths.size()];
        double columnSum = 0.0;
        for (int i = 0; i < widths.size(); i++) {
            String normalized = normalizeSize(widths.get(i));
            if (normalized.length() == 0) {
                System.out.println(
                        "[GRID COLUMN] UNRESOLVED(col size 읽기 불가) id=" + gridId
                                + " col=" + i + " size=" + widths.get(i));
                return null;
            }
            try {
                raw[i] = Double.parseDouble(normalized);
            } catch (NumberFormatException ex) {
                System.out.println(
                        "[GRID COLUMN] UNRESOLVED(col size 파싱 실패) id=" + gridId
                                + " col=" + i + " size=" + widths.get(i));
                return null;
            }
            columnSum += raw[i];
        }

        String gridWidthRaw = normalizeSize(sourceGrid.getAttribute("width"));
        if (gridWidthRaw.length() == 0) {
            System.out.println(
                    "[GRID COLUMN] UNRESOLVED(source Grid width 없음, column 합계를 자기 기준으로 "
                            + "쓰지 않음) id=" + gridId + " columnSum=" + formatNumber(columnSum));
            return null;
        }
        double gridWidth;
        try {
            gridWidth = Double.parseDouble(gridWidthRaw);
        } catch (NumberFormatException ex) {
            System.out.println(
                    "[GRID COLUMN] UNRESOLVED(source Grid width 파싱 실패) id=" + gridId
                            + " width=" + sourceGrid.getAttribute("width"));
            return null;
        }
        if (gridWidth <= 0.0) {
            System.out.println(
                    "[GRID COLUMN] UNRESOLVED(source Grid width<=0) id=" + gridId
                            + " width=" + gridWidthRaw);
            return null;
        }

        if (columnSum > gridWidth + GRID_COLUMN_SUM_TOLERANCE_PX) {
            System.out.println(
                    "[GRID COLUMN] PIXEL_FALLBACK(columnSum>gridWidth, horizontal-scroll 가능성) "
                            + "id=" + gridId + " gridWidth=" + formatNumber(gridWidth)
                            + " columnSum=" + formatNumber(columnSum));
            return null;
        }

        double[] percents = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            percents[i] = raw[i] / gridWidth * 100.0;
        }
        System.out.println(
                "[GRID COLUMN] NORMALIZED_TO_CONTAINER id=" + gridId
                        + " gridWidth=" + formatNumber(gridWidth)
                        + " columnSum=" + formatNumber(columnSum)
                        + " ratio=" + percentFormatter.formatPercent(columnSum / gridWidth * 100.0));
        return percents;
    }

    private String normalizeSize(String raw) {
        String value = trim(raw).toLowerCase();
        if (value.endsWith("px")) {
            value = trim(value.substring(0, value.length() - 2));
        }
        if (value.matches("[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)")) {
            try {
                double number = Double.parseDouble(value);
                return number >= 0.0 ? formatNumber(number) : "";
            } catch (NumberFormatException ignored) {
                return "";
            }
        }
        return "";
    }

    private String formatNumber(double value) {
        long asLong = (long) value;
        if (value == (double) asLong) {
            return String.valueOf(asLong);
        }
        String text = String.valueOf(value);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private boolean isExpression(String text) {
        String value = trim(text).toLowerCase();
        return value.startsWith("expr:");
    }

    private String extractBindColumn(String text) {
        String value = trim(text);
        if (!value.regionMatches(true, 0, "bind:", 0, 5)) {
            return "";
        }
        return trim(value.substring(5));
    }

    private String createBodyColumnId(
            String gridId,
            int row,
            int col,
            String bindColumn,
            Set<String> usedBodyIds) {

        if (bindColumn.length() > 0 && !usedBodyIds.contains(bindColumn)) {
            usedBodyIds.add(bindColumn);
            return bindColumn;
        }

        if (bindColumn.length() > 0) {
            System.out.println(
                    "[GRID TODO] 동일 bind 컬럼이 body에 중복됨: "
                            + gridId + " -> " + bindColumn);
        }

        String generated = safeIdentifier(gridId + "_body_r" + row + "_c" + col);
        int suffix = 2;
        String candidate = generated;
        while (usedBodyIds.contains(candidate)) {
            candidate = generated + "_" + suffix;
            suffix++;
        }
        usedBodyIds.add(candidate);
        return candidate;
    }

    private Set<String> findDatasetColumns(Element sourceGrid, String datasetId) {
        Set<String> result = new LinkedHashSet<String>();
        if (datasetId.length() == 0 || sourceGrid.getOwnerDocument() == null) {
            return result;
        }

        collectDatasetColumns(sourceGrid, datasetId, "Dataset", result);
        if (result.isEmpty()) {
            collectDatasetColumns(sourceGrid, datasetId, "DataSet", result);
        }
        return result;
    }

    private void collectDatasetColumns(
            Element sourceGrid,
            String datasetId,
            String tagName,
            Set<String> result) {

        NodeList datasets = sourceGrid.getOwnerDocument().getElementsByTagName("*");
        for (int i = 0; i < datasets.getLength(); i++) {
            Element dataset = (Element) datasets.item(i);
            if (!tagName.equals(localTagName(dataset))
                    || !datasetId.equals(trim(dataset.getAttribute("id")))) {
                continue;
            }

            NodeList columns = dataset.getElementsByTagName("*");
            for (int c = 0; c < columns.getLength(); c++) {
                Element column = (Element) columns.item(c);
                if (!"Column".equals(localTagName(column))) {
                    continue;
                }
                String id = trim(column.getAttribute("id"));
                if (id.length() > 0) {
                    result.add(id);
                }
            }
            return;
        }
    }

    private String localTagName(Element element) {
        if (element == null) {
            return "";
        }
        String local = element.getLocalName();
        if (local != null && local.length() > 0) {
            return local;
        }
        String name = element.getTagName();
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name;
    }

    private String normalizeDatasetId(String raw) {
        String value = trim(raw);
        return value.startsWith("@") ? value.substring(1) : value;
    }

    private String safeFormatId(String id) {
        return trim(id).length() == 0 ? "(첫 Format)" : id;
    }

    private String safeIdentifier(String raw) {
        String value = trim(raw);
        StringBuilder out = new StringBuilder(value.length() + 1);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean allowed = (ch >= 'A' && ch <= 'Z')
                    || (ch >= 'a' && ch <= 'z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_'
                    || ch == '$';
            out.append(allowed ? ch : '_');
        }
        if (out.length() == 0) {
            return "gridColumn";
        }
        char first = out.charAt(0);
        if (first >= '0' && first <= '9') {
            out.insert(0, '_');
        }
        return out.toString();
    }

    private String normalizeType(String raw) {
        return trim(raw).toLowerCase().replace("_", "").replace("-", "");
    }

    private void logUnsupportedType(String gridId, String band, String attr, String value) {
        System.out.println(
                "[GRID TODO] 미지원 " + attr + ": "
                        + gridId + " band=" + band + " value=" + value
                        + " (임의 inputType 변환 안 함)");
    }

    private Element createW2(Document out, String qualifiedName) {
        return out.createElementNS(NS_W2, qualifiedName);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
