# XPlatform → WebSquare Converter
## 폐쇄망 반입용 Source Project 설치·빌드·실행·검증 가이드

프로젝트명: `xplatform-to-websquare-offline-import`
대상 환경: 인터넷이 없는 폐쇄망 PC (Windows / Linux)

---

## 1. 문서 목적

이 문서는 `xplatform-to-websquare-offline-import` 프로젝트를 폐쇄망 PC로 반입한 뒤, 별도의
인터넷 연결이나 Maven/Gradle 없이 소스를 열고, 빌드하고, sample project를 변환하고, 포함된
offline verifier로 결과를 검증하는 전체 절차를 설명한다.

## 2. 프로젝트 개요

이 프로젝트는 XPlatform XFDL 화면을 WebSquare 5.0.5 XML/JS로 변환하는 Java 8 converter의
**소스 프로젝트**다. 배포용 컴파일 산출물(binary)이 아니라, 폐쇄망에서 직접 컴파일하고 실행할 수
있는 **source project**로 구성되어 있다.

## 3. 반입 프로젝트와 Phase4 baseline 관계

- **Phase4 original ZIP baseline**은 이 프로젝트 작업의 원본이며 **IMMUTABLE/FROZEN**으로 취급된다 —
  이번 반입 프로젝트 생성 과정에서도 전혀 수정하지 않았다.
- 이 프로젝트의 소스는 Phase4 baseline에서 파생된 **Phase4-derived working candidate**
  (`work/phase4-working/...`)를 COPY한 것이며, 그 working candidate에 누적된 **Production 수정 8건**을
  모두 포함한다(§12 참고).
- 즉 이 프로젝트 = Phase4 baseline + 검증된 8건의 최소 수정. baseline 자체를 바꾼 것이 아니라, 그 위에
  파생된 상태를 그대로 반입용으로 옮긴 것이다.

## 4. 필수 환경

| 항목 | 요구사항 |
|---|---|
| OS | Windows 또는 Linux/Unix (둘 다 지원 — `build.bat`/`build.sh` 등 스크립트 쌍 제공) |
| JDK | **1.8.0_111** (exact) |
| 문자 인코딩 | UTF-8 |
| Maven | 불필요 |
| Gradle | 불필요 |
| 외부 JAR | 불필요 |
| 인터넷 연결 | 불필요 |

## 5. 폴더 구조 설명

```
xplatform-to-websquare-offline-import/
├─ src/main/java/...          Production Java source 전체 (76개 파일)
├─ sample-phase3-project/     135 XFDL + 14 XJS 입력 샘플
├─ sample-phase3-output/      최신 working candidate 기준 reference 출력 (136개 XML, 덮어쓰지 않음)
├─ audit/                     Python 기반 audit/verifier 스크립트 + Phase1 fixture
├─ tools/verifier-src/        Java 기반 Phase1ShaVerifier (Production source와 물리적으로 분리)
├─ docs/                      핵심 문서 4종 + 본 가이드(md/docx/pdf)
├─ .idea/                     IntelliJ 최소 project metadata (선택)
├─ .project / .classpath / .settings/   Eclipse 최소 project metadata (선택)
├─ build.bat / build.sh
├─ convert-sample.bat / convert-sample.sh
├─ verify-offline.bat / verify-offline.sh
├─ README-OFFLINE.md
├─ OFFLINE-IMPORT-MANIFEST.md
├─ SHA256SUMS.txt
└─ .gitignore
```

`build/`(컴파일 산출물)는 스크립트 실행 시에만 생성되는 임시 디렉터리이며, 반입 ZIP 자체에는
포함되지 않는다.

## 6. IDE 선택 가이드

이 프로젝트는 특정 IDE에 종속되지 않는다. **IntelliJ IDEA / Eclipse / Command Line** 중 자유롭게
선택할 수 있으며, 어느 쪽도 필수 의존성이 아니다. **Command Line(`build.bat`/`build.sh`)이 최종
기준 빌드 방법**이고, IDE는 이 소스를 열고 실행하는 편의 수단이다.

### 6.1 IntelliJ IDEA 사용

1. **프로젝트 Open**: IntelliJ에서 `File → Open`으로 `xplatform-to-websquare-offline-import` 폴더
   자체를 연다(포함된 `.idea/` metadata를 인식한다).
2. **Project SDK 설정**: `File → Project Structure → Project → SDK`에서 폐쇄망 PC에 설치된
   **JDK 1.8.0_111**을 선택(없으면 `Add SDK`로 등록).
3. **Language Level 확인**: 같은 화면에서 Language level이 **8**로 되어 있는지 확인
   (`.idea/misc.xml`에 `JDK_1_8`로 이미 지정되어 있음).
4. **Source root 확인**: `src/main/java`가 소스 루트로 인식되는지 확인(`.iml`에 이미 지정됨).
5. **실행 방법**: IntelliJ 내장 컴파일러로 `Build → Build Project`를 실행하거나, IntelliJ의
   Terminal 탭에서 `build.bat`/`build.sh`를 직접 실행해도 된다.

### 6.2 Eclipse 사용

1. **Import**: `File → Import → Existing Projects into Workspace`(또는 폴더 구조에 따라
   `File → Import → Projects from Folder or Archive`)로 `xplatform-to-websquare-offline-import`
   폴더를 선택한다(포함된 `.project`/`.classpath`를 인식한다).
2. **Installed JRE 등록**: `Window → Preferences → Java → Installed JREs`에서 폐쇄망 PC의
   **JDK 1.8.0_111**을 추가 등록한다.
3. **JavaSE-1.8 Execution Environment 연결**: 같은 Preferences 화면의
   `Installed JREs` 또는 `Execution Environments`에서 **JavaSE-1.8**에 방금 등록한
   JDK 1.8.0_111을 연결한다(`.classpath`가 `JavaSE-1.8` 컨테이너를 참조하도록 이미 구성되어 있다).
4. **Compiler compliance 확인**: `Window → Preferences → Java → Compiler`에서
   Compiler compliance level이 **1.8**인지 확인(`.settings/org.eclipse.jdt.core.prefs`에 이미 지정됨).
5. **source/output folder 확인**: `src/main/java`가 source, `build/classes`가 output으로 지정되어
   있는지 확인.
6. **실행 방법**: Eclipse 빌드는 저장 시 자동 컴파일되며, Package Explorer에서 원하는 클래스를
   `Run As → Java Application`으로 실행할 수 있다. 또는 Eclipse의 Terminal/외부 셸에서
   `build.bat`/`build.sh`를 직접 실행해도 된다.

## 7. IDE 없이 Command Prompt / Shell 사용

방법 C(권장, 가장 단순):

```
JAVA_HOME을 JDK 1.8.0_111 설치 경로로 설정
→ build.bat (Windows) 또는 ./build.sh (Linux/Unix)
→ convert-sample.bat 또는 ./convert-sample.sh
→ verify-offline.bat 또는 ./verify-offline.sh
```

IDE를 전혀 사용하지 않아도 위 3개 스크립트만으로 빌드/변환/검증이 전부 끝난다.

## 8. Build 방법

Windows:
```
build.bat
```

Linux/Unix:
```
./build.sh
```

두 스크립트 모두 다음을 수행한다:
- 현재 `JAVA_HOME`/`java`/`javac` 버전 표시
- 정확히 1.8.0_111이면 `[TARGET_JDK_MATCH]`, 아니면 `[TARGET_JDK_MISMATCH_WARNING]`(빌드 자체는 계속
  진행 — 이 경고는 실패가 아니며, target JDK 인증으로 승격되지도 않는다. 인증은 §10 참고)
- `src/main/java` 전체를 `build/classes/`로 컴파일(소스 트리 자체에는 `.class`를 생성하지 않음)

## 9. Sample 변환 방법

```
convert-sample.bat        (Windows)
./convert-sample.sh       (Linux/Unix)
```

- 입력: `sample-phase3-project/` (135 XFDL + 14 XJS)
- 출력: `build/sample-output/` (새로 생성, 매 실행마다 초기화됨)
- **reference**: `sample-phase3-output/` — 이 프로젝트에 미리 포함된, 최신 working candidate 기준
  reference 출력이며 **이 스크립트는 이 폴더를 절대 덮어쓰지 않는다**.

즉 `build/sample-output/`(방금 폐쇄망에서 새로 생성한 결과)과 `sample-phase3-output/`(반입 시점의
정답 reference)을 나중에 비교해 재현성을 확인할 수 있다. 정상이면 `149/149`(성공 149, 실패 0)가
출력된다.

## 10. Offline Verification

```
verify-offline.bat        (Windows)
./verify-offline.sh       (Linux/Unix)
```

8단계로 구성:

| 단계 | 내용 | 의미 |
|---|---|---|
| 1 | exact JDK 1.8.0_111 게이트 | **mandatory core gate**. `java`/`javac` 둘 다 정확히 1.8.0_111이어야 PASS. 다른 8u 버전/JDK11/17/21/`--release 8`은 인정하지 않음 |
| 2 | clean compile | `build.bat`/`build.sh` 재실행 |
| 3 | sample conversion | `convert-sample.bat`/`.sh` 재실행, 149/149 확인 |
| 4 | 생성된 output XML 개수 | 136개 확인 |
| 5 | Phase1 SHA verifier | Python(있으면) + Java(항상, mandatory) 양쪽 실행 |
| 6 | source tree `.class`/`.jar` 존재 여부 | 0/0이어야 PASS |
| 7 | reference output diff 요약 | `build/sample-output/` vs `sample-phase3-output/` |
| 8 | (optional) Node.js JS syntax check | Node 없으면 `SKIPPED_OPTIONAL_TOOL` |

결과 표시는 `PASS` / `FAIL` / `SKIPPED_OPTIONAL_TOOL` 세 가지다. **Python이나 Node가 없어도
`SKIPPED_OPTIONAL_TOOL`로만 표시되며 core verification 전체를 실패로 만들지 않는다** — 1~4, 5(Java
verifier만), 6이 core이고, 8과 5의 Python 부분은 optional이다.

**exact JDK 1.8.0_111이 없는 PC에서는 1단계가 반드시 FAIL하고 전체 결과가
`[CORE_VERIFICATION_FAIL]`로 끝난다 — 이는 정상 동작이며 converter defect가 아니다.** 다른 JDK로는
target-JDK 인증을 받을 수 없다는 원칙을 이 스크립트가 강제하는 것뿐이다.

## 11. Phase1 SHA 검증

확정된 추출 recipe(생성된 **출력** XML 기준, 소스 XFDL이 아님):

1. 컨버터가 생성한 WebSquare 출력 XML을 대상으로 한다.
2. 문서 내 모든 `<script>` 요소를 XML 파서(textContent/itertext 의미)로 문서 순서대로 읽는다.
3. 각 요소의 텍스트를 `"\n"`으로 join한다.
4. 끝의 개행을 제거한 뒤 정확히 1개의 `"\n"`을 추가한다.
5. UTF-8(BOM 없음)로 인코딩한다.
6. SHA-256을 계산한다.

Expected:
- `Sample`: `f82379cfb619d611ae4137032af43fd10faf3df88f018ca5db3b72c490f4d3fe`
- `CommentProtection`: `14f3466acde50241698ccf21edec5807464a2a6e903854c78ed59332c7b2b987`

실행:
```
python audit/phase1_sha_verifier.py audit/phase1_sha_manifest.json
```
또는 (Python 없이, JDK만으로):
```
javac -encoding UTF-8 -d build/verifier-classes tools/verifier-src/com/example/xfdltracker/verifier/Phase1ShaVerifier.java
java -cp build/verifier-classes com.example.xfdltracker.verifier.Phase1ShaVerifier audit/phase1_sha_manifest.json
```
두 verifier는 동일한 recipe를 구현하며, `verify-offline.*` 스크립트가 Java 버전을 mandatory core
check로 자동 실행한다.

## 12. 현재 검증된 기능

- control structure (id/type/geometry/hierarchy/property): **148 scanned / 146 PASS / 0 MISMATCH**
- geometry / hierarchy: 대표 화면 REAL_RUNTIME_VERIFIED
- Dataset: **13/13**
- Grid: GRID-1(구조+interaction) / GRID-2 REAL_RUNTIME_VERIFIED
- Static / Edit: 정적 value 렌더링 REAL_RUNTIME_VERIFIED
- CheckBox(unbound): **REAL_RUNTIME_VERIFIED (WIDGET/BOOTSTRAP SEMANTICS)** — `addItem(value,label)`
  호출, 실제 `<input>`+`<label>` 생성, click→checked→`getValue()` round-trip을 실 엔진에서 확인. 단
  생성된 `ev:onpageload`가 페이지 진입 시 **자동으로 발화하는지는 별도로 확인되지 않음**
  (`AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED`, §13 참고 — 과대승격 방지를 위해 두 축으로 분리 판정)
- Phase1 SHA: **STATIC_VERIFIED / PASS**

## 13. 현재 남은 제한

**제품/Runtime known gap 4건**:
1. Defect 2 — `CONTENT_NOT_READY` false-negative: **OPEN / CONTRACT_LIMITATION**. 동적 `set_url()`
   경로에서 `setUrl()`의 성공 콜백/Promise를 신뢰하는 로직이 실패로 오탐될 수 있음.
2. GRID-3 — 다중 Format(default/alternate) 전환: **UNSUPPORTED_SEMANTIC**. WebSquare gridView 구조상
   표현 불가, 수동 재설계 필요.
3. CheckBox — dataset-bound 케이스: **OPEN**. shipped 참조 사례가 없어 안전한 일반화 근거 없음.
4. `ev:onpageload` 자동 발화 신뢰성: **AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED**. 일부 페이지/라우트에서
   onload 바인딩이 자동으로 실행되지 않는 현상이 관찰됨. BIND-1/CheckBox-unbound/Defect-2에 공통.

**별도 certification blocker 1건**:
- exact JDK 1.8.0_111 확보/검증 상태: **BLOCKED_BY_DISTRIBUTION**. 이 프로젝트를 패키징한 온라인 PC에서
  신뢰 가능한 portable 1.8.0_111 바이너리를 확보하지 못했다(Oracle은 로그인 필수라 시도하지 않았고, 그
  외 아카이브는 설치형이거나 update 번호가 다름). **폐쇄망 사용자가 별도로 exact 1.8.0_111을 확보해
  `verify-offline.*`의 1단계 게이트를 통과시켜야 한다.**

## 14. WebSquare 배포 관련 주의사항

이 프로젝트에는 다음이 **포함되지 않는다**:
- WebSquare server / WebSquare Studio
- WebSquare engine JAR
- Tomcat
- MariaDB
- license

이 프로젝트는 XFDL → WebSquare XML/JS **변환기 소스**만 제공한다. 생성된 XML을 실제 WebSquare 환경에
배포하려면, 해당 기관 폐쇄망의 WebSquare 환경에 설치된 **wpack 컴파일 도구와 배포 절차**를 별도로
따라야 한다(이 프로젝트가 그 절차를 대신하지 않는다).

## 15. 오류 해결 / Troubleshooting

| 증상 | 원인/조치 |
|---|---|
| `JAVA_HOME` 미설정 | `java`/`javac`가 PATH에 없으면 `build.bat`/`build.sh`가 즉시 `[FAIL]`로 종료. JDK 1.8.0_111의 `bin/`을 PATH에 추가하거나 `JAVA_HOME`을 설정 |
| java/javac 버전 불일치 | `[TARGET_JDK_MISMATCH_WARNING]`(빌드는 진행) 또는 `verify-offline`의 `[TARGET_JDK_MISMATCH]`(검증 실패) — exact 1.8.0_111만 인정 |
| JDK가 아닌 JRE만 등록된 경우 | `javac`가 없으므로 컴파일 자체가 불가. JRE가 아닌 **JDK**를 설치/등록해야 함 |
| IntelliJ SDK 설정 오류 | `File → Project Structure → SDKs`에서 JDK 1.8.0_111의 실제 설치 경로를 다시 지정 |
| Eclipse Installed JRE 오류 | `Window → Preferences → Java → Installed JREs`에서 등록 상태 확인, `JavaSE-1.8` Execution Environment에 연결되어 있는지 재확인 |
| Eclipse compiler level 오류 | `Window → Preferences → Java → Compiler`에서 Compliance level을 1.8로 재설정 |
| UTF-8 문제 | 모든 스크립트가 `-encoding UTF-8`/`chcp 65001`을 명시적으로 사용 — 콘솔 폰트가 한글을 지원하지 않으면 글자가 깨져 보일 뿐 데이터 자체는 정상 |
| `build/classes` 삭제 후 재빌드 | `build.bat`/`build.sh`는 매 실행 시 `build/`를 삭제 후 재생성하므로 별도 수동 삭제 불필요 |
| Python/Node 미설치 시 optional verification skip | `verify-offline.*`가 자동으로 `[SKIPPED_OPTIONAL_TOOL]`로 표시하고 core verification은 계속 진행 |
| generated output과 reference output 차이 확인 | `verify-offline.*`의 7단계가 자동 비교하며, 필요시 `build/sample-output/`과 `sample-phase3-output/`을 직접 diff 도구로 비교 |

## 16. 보안/폐쇄망 주의사항

- 인터넷 접근 기능 없음(Production Java 코드가 외부 URL을 호출하지 않고, 스크립트도 curl/wget/download를
  실행하지 않음)
- 외부 repository 접근 없음(Maven/Gradle 자체가 없음)
- 실제 credential 포함 안 됨(반입 프로젝트 sanitize 결과는
  `docs/OFFLINE-DOCUMENT-SANITIZATION.md` 참고)
- license 포함 안 됨
- 개발 PC absolute path 의존 없음(모든 스크립트는 `SCRIPT_DIR`/`PROJECT_ROOT` 기준 상대경로 사용)

## 17. 최종 운영 체크리스트

```
[ ] JDK 1.8.0_111 확인 (java -version / javac -version 둘 다)
[ ] IntelliJ 또는 Eclipse SDK 연결 (또는 Command Line만 사용)
[ ] build 성공 (build.bat / build.sh)
[ ] sample conversion 149/149 (convert-sample.bat / convert-sample.sh)
[ ] XML 생성 확인 (136개, build/sample-output/)
[ ] Phase1 SHA PASS (Python 및/또는 Java verifier)
[ ] reference diff 검토 (sample-phase3-output/ vs build/sample-output/)
[ ] .class/.jar source tree 미포함 확인 (verify-offline 6단계)
[ ] verify-offline 전체 결과 확인 (exact JDK 미보유 시 1단계 FAIL은 정상)
```

---

*이 문서(`docs/OFFLINE-USER-GUIDE.md`)가 canonical source이며, 동일 revision에서 생성된
`docs/OFFLINE-USER-GUIDE.docx`/`docs/OFFLINE-USER-GUIDE.pdf`가 함께 제공된다.*
