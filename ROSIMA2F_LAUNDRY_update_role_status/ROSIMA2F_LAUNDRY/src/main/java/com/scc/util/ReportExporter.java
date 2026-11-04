package com.scc.util;

import com.scc.model.StatusReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.scc.util.Constants.FILE_TS;
import static com.scc.util.Constants.now;

public class ReportExporter {

    private static List<Object[]> rows(StatusReport r) {
        return List.of(
                new Object[]{"Total Orders", r.total()},
                new Object[]{"Pending", r.pending()},
                new Object[]{"Processing", r.processing()},
                new Object[]{"Ready", r.ready()},
                new Object[]{"Completed", r.completed()},
                new Object[]{"Cancelled", r.cancelled()});
    }

    private static Path newFile(String ext) throws IOException {
        Path dir = Path.of("exports");
        Files.createDirectories(dir);
        return dir.resolve("LaundryOrderStatusReport_" + LocalDateTime.now().format(FILE_TS) + "." + ext);
    }

    public static Path exportCsv(StatusReport r) throws IOException {
        Path p = newFile("csv");
        StringBuilder sb = new StringBuilder();
        sb.append("Laundry Order Status Report\n");
        sb.append("Generated,").append(now()).append("\n\n");
        sb.append("Status,Count\n");
        for (Object[] row : rows(r)) sb.append(row[0]).append(',').append(row[1]).append('\n');
        Files.writeString(p, sb.toString(), StandardCharsets.UTF_8);
        return p;
    }

    public static Path exportXlsx(StatusReport r) throws IOException {
        Path p = newFile("xlsx");
        List<Object[]> sheet = new ArrayList<>();
        sheet.add(new Object[]{"Laundry Order Status Report"});
        sheet.add(new Object[]{"Generated", now()});
        sheet.add(new Object[]{"Status", "Count"});
        sheet.addAll(rows(r));

        StringBuilder data = new StringBuilder();
        for (int i = 0; i < sheet.size(); i++) {
            data.append("<row r=\"").append(i + 1).append("\">");
            Object[] row = sheet.get(i);
            for (int j = 0; j < row.length; j++) {
                String ref = "" + (char) ('A' + j) + (i + 1);
                if (row[j] instanceof Number) {
                    data.append("<c r=\"").append(ref).append("\"><v>").append(row[j]).append("</v></c>");
                } else {
                    data.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                            .append(xml(String.valueOf(row[j]))).append("</t></is></c>");
                }
            }
            data.append("</row>");
        }

        String head = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
        String relNs = "http://schemas.openxmlformats.org/package/2006/relationships";
        String docRel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(p))) {
            put(zip, "[Content_Types].xml", head
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "</Types>");
            put(zip, "_rels/.rels", head + "<Relationships xmlns=\"" + relNs + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + docRel + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/workbook.xml", head
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"" + docRel + "\">"
                    + "<sheets><sheet name=\"Status Report\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            put(zip, "xl/_rels/workbook.xml.rels", head + "<Relationships xmlns=\"" + relNs + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + docRel + "/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "</Relationships>");
            put(zip, "xl/worksheets/sheet1.xml", head
                    + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                    + "<sheetData>" + data + "</sheetData></worksheet>");
        }
        return p;
    }

    public static Path exportPdf(StatusReport r) throws IOException {
        Path p = newFile("pdf");
        StringBuilder s = new StringBuilder();
        s.append("BT /F1 16 Tf 72 740 Td 22 TL (Laundry Order Status Report) Tj T* ");
        s.append("/F1 11 Tf (Generated: ").append(esc(now())).append(") Tj T* T* ");
        s.append("(").append(esc(String.format("%-18s %6s", "Status", "Count"))).append(") Tj T* ");
        s.append("(").append(esc("-".repeat(25))).append(") Tj T* ");
        for (Object[] row : rows(r)) {
            s.append("(").append(esc(String.format("%-18s %6s", row[0], row[1]))).append(") Tj T* ");
        }
        s.append("ET");
        String stream = s.toString();

        List<String> objs = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
                        + "/Resources << /Font << /F1 5 0 R >> >> >>",
                "<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objs.size(); i++) {
            offsets.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n").append(objs.get(i)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objs.size() + 1).append("\n0000000000 65535 f \n");
        for (int off : offsets) pdf.append(String.format("%010d 00000 n \n", off));
        pdf.append("trailer\n<< /Size ").append(objs.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        Files.write(p, pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
        return p;
    }

    private static void put(ZipOutputStream z, String name, String content) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    private static String xml(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String esc(String t) {
        return t.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
