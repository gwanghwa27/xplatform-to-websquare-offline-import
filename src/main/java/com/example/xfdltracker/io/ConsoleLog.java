package com.example.xfdltracker.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

/** 로컬 변환기 디버깅을 위해 콘솔과 UTF-8 파일 로그를 동시에 설정한다. */
public final class ConsoleLog {
    private ConsoleLog() {
    }

    public static void install(File logFile, boolean append) throws Exception {
        if (logFile == null) {
            return;
        }

        File parent = logFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("로그 디렉터리를 생성할 수 없습니다: " + parent);
        }

        PrintStream originalOut = System.out;
        FileOutputStream fileOut = new FileOutputStream(logFile, append);
        PrintStream teeOut = new PrintStream(
                new TeeOutputStream(originalOut, fileOut),
                true,
                "UTF-8");

        System.setOut(teeOut);
        System.setErr(teeOut);
        System.out.println("[로그] " + logFile.getAbsolutePath());
    }
}
