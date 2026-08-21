package com.example.xfdltracker.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/** BOM 처리를 지원하는 Java 8 텍스트 입출력 도우미. */
public final class TextFileUtil {
    private TextFileUtil() {}

    public static String read(File file, String fallbackEncoding) throws Exception {
        byte[] bytes = readAll(file);
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf) {
            return new String(bytes, 3, bytes.length - 3, Charset.forName("UTF-8"));
        }
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xfe) {
            return new String(bytes, 2, bytes.length - 2, Charset.forName("UTF-16LE"));
        }
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xff) == 0xff) {
            return new String(bytes, 2, bytes.length - 2, Charset.forName("UTF-16BE"));
        }
        return new String(bytes, Charset.forName(fallbackEncoding));
    }

    public static void writeUtf8(File file, String text) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("디렉터리를 생성할 수 없습니다: " + parent);
        }
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8"));
        try {
            writer.write(text);
        } finally {
            writer.close();
        }
    }

    private static byte[] readAll(File file) throws Exception {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream((int)Math.min(file.length(), 1024 * 1024));
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
