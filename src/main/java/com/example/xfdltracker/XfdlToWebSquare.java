package com.example.xfdltracker;

import com.example.xfdltracker.converter.WebSquareGenerator;
import com.example.xfdltracker.io.ConsoleLog;
import com.example.xfdltracker.model.XfdlAnalysisResult;

import java.io.File;

/** 명령행용 XFDL -> WebSquare 형태 XML 변환기. JDK 1.8 호환. */
public class XfdlToWebSquare {
    public static void main(String[] args) throws Exception {
        ConsoleLog.install(new File("logs", "converter.log"), false);
        if (args.length < 1 || args.length > 2) {
            System.err.println("사용법: java -cp bin com.example.xfdltracker.XfdlToWebSquare <screen.xfdl> [output.xml]");
            System.exit(1);
        }

        File input = new File(args[0]);
        if (!input.isFile()) {
            System.err.println("입력 파일을 찾을 수 없습니다: " + input.getAbsolutePath());
            System.exit(2);
        }

        File output;
        if (args.length == 2) {
            output = new File(args[1]);
        } else {
            String name = input.getName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) name = name.substring(0, dot);
            output = new File(input.getParentFile(), name + ".xml");
        }

        if (input.getCanonicalFile().equals(output.getCanonicalFile())) {
            System.err.println("출력 파일은 원본 XFDL과 달라야 합니다: " + input.getCanonicalPath());
            System.exit(3);
        }

        XfdlFunctionTracker tracker = new XfdlFunctionTracker();
        XfdlAnalysisResult analysis = tracker.analyze(input);
        new WebSquareGenerator().generate(input, output, analysis);

        System.out.println("생성 완료: " + output.getAbsolutePath());
    }
}
