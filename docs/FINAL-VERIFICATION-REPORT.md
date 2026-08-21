# XPlatform → WebSquare Converter — Online PC Target-Environment Verification
Final Report — 2026-08-17, last updated 2026-08-18 (follow-up round). Latest state is
`work/results/followup-checkBox-ready-jdk-phase1-final.md`; §18 is the previous milestone; §14~§17 are earlier
milestones kept as historical record — see each section's own "historical" markers.

**최신 상태 요약(상세는 `followup-checkBox-ready-jdk-phase1-final.md`)**: Phase4 original ZIP baseline 불변.
Working candidate에 **8건 수정(FIXED)** 반영(§18까지의 7건 + 이번 라운드의 CheckBox-unbound). CheckBox-unbound는
**REAL_RUNTIME_VERIFIED (WIDGET/BOOTSTRAP SEMANTICS)** — `addItem(value,label)`/실제 input·label 생성/
click→checked→getValue round-trip은 real engine에서 확인했으나, 생성된 `ev:onpageload`의 자동 발화 자체는
확인되지 않아 그 부분만 별도로 **AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED**로 표시(동일 bootstrap을 live widget에
수동 실행해 semantics만 검증했다는 뜻, §9/§11 참고). **제품/Runtime 상 남은 known gap은 4건**: Defect 2
(CONTRACT_LIMITATION), GRID-3(UNSUPPORTED_SEMANTIC), REALRT-2-CheckBox dataset-bound(OPEN), `ev:onpageload`
자동 발화 신뢰성(OBSERVED). 이와 별도로 **certification blocker 1건**: Target JDK 1.8.0_111이
**BLOCKED_BY_DISTRIBUTION**(exact 1.8.0_111 portable 바이너리를 신뢰 가능한 경로로 확보 불가, 대체하지 않음) —
이 5개는 서로 다른 두 범주(제품 gap 4 + 별도 certification blocker 1)이며 "4개"라는 표현과 모순되지 않는다.
**Phase1 SHA는 이번 라운드에 PASS로 해소**(정확한 추출 recipe 확정: 생성된 출력 XML의 `<script>` 콘텐츠, 자세한
내용은 follow-up 문서 §4). 135 XFDL 구조 audit: 148 scanned/146 PASS/0 MISMATCH/2 UNSUPPORTED(변경 없음).

## 1. Overall Result

| Track | Status |
|---|---|
| Engineering (static/mock, Phase4 baseline) | STATIC_VERIFIED / MOCK_RUNTIME_VERIFIED (재검증 완료, 기존 수치와 100% 일치) |
| Real WebSquare | **REAL_RUNTIME_VERIFIED** — WFrame 생성 + TabControl 탭 추가/선택전환까지 실제 확인. CheckBox(unbound)는 widget/bootstrap semantics(value/checked/label/click round-trip)만 REAL_RUNTIME_VERIFIED, 자동 page-init 발화는 AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED(§9/§11) |
| Target JDK 1.8.0_111 | **BLOCKED_BY_DISTRIBUTION** (별도 certification blocker, 제품 gap 4건과는 분리) — exact 1.8.0_111 portable 바이너리를 신뢰 가능한 경로(로그인 불필요)로 확보 불가, 다른 JDK로 대체하지 않음 |
| Phase1 SHA | **STATIC_VERIFIED / PASS** — 추출 recipe 확정 및 재현 성공 (기존 UNRESOLVED에서 해소) |
| Production source | Phase4 original ZIP baseline은 **IMMUTABLE/FROZEN 유지**(미수정, 전 세션 공통). **Phase4-derived working candidate**(`work/phase4-working/...`)에 누적 **8건** 수정 적용: Defect 1(§15), GRID-1/GRID-2(§16), BIND-1/REALRT-1(§17), REALRT-2-Static/Edit(§18), REALRT-2-CheckBox-unbound(이번 라운드, widget/bootstrap semantics REAL_RUNTIME_VERIFIED / 자동 page-init은 별도 OBSERVED) — 최신 상태는 `followup-checkBox-ready-jdk-phase1-final.md` 참고. Defect 2, GRID-3, REALRT-2-CheckBox(dataset bound)는 **의도적으로 OPEN/UNSUPPORTED_SEMANTIC 유지**(수정 미적용) |

## 2. PC Environment

| Component | Detected | Version | Path |
|---|---|---|---|
| OS | Yes | Windows 11 Pro 10.0.26100.9168 | - |
| Java (기본 PATH) | Yes | OpenJDK 21.0.10 (Temurin) | <LOCAL_PATH_REDACTED>\.jdks\temurin-21.0.10 |
| 기타 설치 JDK | Yes | temurin-11.0.30/21.0.10/21.0.11, openjdk-26/26.0.1 | <LOCAL_PATH_REDACTED>\.jdks\* |
| **JDK 1.8.0_111 (목표)** | **No** | 미발견 | - |
| Node.js | Yes | v24.18.0 | - |
| Python | Yes | 3.14.6 | - |
| Git | Yes | 2.54.0 | - |
| Chrome / Edge | Yes | 151.x / 151.x | - |
| Tomcat/WebLogic/JBoss/WebSphere (기존 실행 중) | No | - | - |
| **WebSquare Studio 5 SP5 교육용 dev pack (파일시스템)** | Yes | WebSquare 5.0.5 (build 202403051313), 엔진 jar 5.0_5.5170B.20240319.123453_1.5 | <LOCAL_PATH_REDACTED>\WEBSQUARE_DEV_PACK_SP5_edu |
| Maven | 최초 미발견, 이번 세션에 로컬 설치 | 3.9.16(1차 실패) → 3.9.6(성공) | work/tools/apache-maven-3.9.6 (PATH 변경 없음) |

## 3. Baseline Integrity (Phase4 ZIP)

| 항목 | 기대 | 실제 | 결과 |
|---|---|---|---|
| SHA-256 | 89a290f1...a3f0ef2 | 89a290f1...a3f0ef2 | PASS |
| Java source | 76 | 76 | PASS |
| sample-phase3-project XFDL | 135 | 135 | PASS |
| sample-phase3-project XJS | 14 | 14 | PASS |
| ZIP 내 .class | 0 | 0 | PASS |
| ZIP 내 .jar | 0 | 0 | PASS |
| Maven/Gradle | 없음 | 없음 | PASS |
| IntelliJ 설정 | JDK1.8/bytecode1.8 | languageLevel=JDK_1_8, bytecodeTargetLevel=1.8 | PASS |

Phase6 evidence ZIP: SHA-256 일치(8ea0cb28...881bc), 21개 evidence 파일 전체 `sha256sum -c` PASS,
`PHASE6-SOURCE-CHANGES.diff` 0 bytes 실측 확인.

## 4. Actual Tests Executed

| 명령 | Exit | 비고 |
|---|---|---|
| javac(JDK21, `-source/-target 1.8`, forward-slash 경로) 76개 컴파일 | 0 | 132 class 생성. **주의**: 프로젝트 자체 `build.bat`/`build.sh`는 JDK21에서 그대로 실행 시 실패함 (아래 6번 참고) |
| `XPlatformProjectConverter sample-phase3-project sample-phase3-output UTF-8` | 0 | 149/149 성공 |
| Python XML parse (생성 XML 전수) | - | 136/136 parse 성공 |
| `node --check` (생성 page JS 136개) | - | 136/136 PASS |
| `node --check` (standalone/common JS 15개) | - | 15/15 PASS |
| `node runtime-finalization-mock.js` | 0 | **PASS 41** |
| `node phase4-lifecycle-mock.js` | 0 | **PASS 14** |
| Maven 3.9.6 offline `package` (WRM) | 0 | BUILD SUCCESS (pom 수정 2건, work 사본에만 적용) |
| Tomcat 9 + JDK11 + WRM 실배포 | - | 아래 7번 참고 |

## 5. Static / Mock Result

전부 기대치와 **정확히 일치**:

```
project conversion      149 / 149   PASS
XML parse                136 / 136   PASS
page JS syntax           136 / 136   PASS
standalone/common JS     15 / 15     PASS
Runtime mock              41 / 41     PASS
generated lifecycle mock  14 / 14     PASS
```

### Phase1 SHA-256 — UNRESOLVED (기대치와 불일치, 원인 특정 못함)
Sample.xfdl / CommentProtection.xfdl을 실제로 변환하고 `<script>` 내용을 8가지 합리적인 방식으로 추출·해시했으나
문서에 기재된 기대 SHA-256 값과 **어느 것도 일치하지 않았습니다.** ZIP 안에 이 해시를 재현하는 스크립트/도구가 없어
원래 추출 방식을 확인할 수 없었습니다. **PASS로 강제 보고하지 않고 UNRESOLVED로 유지**했습니다(상세: `work/results/phase1-sha-findings.md`).
변환 자체는 실제로 성공(exit 0)했고 생성 XML도 정상입니다 — 이것이 실제 회귀(regression)인지, 단순히 해시 산출 방식
차이인지는 이번 조사만으로는 판단 불가합니다.

### Phase 2 / Phase 3 세부 regression — NOT_EXECUTED
GridFormatsPhase2 byte-exact, RootIdCollision, namespace/prefix, Huge Grid fail-fast, input=output, partial project,
programmatic convert, output-inside-source, DatasetBinding/ControlPropertyMatrix/GridAdvancedPhase3 byte-identical,
Static External Tab 11종 — 이번 세션에서는 사용자 지시에 따라 PC 환경(JDK/Maven/실 WebSquare) 확보 쪽에 시간을
집중 배분했기 때문에 **개별적으로 재실행하지 않았습니다.** 149/149 전체 프로젝트 변환 안에 해당 fixture들이
포함되어 있어 "변환 자체는 성공"했다는 것은 확인되지만, 개별 byte-exact 비교는 NOT_EXECUTED입니다.

## 6. Target JDK Result

- `java -version` / `javac -version`: 시스템에 1.8.0_111 없음 → **TARGET_JDK_RUNTIME_VERIFIED 불가**
- 대체 시도하지 않음 (`--release 8`이나 다른 Java 8 update로 대체 금지 원칙 준수)
- Phase4 변환기 자체 실행 로그에 반복적으로 `[JS 문법검사 건너뜀] Nashorn 엔진을 찾을 수 없습니다` 출력 — JDK21에는
  Nashorn이 제거되어 있어 자체 JS 문법 검사가 스킵됨. 이는 target JDK 부재의 실측 증거.
- **부수적으로 발견한 실제 재현 가능한 사실**: 이 프로젝트의 `build.bat`/`build.sh`(자체 javac argfile 방식)는
  JDK 9+ 에서 `@sources.txt` 안의 Windows 백슬래시 경로가 javac의 argfile escape 처리(JDK-8027634, JDK9+ 신규
  동작) 때문에 깨져서 컴파일 자체가 실패합니다. JDK 1.8.0_111(구형 tokenizer)에서는 문제되지 않을 것으로 보이며,
  이는 "정확한 JDK가 아니면 빌드조차 재현 안 될 수 있다"는 걸 뒷받침하는 실측 증거입니다. Production 코드 결함이
  아니라 JDK 버전 차이이므로 수정하지 않았습니다.

## 7. WebSquare Result

### 발견된 환경
`<LOCAL_PATH_REDACTED>\WEBSQUARE_DEV_PACK_SP5_edu` — WebSquare Studio 5 SP5 교육용 dev pack. Tomcat 9.0.10,
JDK11.0.13, MariaDB, `WRM`(WebSquare Reference Model, Spring MVC, 실 엔진 서블릿 포함)과 `KMS`(정적 컴포넌트
데모 모음) 두 개 예제 프로젝트 포함.

### 실행한 실제 테스트

1. **KMS 단독 배포** → 엔진 부트스트랩(`_websquare_/javascriptLoader.wq`) **404**, `WebSquare is not defined`
   콘솔 에러. KMS는 WEB-INF가 없는 순수 정적 프로젝트라 엔진 서블릿이 없음이 원인. → **REAL_RUNTIME 불가, 원인 규명**
2. **Maven 로컬 설치**(공식 3.9.16 → 3.9.16 offline 플러그인 부재로 실패 → 3.9.6로 재시도, 둘 다 공식
   dlcdn/archive.apache.org에서 다운로드, SHA-512 검증 완료) → work/tools/apache-maven-3.9.6 (PATH 미변경)
3. **WRM 오프라인 빌드**: work 사본 pom.xml에 (a) 캐시된 플러그인 버전 고정 + surefire 테스트 단계 비활성화,
   (b) `src` 하위 non-.java 리소스(`websquareConfig.properties` 등)를 `target/classes`로 복사하는 `<resources>`
   블록 추가 — **두 수정 모두 work 사본에만 적용, 원본 dev pack·Phase4 Production source 미수정**.
   → **BUILD SUCCESS**, WEB-INF/lib 48개 jar (실 엔진 jar·batik 포함)
4. **WRM `/WRM` context 배포**: `WEBSQUARE_HOME` 시스템 프로퍼티 필요(없으면 config FileNotFoundException) →
   설정 후 **실 라이선스 파일 검증 통과**(Demo License, studio+hybrid platform, 만료 2026-10-31) → Spring context
   기동 성공 → 브라우저에서 `javascriptLoader.wq` **200**, 그러나 `_wpack_/cm/js/commonGlobal.js` 등은 **404**
   (컨텍스트 경로 접두사 없이 요청됨) → `com is not defined` → 앱 초기화 미완료
5. **ROOT context로 임시 재배포**(원본 ROOT는 별도 보관 후 복원): 동일 리소스들 **200**으로 전환, 브라우저에서
   `typeof window.WebSquare === 'object'`, `typeof window.com === 'object'` 직접 확인 — **실제 엔진 객체가 내부
   상태(UUID, moduleList, scriptCachedList 등)를 갖고 정상 초기화됨을 실측**.
6. **실제 페이지(WFrame) 렌더링 시도**: WRM의 Spring MVC `InitController`가 `GET /`을 처리하도록 매핑되어 있으나
   실제로는 **HTTP 404**(브라우저·curl 양쪽에서 확인) — WRM 자체 애플리케이션 계층의 라우팅 문제로 판단, 시간 관계상
   근본 원인 추적은 중단. → **WFrame 생성/TabControl lifecycle 관찰에는 도달하지 못함**

### 실제 trace
WFrame/TabControl A~N 항목(섹션 16) 전부 **NOT_EXECUTED** — 실제 페이지 렌더링에 도달하지 못했으므로 가짜 trace를
기록하지 않았습니다.

## 8. Bugs Found (실제 재현한 것만)

| ID | 분류 | 심각도 | 요약 |
|---|---|---|---|
| ENV-1 | TARGET_JDK_COMPATIBILITY | Low | 프로젝트 `build.bat`/`build.sh`가 JDK9+ 환경에서 javac argfile 백슬래시 이스케이프 문제로 컴파일 실패 (JDK8에서는 미발생 추정) |
| ENV-2 | PROJECT_FRAMEWORK_DEPENDENT | Medium | WRM(WebSquare 자체 예제 앱)의 pom.xml이 Eclipse/m2e 전용으로 작성되어 순수 CLI Maven으로는 리소스 파일 누락(websquareConfig.properties 등) — WRM 자체 문제, XPlatform 컨버터와 무관 |
| ENV-3 | PROJECT_FRAMEWORK_DEPENDENT | Medium | WRM의 `websquare.xml`(WEBSQUARE_HOME 설정)이 ROOT 컨텍스트 배포를 가정하고 있어 비-ROOT 컨텍스트(`/WRM`)에서는 `_wpack_` 공통 스크립트가 404됨 |
| ENV-4 | UNRESOLVED (분류 보류) | - | WRM Spring MVC `GET /` 매핑이 실제로는 404 반환 — 근본 원인 미규명 |
| VERIFIER-? | 없음 | - | Phase6 자체 verifier 스크립트(`phase6-verify-target-jdk8.*`)는 정확한 JDK 부재로 실행하지 못해 감사 대상에서 제외 |

**(Stale — §15/§16/§17로 대체됨)** 이 시점(§7까지)에서는 Phase4 산출물을 실 Runtime에 배포하지 못했으므로 converter
결함이 발견되지 않았으나, §15(Dynamic setUrl 실 Runtime 검증)에서 **converter 자체의 실제 결함 2건**을 확인:

| ID | 분류 | 심각도 | 요약 | 상태 |
|---|---|---|---|---|
| DEFECT-1 | CONVERTER_DEFECT (WebSquareGenerator) | High | 초기 url 없는 runtime `set_url()` 대상 Tabpage가 WFrame 아닌 일반 content로 생성되어 `setSrc is not a function` | **FIXED**, REAL_RUNTIME_VERIFIED (§15) |
| DEFECT-2 | RUNTIME_ADAPTER_CONTRACT (TabRuntimeScriptGenerator) | Medium | 동적 `.setSrc()` 경로에서 `ev:onpageload`가 발생하지 않아 콘텐츠는 정상 로드돼도 `CONTENT_NOT_READY`로 오탐 | **OPEN**, REAL_RUNTIME_VERIFIED(재현), 수정 미적용 |
| GRID-1 | CONVERTER_DEFECT (GridFormatConverter) | Medium | Grid cell의 combo 바인딩(combodataset/combocodecol/combodatacol)이 조용히 드롭됨 | **FIXED**, REAL_RUNTIME_VERIFIED(구조+interaction 둘 다, §18에서 dropdown open/select/CODE↔NAME 왕복까지 확인) |
| GRID-2 | CONVERTER_DEFECT (GridFormatParser/Converter) | Medium | Grid의 summary band("summ")가 통째로 드롭됨 | **FIXED**, REAL_RUNTIME_VERIFIED (§16) |
| GRID-3 | (WebSquare 자체 한계) | - | Grid의 다중 Format(default/alternate)이 WebSquare gridView 구조상 표현 불가 | **UNSUPPORTED_SEMANTIC 확정**, 미수정 (§16) |
| BIND-1 | CONVERTER_DEFECT (WebSquareGenerator) | Medium | 반복 `w2:dataList`에 스칼라 `ref` 바인딩 시 `rowPosition` 미설정으로 값이 빈 채로 렌더 | **FIXED**, REAL_RUNTIME_VERIFIED (§17) |
| REALRT-1 | CONVERTER_DEFECT (ComponentMappingRegistry) | Medium | CheckBox가 real WebSquare에 존재하지 않는 `xf:selectBoolean`으로 매핑되어 DOM에서 완전히 사라짐 | **FIXED**, REAL_RUNTIME_VERIFIED (§17) |
| REALRT-2 | CONVERTER_DEFECT (WebSquareGenerator, Static/Edit만) | Low-Medium | 정적 `value`가 `xf:input`(Edit)/`w2:span`(Static)에서 실 렌더에 반영 안 됨 | **Static/Edit: FIXED**, REAL_RUNTIME_VERIFIED (§18, `label=`/`initValue=` 속성 사용). **CheckBox: OPEN 유지** — `ref=` 바인딩+별도 label 요소가 추가로 필요해 안전한 최소 수정 범위 밖으로 판단 |

위 ENV-1~4는 여전히 WebSquare 자체 예제 앱(WRM)이나 빌드 환경(JDK 버전) 문제로 XPlatform→WebSquare 변환 로직과
무관하지만, DEFECT-1/2, GRID-1~3, BIND-1, REALRT-1/2는 변환 로직(Production converter) 자체 또는 그 산출물과
real WebSquare 엔진 사이의 실제 결함/한계입니다.

## 9. Bugs Fixed

**(Stale — §15~§18 및 follow-up 라운드로 대체됨)** 최종적으로 **8건**을 Production converter 소스에서 수정:

1. DEFECT-1 — `WebSquareGenerator.java`(§15)
2. GRID-1 — `GridFormatConverter.java`(§16, §18에서 구조+interaction 둘 다 REAL_RUNTIME_VERIFIED로 최종 확정)
3. GRID-2 — `GridFormatParser.java`/`GridFormatConverter.java`(§16)
4. BIND-1 — `WebSquareGenerator.java`(§17, binding-logic만; 자동 onpageload 미발화는 별개 관찰 사항으로 미수정)
5. REALRT-1 — `ComponentMappingRegistry.java`(§17, CheckBox의 DOM 존재만 해결; 값/체크상태/라벨은 REALRT-2-CheckBox로 별도 OPEN)
6. REALRT-2-Static — `WebSquareGenerator.java`(§18, `w2:span`에 `label=` 사용)
7. REALRT-2-Edit — `WebSquareGenerator.java`(§18, `xf:input`에 `initValue=` 사용)
8. REALRT-2-CheckBox(unbound) — `WebSquareGenerator.java` `copyBasicProperties()`(follow-up 라운드, 상세는
   `work/results/followup-checkBox-ready-jdk-phase1-final.md` §1/§5): real `w2:checkbox`가 정적 value/label
   속성을 렌더링에 쓰지 않는다는 것을 실 엔진으로 확인 후, BIND-1과 동일한 page-init bootstrap 채널로
   `addItem(value, label)`을 호출하도록 변경. 판정은 **REAL_RUNTIME_VERIFIED (WIDGET/BOOTSTRAP SEMANTICS)** —
   `addItem(value,label)` 호출, 실제 `<input>`/`<label>` 생성, click→checked→`getValue()` round-trip을 real
   engine에서 확인. 다만 생성된 `ev:onpageload`가 페이지 진입 시 **자동으로 발화하는지는 확인하지 못했고**
   (`/popup?w2xPath=...` 라우트에서 무관 함수(`xpTransaction`)까지 포함해 script 블록 자체가 자동 실행되지
   않음을 재확인), 대신 동일한 bootstrap 코드를 살아있는 위젯에 **수동 실행**해 semantics만 검증했다 — 이
   자동 발화 확인 여부는 별도로 **AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED**로 표시한다(BIND-1의 기존
   onpageload 관찰과 동일 계열). Dataset-bound 케이스는 여전히 OPEN(안전한 일반화 근거 없음).

전부 Phase4-derived working candidate(`work/phase4-working/...`)에만 적용. Phase4 original ZIP baseline 자체는
미수정/불변 유지. DEFECT-2, GRID-3, REALRT-2-CheckBox(dataset bound)는 재현·원인까지 확정했으나 **의도적으로
수정하지 않음**(각각 사용자 지시 또는 안전한 최소 수정 범위를 벗어난다는 판단, §15~§18 및 follow-up 문서 참고).
이 절 앞부분에서 언급한 WRM pom.xml 2건은 여전히 work/ 사본에만 적용된, Production 코드가 아닌 WebSquare 자체
예제 프로젝트의 빌드 설정입니다.

## 10. Remaining Risks

**제품/Runtime known gaps (4건)** — 파일럿 적용 여부와 직접 관련된 항목:

- **DEFECT-2 OPEN / CONTRACT_LIMITATION**: `CONTENT_NOT_READY` false-negative가 재현·원인 확정되었으나 수정은
  미적용 상태로 남아있음(§15, follow-up 문서 §2에서 재확인만 수행— 오히려 `ev:onpageload` 자동 발화 신뢰성
  불확실성이 다른 fixture에서도 재확인되어 안전한 guarded fallback을 이번 라운드에도 확정하지 못함). Runtime
  Adapter의 READY 계약을 사용하는 모든 동적 `set_url()` 대상 페이지에 잠재적으로 영향.
- **GRID-3 UNSUPPORTED_SEMANTIC**: XPlatform Grid의 다중 Format(default/alternate) 전환은 WebSquare gridView
  구조상 표현할 방법을 찾지 못해 미지원으로 확정(§16). 해당 패턴을 쓰는 화면은 수동 재설계 필요.
- **REALRT-2-CheckBox — dataset-bound 케이스만 OPEN**: unbound CheckBox는 이번 follow-up 라운드에서 widget/
  bootstrap semantics(value/checked/label/click round-trip) 기준 REAL_RUNTIME_VERIFIED로 해소(§9 항목 8, 자동
  page-init 발화는 별도 AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED — 바로 아래 항목). Dataset과 양방향 바인딩되는
  CheckBox는 여전히 OPEN — shipped dev pack 안에 standalone bound `w2:checkbox` 참조 사례가 전혀 없어 안전하게
  일반화할 근거가 없음(follow-up 문서 §1). 그런 화면은 파일럿 전 별도 검토 필요.
- **`ev:onpageload` 자동 발화 신뢰성 (AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED)**: 일부 페이지/라우트에서 페이지
  진입 시 onload 바인딩이 자동으로 실행되지 않는 현상 관찰(BIND-1, §18). Follow-up 라운드에서 CheckBox-unbound
  bootstrap으로 다시 관찰됐고(§9 항목 8), Defect 2와 동일 계열 배경 요인으로 보임 — 여전히 미해결(follow-up
  문서 §2, §7). `pageLoadStatements` 채널을 쓰는 모든 fix(BIND-1, GRID-1 setNodeSet, CheckBox-unbound)에 공통
  으로 걸리는 위험.

**별도 certification blocker (1건, 위 4건과 분리)**:

- **TARGET JDK 1.8.0_111 — BLOCKED_BY_DISTRIBUTION**: 로컬/온라인 모두 exact 1.8.0_111의 신뢰 가능한 portable
  Windows x64 JDK 바이너리를 확보하지 못함(Oracle은 로그인 필수라 우회 금지, 그 외 아카이브는 설치형 EXE이거나
  update 번호가 다름). 다른 JDK로 대체하지 않음. Nashorn 기반 실제 JS 문법/런타임 검증은 여전히 미수행(상세는
  follow-up 문서 §3). 이는 제품 자체의 화면 변환 품질 gap이 아니라 target JDK 인증 절차의 blocker이므로 위
  4건과 범주가 다름.

**(해소됨 — §14/§15 참고)** ~~REAL_RUNTIME_REQUIRED (WFrame/TabControl)~~: WRM 자체 페이지(§14)와 Phase4
fixture(§15) 양쪽 모두 WFrame/TabControl lifecycle을 실제 브라우저에서 REAL_RUNTIME_VERIFIED 완료.

- **Phase2/Phase3 세부 byte-exact regression**: 이번 세션에서 개별 실행하지 않음(NOT_EXECUTED).

**(해소됨 — follow-up 문서 §4 참고)** ~~Phase1 SHA-256 UNRESOLVED~~: 정확한 추출 recipe(생성된 출력 XML의
`<script>` 콘텐츠, XML parser textContent, `\n` join, 단일 trailing newline, UTF-8)를 확정하고 두 fixture 모두
재현 성공 — **PASS**로 해소.

## 11. Verification Matrix

| Feature | Level | Result | Evidence |
|---|---|---|---|
| ZIP SHA/구조/baseline | STATIC_VERIFIED | PASS | 섹션 3 |
| Phase6 evidence manifest | STATIC_VERIFIED | PASS | `sha256sum -c` 전체 OK |
| 149/149 변환, 136 XML, 136+15 JS syntax | STATIC_VERIFIED | PASS | 섹션 5 |
| Runtime/lifecycle mock (41+14) | MOCK_RUNTIME_VERIFIED | PASS | 섹션 5 |
| Phase1 SHA regression | STATIC_VERIFIED | **PASS**(recipe 확정, 재현 성공) | `work/audit-scripts/phase1_sha_verifier.py`, follow-up 문서 §4 |
| Phase2/3 세부 regression | - | NOT_EXECUTED | - |
| Target JDK 1.8.0_111 | - | **BLOCKED_BY_DISTRIBUTION** | follow-up 문서 §3 |
| WebSquare 엔진 부트스트랩(라이선스/wpack/객체 초기화) | REAL_RUNTIME_VERIFIED | PASS | 섹션 7, 14 |
| WFrame/TabControl lifecycle (WRM 자체 페이지) | REAL_RUNTIME_VERIFIED | PASS | 섹션 14 |
| Phase4 fixture 실 배포 (Static External Tab, Dynamic setUrl) | REAL_RUNTIME_VERIFIED | PASS(Defect 1 수정 후) / Defect 2 OPEN | 섹션 15 |
| 135 XFDL 구조 audit (control/geometry/hierarchy/dataset) | STATIC_VERIFIED | 148 scanned/146 PASS/0 MISMATCH/2 UNSUPPORTED | 섹션 16, 18 |
| Grid combo binding (GRID-1) | REAL_RUNTIME_VERIFIED | PASS — **구조 + interaction 둘 다 확인**(dropdown open/select/CODE↔NAME 왕복) | 섹션 18 |
| Grid summary band (GRID-2) | REAL_RUNTIME_VERIFIED | PASS | 섹션 16 |
| Grid 다중 Format (GRID-3) | UNSUPPORTED_SEMANTIC | 확정, 미수정 | 섹션 16 |
| Dataset 반복 binding (BIND-1) | REAL_RUNTIME_VERIFIED | PASS(binding-logic만; 자동 onpageload 미발화는 별개 OBSERVED) | 섹션 17, 18 |
| Static/Edit 정적 value 렌더 (REALRT-2) | REAL_RUNTIME_VERIFIED | PASS(`label=`/`initValue=`) | 섹션 18 |
| CheckBox 값/체크상태/라벨 — unbound, widget/bootstrap semantics (REALRT-2-CheckBox) | REAL_RUNTIME_VERIFIED (WIDGET/BOOTSTRAP SEMANTICS) | PASS — `addItem(value,label)` bootstrap, click→checked→getValue() round-trip 실 엔진 확인(수동 실행) | follow-up 문서 §1 |
| CheckBox — 자동 page-init 발화 (unbound) | - | **AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED** — 생성된 `ev:onpageload` 자동 발화는 확인 못함(무관 함수까지 포함해 script 블록 자체 미실행 관찰) | follow-up 문서 §1, §7 |
| CheckBox 값/체크상태/라벨 — dataset bound (REALRT-2-CheckBox) | - | **OPEN**(안전한 일반화 근거 없음, shipped 참조 사례 없음) | follow-up 문서 §1 |

## 12. Production Recommendation

**ENGINEERING_READY_FOR_PILOT_WITH_KNOWN_GAPS — TARGET RUNTIME(WebSquare) PARTIALLY VERIFIED, TARGET JDK CERTIFICATION PENDING**

Static/mock 기준으로는 Phase4 baseline이 문서화된 수치와 완전히 일치합니다. **Phase1 SHA는 이번 follow-up
라운드에서 정확한 추출 recipe를 확정하고 재현에 성공해 PASS로 해소**되었습니다(follow-up 문서 §4). 화면 구조
측면에서는 135개 XFDL 전수 audit 결과 컨트롤/위치/크기/hierarchy/Dataset이 **148 scanned / 146 PASS / 0
MISMATCH**로 매우 견고하게 보존됨을 확인했습니다(§16, §18). 실 WebSquare 엔진 위에서도 WFrame/TabControl
생성·탭 전환(§14), Static External Tab·Dynamic setUrl·Grid combo/summary band·Dataset 반복 binding·Static/Edit
정적 값 렌더링(§15~§18)까지 다수의 항목을 REAL_RUNTIME_VERIFIED로 확인했고, CheckBox(unbound)도
value/checked/label/click round-trip 기준으로 REAL_RUNTIME_VERIFIED(WIDGET/BOOTSTRAP SEMANTICS)까지 확인했습니다
(자동 page-init 발화는 별도로 AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED, follow-up 문서 §1). 그 과정에서 발견된
converter 결함 **7건**(DEFECT-1, GRID-1, GRID-2, BIND-1의
binding-logic, REALRT-1, REALRT-2-Static/Edit, REALRT-2-CheckBox-unbound)을 **Phase4-derived working
candidate**(`work/phase4-working/...`)에서 최소 수정하고 실 Runtime 재검증까지 완료했습니다 — **Phase4 original
ZIP baseline 자체는 immutable/frozen으로 유지**되며 모든 수정은 그 baseline 위에 별도로 파생된 working
candidate에만 존재합니다.

다만 다음 **제품/Runtime known gap 4건**은 **의도적으로 수정하지 않은 채 남아 있으며, 파일럿 적용 전 반드시
인지해야 합니다**:

1. **DEFECT-2**(`CONTENT_NOT_READY` false-negative, OPEN / CONTRACT_LIMITATION): 동적 `set_url()`을 사용하는
   화면에서 `setUrl()`의 성공 콜백/Promise를 신뢰하는 로직은 실패로 오탐될 수 있음(§15, follow-up 문서 §2에서
   재확인 — 안전한 guarded fallback을 아직 확정하지 못함).
2. **GRID-3**(다중 Format, UNSUPPORTED_SEMANTIC): XPlatform Grid의 `alternate` Format처럼 여러 레이아웃을 전환하는
   화면은 WebSquare gridView 구조상 표현 불가 — 해당 패턴을 쓰는 화면은 수동 재설계가 필요합니다(§16).
3. **REALRT-2-CheckBox — dataset-bound 케이스만 OPEN**: unbound CheckBox는 이번 라운드에 widget/bootstrap
   semantics(value/checked/label/click round-trip) 기준 REAL_RUNTIME_VERIFIED로 해소됐지만(자동 page-init
   발화는 4번 항목 참고), Dataset과 양방향 바인딩되는 CheckBox는 shipped 참조 사례가 없어 여전히 미해결입니다
   — 그런 화면은 파일럿 전 별도 검토가 필요합니다(follow-up 문서 §1).
4. **`ev:onpageload` 자동 발화 신뢰성**(AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED): 일부 페이지/라우트에서 페이지
   진입 시 onload 바인딩이 자동으로 실행되지 않는 현상이 관찰되었으며(§18, follow-up 라운드에서 CheckBox-unbound
   bootstrap으로도 재확인), Defect 2와 유사한 계열이지만 별개 사안으로 미해결 상태입니다.

이와 별도로 **certification blocker 1건**(위 4건과 범주가 다름, 제품 화면 변환 품질 gap이 아니라 target JDK
인증 절차 자체의 blocker):

- **Target JDK 1.8.0_111 — BLOCKED_BY_DISTRIBUTION**: 신뢰 가능한 portable 바이너리를 확보하지 못해 다른 JDK로
  대체하지 않고 이 상태로 유지합니다(follow-up 문서 §3).

이처럼 **핵심 구조/레이아웃과 unbound CheckBox의 widget/bootstrap semantics까지 높은 신뢰도로 검증됐지만, 위
제품/Runtime gap 4건과 별도의 certification blocker 1건이 남아 있는 상태에서는 "결함 없는 인증"으로 표현하지
않습니다.**

## 13. Files Produced

### Core Documents

| 문서 | 상태 | 용도 |
|---|---|---|
| `work/results/FINAL-VERIFICATION-REPORT.md` | CURRENT / MASTER | 전체 프로젝트 최종 통합 판정, release recommendation, Verification Matrix, known gaps/certification blocker (본 문서) |
| `work/results/followup-checkBox-ready-jdk-phase1-final.md` | CURRENT / DETAIL | CheckBox follow-up, Defect 2 contract limitation, Target JDK 1.8.0_111, Phase1 SHA provenance — 4트랙 상세 source of truth |
| `work/results/conversion-quality-audit-final.md` | CURRENT / QUALITY-AUDIT | 135 XFDL 구조 audit, control/geometry/hierarchy, Grid/Dataset/property, 대표 real-runtime 화면 상세 (§11이 최신 판정) |
| `work/results/phase1-sha-findings.md` | HISTORICAL / SUPERSEDED | Phase1 SHA가 UNRESOLVED였던 시점의 조사 기록 보존용 — 최신 판정 source로 사용 금지 |

문서 간 관계: 본 문서(MASTER)가 최종 판정을 통합하고, 근거 상세는 위 DETAIL/QUALITY-AUDIT 문서로 링크한다.
HISTORICAL 문서는 과거 조사 기록을 보존만 하며(삭제하지 않음), 현재 판정에는 쓰이지 않는다.

### Supporting Evidence / Tooling

- `work/audit-scripts/phase1_sha_verifier.py`, `work/audit-scripts/phase1_sha_manifest.json` — Phase1 SHA deterministic verifier(PASS 재현)
- `work/audit-scripts/xfdl_conversion_audit.py` — 135 XFDL 구조 audit 스크립트
- `work/results/kms-smoketest-findings.md` — KMS 단독 실패 원인
- `work/results/wrm-realruntime-findings.md` — WRM Maven 빌드/배포 전 과정 상세
- `work/results/mvn-package-396-v6.log` 외 빌드 로그 다수
- `work/wrm-build/WRM/pom.xml` — work 사본에만 존재하는 수정본 (원본 dev pack 미변경)
- `work/tools/apache-maven-3.9.6/` — 로컬 Maven 설치 (PATH 미등록)
- `work/websquare-devpack-copy/` — Tomcat+JDK11+KMS+WRM+websquare_home+MariaDB 테스트 사본
- `work/results/wrm-wframe-tabcontrol-trace.md` — 아래 §14의 실제 lifecycle trace 원본

## 14. Addendum (2026-08-18) — GET / 404 근본 원인 규명 및 실제 WFrame/TabControl 검증 성공

이전 보고서 작성 시점에서 REAL_RUNTIME_REQUIRED로 남겨두었던 WFrame/TabControl 항목을, 사용자 지시에 따라
`GET /` 404의 근본 원인을 실제 DEBUG 로그로 추적해 해결하고, 실제 WFrame/TabControl까지 도달했습니다.

### 근본 원인 규명 (실측)
work 사본의 `log4j.xml`(WRM 자체 로깅 설정, 코드 아님)에 `org.springframework.web`을 DEBUG로 켜서 확인한 결과:

- `"/"` 매핑은 정상 등록됨: `Mapped "{[/],methods=[GET]}" onto ... InitController.IndexBase` (Case B 아님)
- DispatcherServlet이 실제로 `InitController.IndexBase`를 정상 호출함 (Case A 아님)
- 호출 도중 **실제 예외 발생** (Case C, 확정):
  ```
  org.springframework.transaction.CannotCreateTransactionException: ...
  Caused by: java.sql.SQLNonTransientConnectionException: Could not connect to address=(host=127.0.0.1)(port=3306)...
  Connection refused: connect
  	at com.inswave.wrm.common.controller.InitController.IndexBase(InitController.java:28)
  ```
- **진짜 원인: dev pack에 번들된 MariaDB(`bin/mariadb-10.5.8-winx64`)가 기동되어 있지 않았음.** WRM 코드/설정 결함이
  아니라 단순 인프라 미기동.

### 조치 (work 사본에만, 원본 dev pack 미변경, 새 DB 설치 아님, 시스템 서비스 미등록)
1. `bin/mariadb-10.5.8-winx64`(기존 데이터 파일 포함)를 `work/websquare-devpack-copy/mariadb`로 복사
2. `mysqld.exe --datadir=<copy>\data --port=3306 --console` 직접 실행 (서비스 등록 없음) → `ready for connections`
3. 번들된 `mysql.exe` 클라이언트로 WRM 앱이 실제 사용하는 자격증명(`db.properties`: `APP`/`APP`/`WRM`)으로 접속
   확인 → `SELECT 1` 성공
4. Tomcat 재기동 → `GET /` → **HTTP 200**, 실제 로그인 폼 HTML(838 bytes) 렌더링 확인

### 실제 WFrame/TabControl 검증 (브라우저 실측, mock 아님)
1. dev pack DB에 이미 시딩되어 있던 실제 테스트 계정(`HM_MEMBER_BASIC.EMP_CD=<DEV_PACK_TEST_CREDENTIAL_REDACTED>`,
   `PASSWORD=<DEV_PACK_TEST_CREDENTIAL_REDACTED>`)으로 브라우저에서 직접 ID/PW 입력 후 로그인 버튼 클릭 →
   서버 응답 `{"statusMsg":"로그인 성공","statusCode":"S"}`
2. 로그인 후 메인 화면에서 **실제 WFrame 4개** 확인: `mf_wfm_header`, `mf_tac_layout_contents_MAIN_body`,
   `mf_wfm_side`, `mf_wfm_footer` (모두 class `w2wframe`)
3. **실제 TabControl** 확인: `mf_tac_layout` (class `w2tabcontrol tabc_layout`), 활성 탭
   `mf_tac_layout_tab_MAIN` (`w2tabcontrol_active w2tabcontrol_selected`)
4. DB에서 로드된 실제 메뉴 트리(업무화면>시스템>코드관리/권한관리/...) 중 "코드관리" 메뉴를 실제 클릭
5. **실제 tab lifecycle 이벤트 관찰** (클릭 전/후 DOM 직접 비교):
   - 클릭 전: `mf_tac_layout_tab_MAIN` = `...w2tabcontrol_active w2tabcontrol_selected`
   - 클릭 후: `mf_tac_layout_tab_MAIN`에서 active/selected 클래스 **제거됨**; 새 탭
     `mf_tac_layout_tab_001001001`이 `w2tabcontrol_active w2tabcontrol_selected`로 **생성됨**
   - 새 탭 내부에 **중첩 WFrame**까지 생성됨: `mf_tac_layout_contents_001001001_body`
     (class `w2wframe w2tabContainer_contents ...`), 그 안에 `..._body_wfm_header`까지 확인

이것으로 원래 과제(섹션 16)의 I(selected metadata), J(tab switching state) 항목이 **실제 관찰 증거로
REAL_RUNTIME_VERIFIED** 됩니다. 나머지 항목(setSrc lifecycle, rapid setUrl, remove during load, Parent/Owner/
Opener depth, same-XML multi-instance, XJS isolation)과 **Phase4 변환 결과물을 이 실제 런타임에 배포해 관찰하는
것**은 이번 세션에서 다루지 않았으며 후속 과제로 남습니다.

Production converter 소스, 원본 dev pack 모두 이번 추가 조치에서도 미수정. 상세 원본 trace는
`work/results/wrm-wframe-tabcontrol-trace.md` 참고.

## 15. Dynamic setUrl 실 Runtime 검증 — Defect 1 발견·수정, Defect 2 재현 (2026-08-18)

Phase4 fixture를 Static External Tab → Dynamic setUrl 순서로 실 Runtime에 배포해 검증하는 과정에서 실제
defect 2건을 확인. 상세 근거는 `work/results/defect1-defect2-findings.md` 참고. 요약:

| Defect | 상태 | 판정 |
|---|---|---|
| Defect 1: 초기 url 없는 runtime `set_url()` 대상 Tabpage가 WFrame이 아니어서 `setSrc is not a function` | **Production 수정 완료** | REAL_RUNTIME_VERIFIED |
| Defect 2: 콘텐츠는 정상 로드되지만 `ev:onpageload`가 동적 `.setSrc()` 경로에서 발생하지 않아 `CONTENT_NOT_READY`로 오탐 | **재현·원인 확정, 수정 미적용(사용자 지시)** | REAL_RUNTIME_VERIFIED (재현) |

### Defect 1 수정 요약
`WebSquareGenerator.java`의 `convertTab()`에 `isRuntimeSetUrlTarget()` 헬퍼를 추가해, 초기 `url`이 없고
`tabRuntimePlan`상 실제 `SET_URL` 대상인 Tabpage에만 `frameMode="wframe" scope="true"`를 부여하도록 수정.
Static External Tab, 일반 inline/static Tab, Runtime Adapter의 `openAction`은 변경하지 않음. pristine
소스 재컴파일 output과의 diff로 **정확히 9개 파일, 파일당 1줄**만 변경됨을 확인. 실 엔진 재검증에서
`setSrc` TypeError 소멸 + `Search.xfdl` UI 실제 렌더링(`btnSave`, `edtKeyword`) 확인.

### Defect 2 재현 요약
Defect 1 수정 적용 상태에서 Runtime Adapter의 `setUrl()`이 반환하는 Promise에 직접 `.then()/.catch()`를
연결해 호출한 결과, `state:"LOADED"`이고 UI가 실제로 정상 렌더링된 상태에서도 Promise가
`CONTENT_NOT_READY`로 reject됨을 확인(false-negative). 원인: 생성된 자식 페이지(`Search.xml`)의
`<body ev:onpageload="scwin.__xpws_onpageload">` 바인딩은 문법적으로 WRM 자체가 쓰는 것과 동일한 정상
패턴이지만, 실 엔진이 `.setSrc()`로 동적 교체된 WFrame 콘텐츠에 대해서는 이 이벤트를 발생시키지 않음(핸들러를
수동 호출하면 정상 동작 확인). 즉 Runtime Adapter의 READY 계약이 전제하는 이벤트가 이 경로에서 실제로
발생하지 않는 것이 근본 원인. 수정 후보(폴백 신호 추가 등)는 `defect1-defect2-findings.md`에 제시했으나
**적용하지 않음** — Defect 1과 분리 유지, 추가 실 Runtime 조사 없이 최종 확정 불가.

### 전체 재-회귀 (Production 소스 변경에 따른 필수 재검증)
Defect 1 수정 반영 빌드로 재실행, 전부 기존 baseline과 동일한 수치로 재통과:

| 항목 | 결과 |
|---|---|
| 전체 변환 (149개) | 149/149 PASS |
| XML parse (136개) | 136/136 PASS |
| Page 내장 JS 구문 (136개) | 136/136 PASS |
| Standalone JS 구문 (15개) | 15/15 PASS |
| Runtime finalization mock | 41/41 PASS |
| Lifecycle mock | 14/14 PASS |
| Phase1 SHA-256 (Sample.xfdl/CommentProtection.xfdl) | UNRESOLVED 유지 (강제 PASS 처리하지 않음) |
| WRM 로그인→WFrame/TabControl 실 Runtime 재확인 | 변경 없음, 정상 (WFrame 4개, TabControl active/selected 동일) |
| Static External Tab fixture 실 Runtime 재확인 | 변경 없음, 정상 |

Production 소스 변경분은 이번 §15의 Defect 1 수정(`WebSquareGenerator.java`, 9개 출력 파일 영향)이 유일함.

## 16. 화면 변환 품질(구조/레이아웃) 전수 Audit + Grid 결함 수정 (2026-08-18, 자율 진행)

135개 XFDL 전체를 XPlatform 소스와 WebSquare 생성 XML로 자동 구조 비교(`work/audit-scripts/xfdl_conversion_audit.py`,
신규 work 임시 스크립트)하고, 대표 화면 5개를 실 WebSquare Runtime에서 DOM 기준으로 검증했습니다. 상세 근거는
`work/results/conversion-quality-audit-final.md` 참고. 이 절은 §1(Overall Result)·§8(Bugs Found)·§9(Bugs Fixed)의
연장선이며 최신 상태를 반영합니다.

### 자동 Audit 최종 수치

| 항목 | TOTAL | PASS | MISMATCH | UNSUPPORTED |
|---|---|---|---|---|
| 일반 컨트롤 (id/type/geometry/hierarchy/property) | 148 | 146 | 0 | 2 (`FileDownload`, 문서화된 의도적 미지원) |
| Grid 구조/바인딩 | 3 | 2 | 0 | 1 (다중 Format, UNSUPPORTED_SEMANTIC) |
| Dataset/Column | 13 | 13 | 0 | - |

control 누락/부모 변경/Div 이탈/위치·크기 손실/잘못된 type 변환은 **0건**.

### Grid 결함 3건 최종 상태

| ID | 내용 | 상태 |
|---|---|---|
| GRID-1 | combo binding(TYPE 컬럼 → dsCodes) 손실 | **FIXED**, `[GridFormatConverter] resolveInputType`/`applyCellPresentation`/신규 `appendComboChoices` — 실 shipped KMS 샘플의 `inputType="select"` + `w2:choices/w2:itemset` 구조를 그대로 재사용. 이 시점엔 구조만 REAL_RUNTIME_VERIFIED였고 데이터 상호작용은 미확인이었으나, **§18에서 work/browser 진단 데이터로 dropdown open/select/CODE↔NAME 왕복까지 확인해 구조+interaction 둘 다 REAL_RUNTIME_VERIFIED로 최종 확정**(historical: 아래 원문은 당시 시점 기록) |
| GRID-2 | summary band(`Band id="summ"`) 손실 | **FIXED**, `[GridFormatParser]` summCells 라우팅 + `[GridFormatConverter]` 신규 `appendFooter` — 실 shipped KMS 샘플의 `<w2:footer>` 구조 재사용. REAL_RUNTIME_VERIFIED(DOM에서 footer/colspan=3/text="summary" 실측 확인) |
| GRID-3 | 다중 Format(default/alternate) | **UNSUPPORTED_SEMANTIC 확정** — dev pack 문서·전체 shipped 샘플에서 선언적 다중 Format 구조를 찾지 못해 임의 구현하지 않음. 기존 콘솔 TODO 로그로 이미 명시적으로 보고 중(조용한 손실 아님). Production 미수정 |

수정은 `WebSquareGenerator.java`가 아니라 Grid 전용 변환기 `GridFormatParser.java`/`GridFormatConverter.java`에서만
이뤄졌고, output diff는 `Form/GridAdvancedPhase3.xml` **1개 파일만** 변경(EXPECTED_CHANGE, 나머지 134개 fixture
byte-identical) — Defect 1 fix 범위와 완전히 분리됨.

### 대표 5개 화면 Real Runtime DOM 검증

`NestedContainer.xfdl`(nested Div), `Form/Main/TabExternalRelativePath.xfdl`(Div+Tab 컨테이너), `GridAdvancedPhase3.xfdl`
(complex Grid), `DatasetBinding.xfdl`(Dataset/binding), `ControlPropertyMatrix.xfdl`(일반 컨트롤 16종) — 5개 전부 실제
로드·렌더 성공, 위치/크기/parent-child DOM 관계는 5/5 정확. (sample project 전체에 "Div+Grid" 조합 fixture가 존재하지
않아 해당 유형은 대표에 포함 불가 — 정직하게 기록, fixture 하드코딩/조작 없음.)

### 신규 발견 OPEN 항목 (Grid와 무관, 발견 당시 미수정 — **historical, 최종 상태는 §17/§18 참고**)

Real-runtime 검증 과정에서 static-only audit로는 보이지 않던 항목 3건을 추가 발견 — 범위가 크게 확장되어(XForms
모델 바인딩 아키텍처 전반) 이번 세션 목표(Grid 3건)와 분리해 당시 OPEN으로만 기록했음(아래는 발견 시점 기록):

- **BIND-1**: `<Bind><BindItem>`으로 반복 dataList(`dsMain`)의 특정 row에 바인딩된 `xf:input`이 실 Runtime에서
  빈 값으로 렌더(`ref="data:dsMain.NAME"`가 row 인덱스를 특정하지 못하는 것으로 추정).
- **REALRT-1**: `xf:selectBoolean`(CheckBox)이 실 Runtime DOM에서 완전히 사라짐(다른 15개 컨트롤은 정상 렌더).
- **REALRT-2**: `xf:input`/`w2:span`의 정적 `value` 속성이 실 Runtime에서 반영되지 않음(반면 `xf:trigger`의 정적
  value는 정상 표시) — 모델 `ref` 바인딩 여부에 따라 갈리는 것으로 보이나 미확정.

### 전체 Regression (Grid 수정 반영)

149/149 변환, 136/136 XML parse, 136/136 page JS, 15/15 standalone JS, 41/41 runtime mock, 14/14 lifecycle mock —
전부 재통과. Phase1 SHA는 UNRESOLVED 유지, Target JDK는 TARGET_JDK_RUNTIME_REQUIRED 유지. WRM WFrame/TabControl,
Static External Tab, Dynamic setUrl(Defect 1) 실 Runtime 재확인 결과 전부 변경 없음. **Defect 2(CONTENT_NOT_READY)는
이번 세션에서 손대지 않았고 여전히 OPEN**(재확인 시 동일하게 재현됨).

### §1/§8/§9 갱신 요약 (stale 정리)

- Production source: Phase4 original ZIP baseline은 여전히 IMMUTABLE. Working candidate에는 이제 Defect 1(§15) +
  GRID-1/GRID-2/BIND-1/REALRT-1/REALRT-2-Static·Edit(§16~§18) 수정이 존재. Defect 2, GRID-3, REALRT-2-CheckBox는
  의도적으로 미수정(OPEN/UNSUPPORTED_SEMANTIC) — 최신 상태는 §18 참고.
- Bugs Fixed: Defect 1, GRID-1(구조+interaction), GRID-2, BIND-1(binding-logic), REALRT-1, REALRT-2-Static,
  REALRT-2-Edit — 총 7건.
- Bugs Found 중 미해결: Defect 2(OPEN), GRID-3(UNSUPPORTED_SEMANTIC), REALRT-2-CheckBox(OPEN), BIND-1의 자동
  onpageload 미발화(OBSERVED, 별개 관찰).

## 17. BIND-1 / REALRT-1 / REALRT-2 처리 (2026-08-18, §16 후속, 자율 진행)

§16에서 Grid와 무관하게 신규 발견된 3건을 사용자 지시에 따라 끝까지 자율 처리. 상세 근거는
`work/results/conversion-quality-audit-final.md` §4 참고. 요약:

| ID | 내용 | 상태 |
|---|---|---|
| BIND-1 | `<Bind><BindItem>`이 반복 `w2:dataList`의 특정 row를 가리키지 못해 값이 빈 채로 렌더 | **FIXED** — `[WebSquareGenerator] applyBindings`가 스칼라 value 바인딩이 걸린 데이터셋마다 `{datasetId}.setRowPosition(0);` 부트스트랩을 1회 추가(신규 필드 `rowPositionBootstrapped`). 실 엔진의 `w2:dataList.setRowPosition()`(비공식이나 실존 확인) 사용. REAL_RUNTIME_VERIFIED(`edtName` 값이 "Alpha"로 정상 렌더 확인) |
| REALRT-1 | `xf:selectBoolean`(CheckBox)이 실 Runtime DOM에서 완전히 사라짐 | **FIXED** — `xf:selectBoolean`은 real WebSquare 문서/샘플 어디에도 없는 태그였음. `[ComponentMappingRegistry]`의 CheckBox 매핑을 실제 존재하는 `w2:checkbox`로 정정(shipped KMS 샘플로 검증). REAL_RUNTIME_VERIFIED(컨트롤이 DOM에 다시 나타남) — 단 내부 체크 상태/라벨 표시는 REALRT-2와 같은 근본 원인 계열로 남아 있음(중복 항목 생성 안 함) |
| REALRT-2 | 정적 `value` 속성이 `xf:input`/`w2:span` 등 XForms 데이터 컨트롤에서 실 렌더에 반영 안 됨 | 이 시점엔 전체 OPEN으로 기록(근본 수정이 `copyBasicProperties` 전체 재설계급이라 판단해 보류). **§18에서 범위를 Static(`w2:span`)/Edit(`xf:input`)/CheckBox로 좁혀 재조사한 결과 Static/Edit는 shipped 문서에 정확히 명시된 대체 속성(`label=`/`initValue=`)이 있어 FIXED, REAL_RUNTIME_VERIFIED로 확정. CheckBox만 `ref=` 바인딩+별도 label 요소가 추가로 필요해 OPEN 유지**(historical: 아래 원문은 당시 시점 기록) |

output diff: `DatasetBinding.xml`(1줄 추가), `ControlPropertyMatrix.xml`(태그명 1곳) — 이 2개 파일만 영향, 나머지
133개 fixture byte-identical.

### Round 2 이후 전체 Regression

149/149 변환, 136/136 XML parse, 136/136 page JS, 15/15 standalone JS, 41/41 runtime mock, 14/14 lifecycle mock —
전부 재통과. WRM WFrame/TabControl, Static External Tab, Dynamic setUrl(Defect 1), GridAdvancedPhase3 실 Runtime
재확인 전부 변경 없음. **Defect 2는 이번에도 손대지 않았고 OPEN 유지**(재확인 시 동일 재현). Phase1 SHA는
UNRESOLVED, Target JDK는 TARGET_JDK_RUNTIME_REQUIRED 그대로 유지.

### Grid audit 지표 표기 정정

Round 1 진행 중 한때 Grid audit이 "TOTAL=5"로 표시된 적이 있었는데, 이는 finding row 수(Grid element당 발견된
문제 개수만큼 누적, 최대 5 = PASS 2 + MISMATCH 3)였을 뿐 project의 실제 Grid element 개수(3개)와는 다른
지표였습니다. GRID-1/2 수정 후 finding row 수가 우연히 3(PASS 2 + UNSUPPORTED 1)으로 줄어 element 개수와
같아졌습니다 — 혼동 방지를 위해 audit 스크립트 출력을 "Grid elements scanned: 3"과 "Finding rows: TOTAL=3 ..."
으로 명칭을 분리했습니다.

## 18. REALRT-2 최소 fix(Static/Edit) + GRID-1 interaction 검증 (2026-08-18, §17 후속, 자율 진행)

상세 근거는 `work/results/conversion-quality-audit-final.md` §4 참고. 요약:

**REALRT-2**: Static/Edit/CheckBox 3종을 shipped WebSquare 공식 문서 attribute 목록 + KMS 샘플로 개별 조사한 결과,
`w2:span`은 `label`, `xf:input`은 `initValue`가 실제 정적 표시값 속성(둘 다 공식 문서에 그 용도로 명시, 반면
"value"는 이 두 태그에 대해 문서화조차 안 되어 있었음)임을 확인. `[WebSquareGenerator] copyBasicProperties`를
target tag별로 분기해 이 두 경우만 최소 수정 — **Static/Edit는 FIXED, REAL_RUNTIME_VERIFIED**(`sta.textContent`,
`edt.value`, `mask.value`, 중첩 PopupDiv 안의 span까지 전부 실제 DOM에서 확인). CheckBox(`w2:checkbox`)는 공식
문서상 `value`가 `renderType="native"`일 때만 적용되고 그마저도 라벨은 별도 요소로 만들어야 한다는 제약이 있어
단일 attribute 교체로 해결되지 않음을 확인 — **OPEN 유지, DOM 존재만으로 semantic PASS 처리하지 않음**.

**GRID-1**: work/browser 진단 API(`dsCodes`/`dsMain`에 `setCellData`로 CODE/NAME/TYPE 값 주입 — Production
fixture 데이터는 미수정)로 interaction까지 끝까지 검증. 실제 그리드 셀에 `dblclick` dispatch → 진짜 combobox
드롭다운이 열림(`w2selectbox_open`) → 아이템 목록에 "TypeAlpha"/"TypeBeta" 정상 표시 → "TypeBeta" 클릭 →
`dsMain` row 값이 `CODE="B2"`로, 셀 표시가 "TypeBeta"로 즉시 갱신 — CODE↔NAME 양방향 매핑까지 실제 동작 확인.
**GRID-1은 이제 구조(STRUCTURE) + 상호작용(INTERACTION) 둘 다 REAL_RUNTIME_VERIFIED로 완전 승격.**

**BIND-1 표기 분리**: binding 로직(`setRowPosition(0)`) 자체는 FIXED/REAL_RUNTIME_VERIFIED로 재확인. 다만
`DatasetBinding.xml`은 최상위 popup 페이지인데도 `ev:onpageload`가 네비게이션 직후 자동으로 발화하지 않아
검증은 수동 트리거로 수행했음을 명확히 표기 — 이 자동 미발화 자체는 Defect 2와 유사 계열이나 별개의 관찰
사항으로, 이번에도 수정하지 않음.

### 최종 output diff (3 라운드 누적)

17개 파일 변경(`GridAdvancedPhase3.xml`, `DatasetBinding.xml`, `ControlPropertyMatrix.xml` + Static/Edit을 포함한
Tab 계열 fixture 13개). 나머지 118개 fixture byte-identical. 전수 diff 검토로 각 파일이 의도한 속성/구조 변화만
포함함을 확인(UNEXPECTED_CHANGE 0건).

### 최종 Regression (1회 수행)

149/149 변환, 136/136 XML parse, 136/136 page JS, 15/15 standalone JS, 41/41 runtime mock, 14/14 lifecycle mock —
전부 재통과. WRM WFrame/TabControl, Static External Tab, Dynamic setUrl(Defect 1), GridAdvancedPhase3 실 Runtime
재확인 전부 변경 없음. **Defect 2, Target JDK 1.8.0_111, Phase1 SHA-256은 이번 세션에서 전혀 건드리지 않았고
각각 OPEN / TARGET_JDK_RUNTIME_REQUIRED / UNRESOLVED 그대로 유지.**

### controls 최종 수치

**controls = 148 scanned / 146 PASS / 2 UNSUPPORTED / 0 MISMATCH**
