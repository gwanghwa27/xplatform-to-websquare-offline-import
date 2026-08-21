# Follow-up 1~4 Final Report — 2026-08-18

이 문서는 FINAL-VERIFICATION-REPORT.md §18 이후 진행된 4개 독립 후속 트랙(CheckBox / Defect 2 / Target JDK
1.8.0_111 / Phase1 SHA)의 최종 결과를 기록한다. 각 트랙은 독립적으로 진행했고, 한 트랙이 BLOCKED/OPEN이어도
나머지 트랙 진행에 영향 주지 않았다(사용자 지시).

---

## 1. CheckBox value / checked / label — **PARTIAL (unbound: REAL_RUNTIME_VERIFIED [WIDGET/BOOTSTRAP SEMANTICS] + AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED, bound: OPEN)**

### 1-A. Source semantic 조사
`sample-phase3-project` 전체(135 XFDL)에서 `CheckBox` 사용례는 **정확히 1건**뿐:
```xml
<CheckBox id="chk" text="Use" left="440" top="120" width="100" height="24"/>
```
(`ControlPropertyMatrix.xfdl`) — `value`/`truevalue`/`falsevalue`/`checked` 계열 속성, BindItem, onclick/onchanged
전부 없음. 즉 소스 corpus에는 **unbound, text-only** 사용 패턴만 존재하며, bound CheckBox 또는 초기
checked=true 패턴을 일반화할 근거가 fixture corpus 안에 없음(추측 금지 원칙에 따라 이 갭은 그대로 보고).

### 1-B. WebSquare 실제 규칙 조사 (문서/샘플/실 엔진 기준, 추측 없음)
- 전체 dev pack(`work/websquare-devpack-copy`, KMS/WRM/ROOT 포함)에서 `w2:checkbox` 문자열이 등장하는 실제
  shipped 사례는 34건이며, **전부 `w2:tree`의 itemset 자식 스키마(`<w2:checkbox ref=""/>`, boolean flag)**이고,
  standalone 폼 컨트롤로 쓰인 shipped 예시는 **0건**(우리 변환기 자체 출력 제외). 즉 standalone `w2:checkbox`
  바인딩 관례를 보여주는 공식 문서/샘플/실 프로덕션 화면이 dev pack 안에 전혀 없음.
- 반대로 "체크박스 그룹"의 공식 권장 패턴은 `xf:select renderType="checkboxgroup"` + `xf:choices`/`xf:item`
  또는 `xf:itemset`(`ROOT/cm/template/snippets/10_입력폼/10_06 Checkbox.xml`, `KMS/checkBox-002/checkBox_sample.xml`)
  — 하지만 이는 XPlatform CheckBox(단일 boolean)와 의미가 다른 다중 항목 그룹 위젯이라 그대로 재사용 불가.
- 그래서 `w2:checkbox`(`ComponentMappingRegistry`가 이미 매핑 중인 대상)가 real 엔진에서 실제로 무엇을 하는
  위젯인지 **문서 대신 real 엔진 자체의 `getConfiguredOptions()`로 직접 확인**(추측 아님, live evidence):
  ```json
  {"pluginType":"uiplugin.checkbox","pluginName":"checkbox",
   "userEvents":["onchange","onlabelclick","oncheckboxclick","onviewchange","onkeyup","onkeydown"],
   "value":"","ref":"","selectedindex":-1,"falseValue":"","title":"","useCheckboxTitle":true, ...}
  ```
  그리고 prototype 메서드 목록에서 `addItem/deleteItem/getCheckboxList/getLabelList/setSelectedIndex/
  getSelectedIndex/setValue/getValue/checkAll/setRef/unbindRef` 확인.

### 1-C. 실 Runtime 진단 (실 브라우저, Production fixture 수정 없이 먼저 진단)
`ControlPropertyMatrix.xml`(이미 배포된 실 fixture, `mf_chk`)에서 직접 확인:
1. **수정 전 실제 렌더링**: `<div class="w2checkbox"><table class="w2checkbox_main"></table></div>` — 완전히
   빈 테이블. `<input>`도 `<label>`도 없음. `getValue()`는 항상 `""`.
2. `setProperty('title'/'value'/'selectedindex', ...) + redraw()`만으로는 아무 것도 렌더링되지 않음(정적
   속성은 렌더링에 전혀 관여하지 않음 — real 엔진으로 직접 확인).
3. `mf_chk.addItem("Use", "Y")` 호출 → 실제 `<input type="checkbox">` + `<label>` 행이 생성됨, `getValue()`가
   즉시 두 번째 인자(label) 문맥이 아니라 **첫 번째 인자(value)** 를 따름 → `addItem(value, label)` 시그니처
   확정.
4. 렌더된 `<input>`을 실제 클릭 → `checked=true`, `getValue()`가 즉시 `"Use"`(설정한 value)로 바뀜.
   `setSelectedIndex(-1)` → 다시 unchecked, `getValue()`가 다시 `""`. **양방향 round-trip 확인.**

### 1-D. 적용한 최소 수정
**[WebSquareGenerator] copyBasicProperties** — `w2:checkbox` 대상 분기 신규 추가:
- 기존 "else" 분기(`target.setAttribute("value", text)`)는 real 엔진이 렌더링에 전혀 쓰지 않는 죽은 속성이라
  제거.
- 대신 BIND-1과 동일한 page-init bootstrap 채널(`pageLoadStatements` → `scwin.__xpws_initBindings` →
  `ev:onpageload="scwin.__xpws_onpageload"`)에 `<targetId>.addItem("<value>", "<label>")` 호출을 추가.
  `value` = XPlatform CheckBox의 `value` 속성(있으면), 없으면 `text`로 폴백. `label` = `text`.
- fixture corpus에 checked-초기값을 나타내는 XPlatform 속성이 전혀 없으므로 `setSelectedIndex(0)` 호출은
  추가하지 않음(안전한 일반화 근거 없음 → 기존 기본 unchecked 동작 그대로 보존).
- 원인/흐름/영향 범위/JDK 영향: 원인은 1-C에서 확정한 "정적 value/label 속성이 렌더링에 관여하지 않음"이라는
  real 엔진 동작. caller는 `convertChildren()`이 각 컨트롤마다 호출하는 `copyBasicProperties(src, target)` →
  `target.getTagName()`으로 `w2:checkbox`만 분기. 영향 범위는 `w2:checkbox`로 매핑되는 컨트롤(현재 corpus에서는
  `chk` 1개)로 완전히 국한 — Static/Edit/Button 등 다른 분기는 무변경. JDK 영향 없음(순수 문자열/DOM API, Java 8
  문법 범위 내 `StringBuilder`/`Set` 등 기존 스타일 재사용, 신규 문법 없음).

### 1-E. 성공 기준 대비 결과
- Unbound CheckBox(유일한 corpus 사례) — **판정을 두 축으로 분리**(과대승격 방지):
  - **REAL_RUNTIME_VERIFIED (WIDGET/BOOTSTRAP SEMANTICS)**: `addItem(value,label)` 호출, 실제
    `<input type="checkbox">`+`<label>` 생성, click→checked→`getValue()` round-trip을 실 엔진에서 확인. 근거:
    (a) wpack 컴파일된 실제 번들 안에 정확한 코드가 그대로 들어있음을 직접 확인(생성 로직 정합성), (b) 그
    정확히 동일한 문자열의 스크립트를 실 엔진의 살아있는 위젯 인스턴스에 대해 **수동 실행**해 렌더링/round-trip
    을 확인(위젯 동작 정합성).
  - **AUTO_PAGE_INIT_NOT_VERIFIED / OBSERVED**: 생성된 `ev:onpageload`가 페이지 진입 시 **자동으로 발화하는지는
    확인하지 못함**. 이번 세션에서 `/popup?w2xPath=...` direct-preview 라우트는 `<script>` 블록(우리 addItem
    bootstrap 포함, 기존 무관 함수 `xpTransaction`/`prop`도 동일하게) 자체가 자동 실행되지 않는 현상을 재확인
    했다(fresh tab으로 재확인, false positive 아님). 이는 이번 세션에서 새로 발견한 회귀가 아니라, 이미
    문서화된 "BIND-1 auto onpageload 미발화" 이슈와 **동일 계열의 기존 환경 한계**이며, `pageLoadStatements`
    채널을 쓰는 다른 모든 기존 fix(BIND-1, GRID-1 setNodeSet)에도 동일하게 적용되는 제약이다. 즉 "생성된 코드가
    실행되면 올바르게 동작한다"는 fabrication 없이 확인했지만, "항상 자동 실행되는가"는 별도로 미확인 상태로
    남겨 §10 BIND-1 onpageload 관찰과 동일 계열로 귀속시킨다.
- Bound CheckBox(Dataset ↔ checkbox) — **OPEN**. corpus에 bound CheckBox 사례가 전혀 없고, dev pack에도
  standalone bound `w2:checkbox` shipped 예시가 전혀 없어 "추측 없이" 일반화할 근거가 없음. `itemsetObj`/
  `setNodeSet`류 API가 위젯 내부에 존재하는 것은 확인했으나(1-B), 실제 XML 선언 형태·컬럼 스키마 요구사항을
  검증할 shipped 참조가 없어 Production 반영하지 않음.

**최종 판정: CheckBox = PARTIAL** (unbound = REAL_RUNTIME_VERIFIED[WIDGET/BOOTSTRAP SEMANTICS] +
AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED, dataset bound 케이스 = OPEN, 안전한 일반화 근거 없음).

---

## 2. Defect 2 — CONTENT_NOT_READY — **OPEN / CONTRACT_LIMITATION (변경 없음)**

이전 세션(`work/results/defect1-defect2-findings.md`)에서 이미 재현·원인을 확정한 상태였음:
- 재현: `mf_scwin.__xpTabRuntime.setUrl` 반환 Promise가 `CONTENT_NOT_READY`로 reject되지만, 그 시점에
  `Search.xfdl` 변환 UI는 이미 실제로 정상 렌더링되어 있음(false-negative).
- 원인: 실 WebSquare 5.0.5 SP5 엔진이, Runtime Adapter가 동적으로 `.setSrc()`를 호출해 WFrame 콘텐츠를
  교체하는 경로에서는 `<body ev:onpageload="...">` 이벤트를 발생시키지 않음(자식 스스로 READY를 알리는
  기존 계약이 이 경로에서 전제 자체가 깨짐).
- 제안된 후보 3가지 중 "폴링 폴백 추가"가 원칙적으로 가장 안전하다고 판단됐으나, 정확한 2차 signal을 특정하지
  못해 미적용 상태였음.

### 이번 세션 재확인 및 결론
이번 세션에서 동일한 `/popup?w2xPath=...` direct-preview 라우트로 여러 fixture를 반복 로드하며 **`ev:onpageload`
자체가 이 라우트에서는 전혀 자동 발화하지 않는다는 것을 다른 fixture(`ControlPropertyMatrix`)에서도 독립적으로
재확인**했다(§1-E 참고). 즉 "동적 setSrc 경로에서만 onpageload가 안 뜬다"는 기존 결론에 더해, **이 direct-preview
라우트 자체도 onpageload가 자동 발화하지 않는 별도 요인**이 섞여 있을 가능성이 있어, guarded fallback(candidate
2: `.setSrc()` 직후 어댑터가 명시적으로 1회 `__xpws_onpageload` 호출)의 안전성을 이번 세션에서 요구된 8개
실 Runtime 시나리오(sync/async child, rapid setUrl, remove-during-load, exception 등) 전부에 대해 검증하기에는
근거가 더 필요해졌다 — 즉 **불확실성이 줄지 않고 오히려 "정상 경로에서도 onpageload가 항상 보장되지 않을 수
있다"는 방향으로 늘었다**.

**판정 규칙 적용**: "안전한 contract 미확정 → OPEN / CONTRACT_LIMITATION". 이번 세션에서 Production
(`TabRuntimeScriptGenerator` 등)에 어떠한 수정도 적용하지 않았다. Defect 1과 마찬가지로 재현·원인 분석은
이미 완료된 상태를 유지하며, 수정 후보 3가지도 그대로 후보로 남긴다(문서 갱신만, 코드 변경 없음).

**최종 판정: DEFECT-2 = OPEN / CONTRACT_LIMITATION** (변경 없음, 세션 내 추가 근거로 재확인만 수행).

---

## 3. Target JDK 1.8.0_111 — **BLOCKED_BY_DISTRIBUTION**

### 3-A. 로컬 환경 검색 (완료)
`JAVA_HOME`/`PATH`/`<LOCAL_PATH_REDACTED>\.jdks`/`<LOCAL_PATH_REDACTED>\.jdks`/`<LOCAL_PATH_REDACTED>\Downloads`/
`work\tools`/`Program Files\Java`/`Program Files\Eclipse Adoptium`/기존 devpack(`WEBSQUARE_DEV_PACK_SP5_edu`,
JDK11 포함) 전부 확인. `java -version`/`javac -version` 결과:
```
openjdk version "21.0.10" 2026-01-20 LTS
Temurin-21.0.10+7 (java.exe / javac.exe 동일 버전)
```
그 외 설치된 JDK: temurin-11.0.30, temurin-21.0.11, openjdk-26/26.0.1, devpack 내장 JDK11 — **1.8.0_111 exact
build는 어디에도 없음**.

### 3-B. exact JDK 미확보 시 온라인 조사 (완료)
- Internet Archive(`archive.org/details/Java-Archive`)에 8u111의 Linux/Solaris/macOS(JDK), Windows(JRE만) 아카이브는
  있으나 **순수 Windows x64 JDK 아카이브(설치 프로그램이 아닌 형태)는 없음**. Windows용으로 유일하게 존재하는
  8u111 관련 파일은 `jdk-8u111-nb-8_2-windows-x64.exe`(NetBeans 8.2 번들 설치형 EXE, 326MB) — **portable
  archive가 아니라 설치 프로그램**이고 NetBeans까지 함께 설치되므로 "portable/archive 형태만 work/에 배치,
  설치 프로그램 실행 금지" 조건을 충족하지 못함.
- Azul Zulu 아카이브(`cdn.azul.com/zulu/bin/`) 확인 — 8u45/8u51/8u60/8u65/8u66 등은 있으나 **정확히 update
  111에 대응하는 빌드는 없음**(Zulu의 재빌드 캘린더가 Oracle의 update 번호와 1:1 대응하지 않음).
- Oracle 공식 Java SE 8 Archive는 exact 1.8.0_111을 포함하지만 **Oracle 계정 로그인 + 라이선스 동의가
  필수** — 이는 명시적으로 우회 금지된 경로.

### 3-C / 3-D. 결론
신뢰 가능한 provenance(공식 vendor 또는 신뢰 가능한 OpenJDK distribution archive)를 가진 **portable/archive
형태의 정확한 Windows x64 JDK 1.8.0_111 바이너리를 합법적으로 확보하지 못했다.** 설치 프로그램 실행(NetBeans
번들 EXE) 또는 Oracle 로그인 우회는 모두 명시적으로 금지된 경로이므로 시도하지 않았다.

**최종 판정: TARGET JDK 1.8.0_111 = BLOCKED_BY_DISTRIBUTION** (다른 JDK로 대체하지 않음, `--release 8`도
TARGET_JDK_RUNTIME_VERIFIED로 인정하지 않음 — 기존 상태 유지).

---

## 4. Phase1 SHA provenance — **STATIC_VERIFIED / PASS (provenance 확정, 재현 성공)**

### 4-A/4-B. 기존 evidence 검색
전체 프로젝트(Phase1~Phase6 산출물, BAT/SH, Java, markdown 포함)에서 expected hash 문자열/추출 방법을
검색했으나 **extraction recipe를 명시한 스크립트/문서는 존재하지 않음**(`work/results/phase1-sha-findings.md`에
이미 같은 결론 기록됨, 이번 세션에서 grep으로 재확인 — `MessageDigest`/`sha256`/`Get-FileHash` 등도 프로젝트
전체에 관련 스크립트 없음). 과거 "exact PASS" 기록(`PHASE3-REGRESSION-RESULT.md`)은 결과값만 적혀 있고
추출 방법 자체는 기록되어 있지 않아 provenance 증거로 인정하지 않음.

### 4-C. Deterministic candidate matrix (완료, 총 각 34개 조합 × 2 fixture 실행)
`Sample.xfdl`/`CommentProtection.xfdl`을 현재 working candidate 컨버터로 재변환한 뒤(변환기 자체는 미변경,
순수 산출물만 새로 생성), 다음 축의 전 조합에 대해 SHA-256을 계산:
- 추출 소스: source XFDL의 `<Script>` CDATA(raw regex) vs XML parser textContent, **generated 출력 XML의
  `<script>` CDATA(raw)** vs **generated 출력 XML의 XML parser textContent**
- 개행: as-is / CRLF / LF-normalized / strip / strip+CRLF / trailing-newline-normalized / no-trailing-newline
- 인코딩: UTF-8 무BOM / UTF-8 BOM

**일치한 조합**: **생성된 출력 XML(`Sample.xml`/`CommentProtection.xml`)의 모든 `<script>` 요소를 XML
파서로 읽어(itertext) 문서 순서대로 `"\n"`으로 join한 뒤, 끝의 개행을 정확히 1개로 정규화(rstrip 후 `"\n"`
1개 추가)하고 UTF-8(무BOM)로 인코딩 → SHA-256.**

| Fixture | Expected | Actual (recipe 적용) | 결과 |
|---|---|---|---|
| Sample | `f82379cfb619d611ae4137032af43fd10faf3df88f018ca5db3b72c490f4d3fe` | 동일 | **PASS** |
| CommentProtection | `14f3466acde50241698ccf21edec5807464a2a6e903854c78ed59332c7b2b987` | 동일 | **PASS** |

핵심 발견: expected hash는 **source XFDL이 아니라 컨버터가 생성한 출력 XML의 스크립트 콘텐츠**를 대상으로
한 값이었음 — "변환 자체가 스크립트를 손실 없이 재현하는지"를 검증하는 회귀였다는 뜻이며, 이번 working
candidate가 정확히 그 조건을 만족한다.

### 4-D. Deterministic verifier 신규 추가
`work/audit-scripts/phase1_sha_verifier.py` — manifest(JSON, fixture name/출력 XML 경로/expected hash) 입력
방식, fixture 이름 하드코드 없음. UTF-8/expected/actual/byte 수/추출 모드를 모두 출력. Production 컨버터
코드에는 영향 없음(순수 검증 스크립트, Java 소스 변경 없음).
`work/audit-scripts/phase1_sha_manifest.json` — Sample/CommentProtection 2건 매니페스트.

실행 결과(이번 세션, working candidate 산출물 기준):
```
[PASS] Sample: expected=f82379cf...4d3fe actual=f82379cf...4d3fe bytes=1846
[PASS] CommentProtection: expected=14f3466a...2b987 actual=14f3466a...2b987 bytes=1913
```

**최종 판정: Phase1 SHA = STATIC_VERIFIED / PASS** (provenance 확정 + 재현 성공, 기존 UNRESOLVED에서 복구).

---

## 5. Production working-candidate 변경 파일 (이번 세션)

**[WebSquareGenerator] copyBasicProperties** — `w2:checkbox` 전용 분기 추가(§1-D). 유일한 코드 변경.
- 원인: real `w2:checkbox`(uiplugin.checkbox)는 정적 `value`/`label` 속성을 렌더링에 전혀 쓰지 않음(실 엔진
  `getConfiguredOptions()`/DOM 관찰로 확정).
- caller/callee flow: `convertChildren()` → `copyBasicProperties(src, target)`(target.getTagName()으로 분기)
  → `pageLoadStatements`(기존 BIND-1이 만든 동일 채널) → `buildBindingBootstrapScript()` →
  `scwin.__xpws_initBindings`/`scwin.__xpws_onpageload` → `finalizePageLoadBinding()`이 `body`에
  `ev:onpageload="scwin.__xpws_onpageload"` 설정.
- 정확한 삽입 위치: `WebSquareGenerator.java` `copyBasicProperties()` 메서드 내, 기존 `w2:span`/`xf:input`
  분기 옆에 `w2:checkbox` else-if 분기 추가(파일 내 정확한 라인은 diff 참고).
- 영향 범위: `w2:checkbox`로 매핑되는 컨트롤만(현재 corpus 전체 149개 화면 중 `chk` 1개, 파일 1개
  `Form/ControlPropertyMatrix.xml`만 출력 변경 — diff로 전수 확인).
- regression 위험: 낮음. 149/149 conversion, 136/136 XML parse, 136/136 JS syntax(node --check), 15/15
  standalone/common JS 모두 재확인 통과, 변경된 산출물이 정확히 1개 파일로 국한됨을 diff로 확인.
- JDK 1.8.0_111 영향: 없음(문자열/DOM API만 사용, Java 8 문법 범위 내).

Defect 2, Target JDK, Phase1 SHA 트랙에서는 Production 소스(Java)를 전혀 수정하지 않았다(Phase1 SHA는
검증 스크립트만 신규 추가, 컨버터 코드 미변경).

---

## 6. 전체 회귀 (Production 변경 있었으므로 수행)

이번 follow-up 라운드에서 **실제로 재실행한 항목**(REEXECUTED_THIS_ROUND):

| 항목 | 결과 | 상태 |
|---|---|---|
| Java compile | 0 errors | REEXECUTED_THIS_ROUND |
| project conversion | 149/149 | REEXECUTED_THIS_ROUND |
| XML parse | 136/136 | REEXECUTED_THIS_ROUND |
| page JS syntax(`node --check`) | 136/136 | REEXECUTED_THIS_ROUND |
| standalone/common JS(`node --check`) | 15/15 | REEXECUTED_THIS_ROUND |

**재실행하지 않은 항목**(이번 라운드에 41/14를 실제로 실행한 것이 아님을 명시):

| 항목 | 결과 | 상태 |
|---|---|---|
| runtime mock | 41/41 (이전 세션 값) | **PREVIOUS_PASS_RETAINED / NOT_REEXECUTED_THIS_ROUND** |
| lifecycle mock | 14/14 (이전 세션 값) | **PREVIOUS_PASS_RETAINED / NOT_REEXECUTED_THIS_ROUND** |

근거: 이번 세션의 유일한 Production 변경(`w2:checkbox` 분기)이 만든 산출물 diff가 `Form/ControlPropertyMatrix.xml`
정확히 1개 파일로 국한됨을 확인했고, Tab runtime/lifecycle 계열 fixture 산출물 전부는 byte-identical(무변경)임을
diff로 확인했다. 즉 41/41, 14/14의 **입력 산출물 자체가 이번 라운드에 바뀌지 않았다**는 사실이 근거이며, harness
스크립트 자체가 이번 세션에 파일로 남아있지 않아 재실행하지 않았다 — 이전 세션 값을 그대로 유지(RETAINED)하는
것이지, 이번 라운드에 41/41·14/14를 새로 측정한 것이 아니다.

| 항목 | 결과 |
|---|---|
| Production diff (이번 세션 시작 시점 대비) | 정확히 1개 파일 변경 (`Form/ControlPropertyMatrix.xml`) — EXPECTED_CHANGE. UNEXPECTED_CHANGE 없음 |
| Phase4 original ZIP baseline | 무변경(비교만 수행, 수정 없음) |

## 7. Real Runtime smoke 최종 상태
- `ControlPropertyMatrix.xml`의 `w2:checkbox`(`chk`): 생성된 정확한 bootstrap 코드가 wpack 컴파일 번들에
  그대로 포함됨을 확인, 그 코드를 real 엔진의 살아있는 위젯에 실행해 label/checked/value/click round-trip
  확인(§1-E). `/popup?w2xPath=...` 라우트에서 `ev:onpageload` 자동 발화 자체가 이번 세션 내 다른 무관 함수
  (`xpTransaction`)까지 포함해 전혀 일어나지 않는 것을 재확인 — 이는 기존에 이미 OPEN으로 추적 중인
  BIND-1/Defect-2 계열 onpageload 이슈와 동일 계열이며 이번 checkbox 수정이 새로 만든 문제가 아님.
- Static External Tab / WRM WFrame·TabControl 로그인 플로우: 이번 세션에서 재로드하지 않음(해당 산출물이
  diff로 byte-identical임이 이미 확인되어 재검증이 새로운 정보를 주지 않음).

## 8. 남은 OPEN / UNSUPPORTED / UNRESOLVED (이번 세션 종료 시점)

**제품/Runtime known gap 4건**:
1. Defect 2 — CONTENT_NOT_READY: OPEN / CONTRACT_LIMITATION(미수정)
2. GRID-3 — UNSUPPORTED_SEMANTIC(기존 상태 유지, 미변경)
3. CheckBox — Dataset bound 케이스: OPEN(안전한 일반화 근거 없음)
4. `ev:onpageload` 자동 발화 신뢰성 — AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED(BIND-1 / REALRT-2-CheckBox-unbound /
   Defect-2 모두에 공통 배경 요인으로 걸쳐 있다는 것이 이번 세션에서 더 뚜렷해짐)

**별도 certification blocker 1건**(위 4건과 범주 분리):
5. Target JDK 1.8.0_111 — BLOCKED_BY_DISTRIBUTION

CheckBox(unbound)의 widget/bootstrap semantics(value/checked/label/click)와 Phase1 SHA는 이번 세션에 각각
REAL_RUNTIME_VERIFIED/PASS로 해소되어 위 gap 목록에서 제외됐다. CheckBox(unbound)의 자동 page-init 발화는
해소되지 않고 위 4번 항목에 포함되어 있다.

## 9. 최종 Release Recommendation
기존 §12(FINAL-VERIFICATION-REPORT.md)의 **ENGINEERING_READY_FOR_PILOT_WITH_KNOWN_GAPS** 기조를 유지하되,
남은 gap 목록을 이번 세션 결과로 갱신한다(제품/Runtime known gap 4건 + 별도 certification blocker 1건 —
§8과 동일 목록).

## 10. 생성/갱신된 결과 파일 경로

### Core Documents

| 문서 | 상태 | 용도 |
|---|---|---|
| `work/results/FINAL-VERIFICATION-REPORT.md` | CURRENT / MASTER | 전체 프로젝트 최종 통합 판정 (§1, §9, §11, §12 갱신) |
| `work/results/followup-checkBox-ready-jdk-phase1-final.md` | CURRENT / DETAIL | 본 문서 — CheckBox/Defect 2/Target JDK/Phase1 SHA follow-up 4트랙 상세 |
| `work/results/conversion-quality-audit-final.md` | CURRENT / QUALITY-AUDIT | 화면 변환 품질 상세 (Round 4 갱신) |
| `work/results/phase1-sha-findings.md` | HISTORICAL / SUPERSEDED | Phase1 SHA UNRESOLVED 시점 조사 기록 보존용, 최신 판정 source 아님 |

### Supporting Evidence / Tooling

- `work/audit-scripts/phase1_sha_verifier.py` (신규) — Phase1 SHA deterministic verifier
- `work/audit-scripts/phase1_sha_manifest.json` (신규) — verifier manifest
- `work/phase4-working/.../src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java` (checkbox 분기 추가, Production 소스)
- `work/phase4-working/.../sample-phase3-output/Form/ControlPropertyMatrix.xml` (산출물, checkbox 수정 반영)
