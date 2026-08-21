# XPlatform → WebSquare Converter
## 폐쇄망 반입용 Source Project

상세 설치/실행/검증 방법:

- `docs/OFFLINE-USER-GUIDE.md`
- `docs/OFFLINE-USER-GUIDE.docx`
- `docs/OFFLINE-USER-GUIDE.pdf`

## 1. 프로젝트 목적

Phase4-derived working candidate(누적 Production 수정 8건 포함)를 인터넷 없는 폐쇄망 PC로 반입해,
Maven/Gradle/외부 JAR 없이 JDK 1.8.0_111만으로 compile/convert/verify할 수 있게 만든 독립 소스
프로젝트다. Phase4 original ZIP baseline은 IMMUTABLE/FROZEN이며 이 프로젝트는 그 위에 파생된
working candidate를 COPY한 것이다.

## 2. 요구 환경

- JDK **1.8.0_111**
- UTF-8
- Maven/Gradle **불필요**
- 외부 JAR **불필요**
- 인터넷 연결 **불필요**

## 3. IDE 선택

이 프로젝트는 특정 IDE에 종속되지 않는다.

지원:

- IntelliJ IDEA
- Eclipse
- Command Line

### IntelliJ

Project SDK:
JDK 1.8.0_111

Language Level:
8

### Eclipse

Installed JRE:
JDK 1.8.0_111

Execution Environment:
JavaSE-1.8

Compiler compliance:
1.8

둘 중 하나를 선택해서 사용할 수 있으며 제공된 `build.bat`/`build.sh`가 최종 기준 빌드 방법이다.

## 4. Command line build

```
build.bat        (Windows)
./build.sh        (Linux/Unix)
```

## 5. Sample 변환

```
convert-sample.bat        (Windows)
./convert-sample.sh        (Linux/Unix)
```

입력: `sample-phase3-project/` → 출력: `build/sample-output/` (reference `sample-phase3-output/`는
덮어쓰지 않음)

## 6. Offline verification

```
verify-offline.bat        (Windows)
./verify-offline.sh        (Linux/Unix)
```

## 7. 현재 검증 상태

controls:
148 scanned / 146 PASS / 0 MISMATCH / 2 UNSUPPORTED

Dataset:
13/13

Grid:
GRID-1 REAL_RUNTIME_VERIFIED
GRID-2 REAL_RUNTIME_VERIFIED
GRID-3 UNSUPPORTED_SEMANTIC

CheckBox unbound:
REAL_RUNTIME_VERIFIED
(WIDGET/BOOTSTRAP SEMANTICS)

Auto page init:
NOT VERIFIED / OBSERVED issue

Phase1 SHA:
PASS

## 8. 남은 known gaps

제품/Runtime known gap 4건: Defect 2(CONTENT_NOT_READY, OPEN/CONTRACT_LIMITATION), GRID-3(다중
Format, UNSUPPORTED_SEMANTIC), CheckBox dataset-bound(OPEN), `ev:onpageload` 자동 발화 신뢰성
(AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED).

별도 certification blocker 1건: Target JDK 1.8.0_111 확보/검증(BLOCKED_BY_DISTRIBUTION — 폐쇄망에서
별도 확보 필요, `verify-offline.*`의 1단계 게이트로 확인).

상세는 `docs/FINAL-VERIFICATION-REPORT.md`, `docs/followup-checkBox-ready-jdk-phase1-final.md` 참고.

## 9. WebSquare Runtime 관련 주의사항

- 실제 폐쇄망 WebSquare 서버/Studio/dev pack은 이 프로젝트에 포함되지 않음
- generated XML 배포 시 해당 환경의 WebSquare wpack 절차가 필요할 수 있음

## 10. immutable Phase4 baseline과 현재 working candidate의 관계

Phase4 original ZIP baseline은 절대 수정하지 않는다. 이 반입 프로젝트의 소스는 그 baseline에서
파생된 Phase4-derived working candidate(`work/phase4-working/...`, 누적 Production 수정 8건 포함)를
그대로 COPY한 것이며, baseline 자체를 대체하거나 덮어쓰지 않는다.
