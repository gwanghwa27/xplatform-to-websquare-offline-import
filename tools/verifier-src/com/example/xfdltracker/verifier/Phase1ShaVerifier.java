package com.example.xfdltracker.verifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Java 8, dependency-free reimplementation of audit/phase1_sha_verifier.py, for offline sites
 * where only a JDK (no Python) is available. Not part of the Production converter — this is a
 * standalone offline verification tool.
 *
 * Recipe (must stay identical to the Python verifier and to the documented provenance):
 *   1. Take the GENERATED WebSquare output XML (not the source .xfdl).
 *   2. Parse with an XML parser; collect the text content of every &lt;script&gt; element
 *      (any namespace prefix; matched by local name "script"), in document order.
 *   3. Join the per-element text with a single "\n" separator.
 *   4. Strip trailing CR/LF, then append exactly one trailing "\n".
 *   5. Encode as UTF-8, no BOM.
 *   6. SHA-256 the resulting bytes.
 *
 * Manifest format (JSON), minimal hand-rolled parser to avoid any external JSON library:
 *   {"cases": [{"name": "...", "xml": "relative/or/absolute/path.xml", "expected": "hexhash"}, ...]}
 *
 * "xml" paths are resolved relative to the manifest file's own directory when not absolute,
 * matching the Python verifier's behavior so results do not depend on the working directory.
 */
public final class Phase1ShaVerifier {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: java Phase1ShaVerifier <manifest.json>");
            System.exit(2);
            return;
        }
        File manifestFile = new File(args[0]).getAbsoluteFile();
        File manifestDir = manifestFile.getParentFile();
        String manifestJson = readFile(manifestFile);

        List<Case> cases = parseCases(manifestJson);
        boolean allPass = true;
        for (Case c : cases) {
            File xmlFile = new File(c.xml);
            if (!xmlFile.isAbsolute()) {
                xmlFile = new File(manifestDir, c.xml);
            }
            String actual = extractScriptHash(xmlFile);
            boolean pass = actual.equalsIgnoreCase(c.expected);
            if (!pass) allPass = false;
            System.out.println("[" + (pass ? "PASS" : "FAIL") + "] " + c.name
                    + ": expected=" + c.expected + " actual=" + actual
                    + " xml=" + xmlFile.getPath());
        }
        System.exit(allPass ? 0 : 1);
    }

    private static String extractScriptHash(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        try (InputStream in = new FileInputStream(xmlFile)) {
            doc = builder.parse(in);
        }

        List<String> parts = new ArrayList<String>();
        collectScriptText(doc, parts);

        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) joined.append('\n');
            joined.append(parts.get(i));
        }
        String content = joined.toString();
        while (content.endsWith("\r") || content.endsWith("\n")) {
            content = content.substring(0, content.length() - 1);
        }
        content = content + "\n";

        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private static void collectScriptText(Node node, List<String> parts) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String localName = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                if ("script".equalsIgnoreCase(localName)) {
                    parts.add(textContent(child));
                }
                collectScriptText(child, parts);
            }
        }
    }

    private static String textContent(Node node) {
        StringBuilder sb = new StringBuilder();
        appendText(node, sb);
        return sb.toString();
    }

    private static void appendText(Node node, StringBuilder sb) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            short type = child.getNodeType();
            if (type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            } else if (type == Node.ELEMENT_NODE) {
                appendText(child, sb);
            }
        }
    }

    private static String readFile(File f) throws Exception {
        byte[] bytes;
        try (FileInputStream fis = new FileInputStream(f)) {
            bytes = new byte[(int) f.length()];
            int off = 0;
            int n;
            while (off < bytes.length && (n = fis.read(bytes, off, bytes.length - off)) >= 0) {
                off += n;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Minimal hand-rolled JSON reader for exactly the manifest shape used here -- no external library. */
    private static List<Case> parseCases(String json) {
        List<Case> result = new ArrayList<Case>();
        int idx = 0;
        while (true) {
            int nameIdx = json.indexOf("\"name\"", idx);
            if (nameIdx < 0) break;
            String name = extractStringValue(json, nameIdx);
            int xmlIdx = json.indexOf("\"xml\"", nameIdx);
            String xml = extractStringValue(json, xmlIdx);
            int expIdx = json.indexOf("\"expected\"", xmlIdx);
            String expected = extractStringValue(json, expIdx);
            Case c = new Case();
            c.name = name;
            c.xml = xml;
            c.expected = expected;
            result.add(c);
            idx = expIdx + 1;
        }
        return result;
    }

    private static String extractStringValue(String json, int keyIdx) {
        int colon = json.indexOf(':', keyIdx);
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        return json.substring(firstQuote + 1, secondQuote);
    }

    private static final class Case {
        String name;
        String xml;
        String expected;
    }

    private Phase1ShaVerifier() {
    }
}
