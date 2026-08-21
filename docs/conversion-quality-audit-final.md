# XPlatform → WebSquare 화면 변환 품질 — 최종 Audit 결과 (2026-08-18, 4 rounds)

자율 진행(사용자 중간 승인 없이) 실행. Phase4 original ZIP baseline은 전혀 수정하지 않음(불변 유지).
모든 수정은 `work/phase4-working/...`(Phase4-derived working candidate)에서만 수행.

- Round 1: 135 XFDL 전체 구조 audit + Grid 결함 3건(GRID-1/2/3) 처리.
- Round 2: Round 1에서 Grid와 무관하게 신규 발견된 BIND-1/REALRT-1/REALRT-2 처리(BIND-1/REALRT-1 FIXED, REALRT-2는
  당시 OPEN 유지).
- Round 3: REALRT-2를 Static/Edit/CheckBox로 좁혀 재조사해 Static/Edit는 FIXED, CheckBox는 안전한
  최소 수정을 확정하지 못해 OPEN 유지. GRID-1을 work/browser 진단 데이터로 interaction까지 검증 완료.
- Round 4(최신, 화면 변환 품질과 별도 트랙 — 상세는 `work/results/followup-checkBox-ready-jdk-phase1-final.md`):
  CheckBox unbound value/checked/label을 real 엔진 `getConfiguredOptions()`/`addItem()` API로 재조사해 FIXED로
  전환(dataset bound 케이스만 OPEN 유지). Defect 2/Target JDK/Phase1 SHA는 이 문서(화면 변환 품질) 범위 밖의
  별도 트랙이라 상세는 follow-up 문서 참고.

> **문서 관계 안내**: 이 문서는 화면 변환 품질(control/geometry/hierarchy/Grid/Dataset/property) 상세 audit이다.
> **최종 프로젝트 판정은 `FINAL-VERIFICATION-REPORT.md`**(CURRENT/MASTER)를 따르고, **CheckBox/Defect 2/Target
> JDK/Phase1 SHA follow-up 상세는 `followup-checkBox-ready-jdk-phase1-final.md`**(CURRENT/DETAIL)를 참고한다.
> 이 문서 자체의 최신 판정은 §11을 본다(§1~§10은 Round 1~3 시점 기록, Historical 표시된 부분 포함).

## 1. 결론 요약

XPlatform 화면의 컨트롤/위치/크기/property/Div 계층/Dataset은 **실질적으로 매우 높은 정확도로 보존**됩니다.
4 라운드에 걸쳐 실제 결함 7건(GRID-1, GRID-2, BIND-1, REALRT-1, REALRT-2-Static, REALRT-2-Edit,
REALRT-2-CheckBox-unbound)을 수정해 REAL_RUNTIME_VERIFIED로 승격했습니다. GRID-3(다중 Format)은
UNSUPPORTED_SEMANTIC으로 확정했고, CheckBox의 dataset-bound 값/체크상태/라벨 렌더링(REALRT-2 잔여분)은 안전한
최소 수정 범위를 확정하지 못해 **OPEN으로 유지**했습니다(unbound 케이스는 Round 4에서 해소).

## 2. 자동 Audit 최종 수치

**controls = 148 scanned / 146 PASS / 2 UNSUPPORTED / 0 MISMATCH**

| 항목 | scanned | PASS | MISMATCH | UNSUPPORTED |
|---|---|---|---|---|
| 일반 컨트롤 (id/type/geometry/hierarchy/property) | 148 | 146 | **0** | 2 (`FileDownload`, 문서화된 의도적 미지원) |
| Grid 구조/바인딩 (finding rows, §Grid 지표 표기 참고) | 3 | 2 | **0** | 1 (다중 Format, UNSUPPORTED_SEMANTIC) |
| Dataset/Column | 13 | 13 | **0** | - |

**Grid 지표 표기**: Grid audit의 "TOTAL"은 Grid element당 발견된 문제 개수만큼 누적되는 **finding row 수**이며,
project의 실제 Grid element 개수(3개: `grdMain`/`ComponentMethodConversion.grd`/`UnsupportedFeatures.grd`, 항상
3으로 고정)와는 다른 지표입니다. GRID-1/2 수정 완료 후 finding row 수가 3(PASS 2 + UNSUPPORTED 1)으로 우연히
element 개수와 같아졌을 뿐입니다. 혼동 방지를 위해 audit 스크립트 출력을 "Grid elements scanned: 3"과
"Finding rows: TOTAL=3 ..."으로 명칭 분리(`work/audit-scripts/xfdl_conversion_audit.py`).

## 3. Real Runtime 대표 5개 결과

**판정 기준**: 아래 5개는 render(정상 로드)/geometry(위치·크기)/hierarchy(parent-child DOM 관계) 기준으로 5/5
REAL_RUNTIME_VERIFIED입니다. property 값(텍스트/체크상태 등)은 §4의 개별 defect 판정을 따로 참고하세요.

| # | Fixture | 유형 | render/geometry | hierarchy | property |
|---|---|---|---|---|---|
| 1 | `NestedContainer.xfdl` | nested Div/container | source와 정확히 일치 | DOM 실측: edt→grpA→divA→grp_content 확인 | GroupBox title은 registry 기존 PARTIAL(신규 아님) |
| 2 | `Form/Main/TabExternalRelativePath.xfdl` | Div + 컨테이너(Tab) | 좌표 source와 일치 | DOM 실측: tabNested의 parent=divWrap | 해당 없음 |
| 3 | `GridAdvancedPhase3.xfdl` | complex Grid | 좌표 source와 일치 | parent=grp_content | GRID-1(구조+interaction)/GRID-2 반영 확인(§4) |
| 4 | `DatasetBinding.xfdl` | Dataset/binding | 좌표 정상 | 해당 없음 | BIND-1 반영 확인(§4) |
| 5 | `ControlPropertyMatrix.xfdl` | 일반 컨트롤 16종 | 좌표 source와 일치 | 해당 없음 | REALRT-1(FIXED, DOM 존재) + REALRT-2-Static/Edit(FIXED, 실제 텍스트/값 렌더 확인) + REALRT-2-CheckBox(OPEN, 값/체크상태/라벨 미표시) |

sample project 전체(135개 XFDL)에 "Div + Grid" 조합 fixture가 없어 해당 유형은 대표에 포함 불가(하드코딩 없이
정직하게 기록) — 대신 "Div + 컨테이너(Tab)"(#2)로 대체.

## 4. 결함 최종 상태

### GRID-1 — combo binding 손실 → **FIXED, REAL_RUNTIME_VERIFIED (STRUCTURE + INTERACTION)**

Round 1에서 구조(`inputType="select"` + `w2:choices/w2:itemset`)까지 FIXED. 이번 라운드에서 **work/browser 진단
데이터만 사용**(Production fixture 데이터는 수정하지 않음)해 interaction까지 끝까지 검증:

1. `dsCodes.setCellData(0,'CODE','A1')` / `setCellData(0,'NAME','TypeAlpha')`, row1에 `B2`/`TypeBeta` 주입.
2. `dsMain.setCellData(0,'CODE','C001')` / `'NAME','Sample'` / `'TYPE','A1')` — TYPE은 dsCodes가 채워진 뒤에야
   값이 실제로 반영됨을 확인(select 컬럼이 itemset 유효성 검증을 하는 것으로 추정).
3. 그리드가 실제로 1개 row를 렌더: TYPE 셀이 `data-inputtype="select"`, 표시 텍스트 **"TypeAlpha"**(NAME) —
   CODE "A1" → 라벨 "TypeAlpha"로 정상 매핑.
4. 셀에 실제 `dblclick` 이벤트를 dispatch해 편집 모드 진입 → 실제 `role="combobox"` 드롭다운이 열림
   (`w2selectbox_open` 클래스 확인) → 드롭다운 아이템 목록에 **"TypeAlpha"/"TypeBeta"** 둘 다 정상 표시.
5. "TypeBeta" 아이템을 `click` → `dsMain.getRowData(0)`이 `["C001","Sample","B2"]`로 변경, 셀 표시 텍스트도
   **"TypeBeta"**로 즉시 갱신 — CODE↔NAME 양방향 매핑이 실제 select/click 상호작용까지 정상 동작함을 확인.

**판정**: 구조와 interaction 둘 다 REAL_RUNTIME_VERIFIED로 확정(구조만으로는 완전한 REAL_RUNTIME_VERIFIED로
승격하지 않는다는 원칙에 따라, 이번 interaction 검증 성공으로 완전 승격). Production fixture(`GridAdvancedPhase3.xfdl`)
데이터는 전혀 수정하지 않음 — 전부 브라우저 런타임 진단 API 호출로만 수행.

### GRID-2 — summary band 손실 → **FIXED, REAL_RUNTIME_VERIFIED** (Round 1, 변경 없음)

### GRID-3 — 다중 Format → **UNSUPPORTED_SEMANTIC 확정** (Round 1, 변경 없음)

### BIND-1 — Dataset 반복 binding 값 미렌더 → **FIXED (binding fix)**, **page-init/onpageload 이슈는 별개로 표기**

- **binding fix 자체**: `[WebSquareGenerator] applyBindings`가 스칼라 value 바인딩이 걸린 데이터셋마다
  `{datasetId}.setRowPosition(0);` 부트스트랩을 추가. 이 fix가 real WebSquare에서 실제로 올바른 row를 가리키게
  만든다는 것은 **REAL_RUNTIME_VERIFIED**(수동으로 onpageload 경로를 실행시켰을 때 `edtName.getValue()==="Alpha"`
  확인).
- **별도 이슈(수정 대상 아님)**: `DatasetBinding.xml`은 최상위 popup 페이지인데도 `ev:onpageload`가 네비게이션
  직후 자동으로 실행되지 않아, 위 검증은 `mf_scwin.__xpws_onpageload({})`을 **수동 트리거**해서 확인한 것입니다.
  이는 BIND-1의 `setRowPosition` 로직 자체의 결함이 아니라, Defect 2(WFrame 동적 reload의 `CONTENT_NOT_READY`)와
  유사한 계열이지만 별개인 **page-init 단계의 환경 특성**입니다. 이번 세션 범위(Defect 2 수정 금지 지시 포함)와
  무관해 관찰만 기록하고 손대지 않았습니다.
- **표기**: `BIND-1 binding-logic: FIXED / REAL_RUNTIME_VERIFIED (triggered manually)`, `BIND-1 auto page-init:
  OBSERVED, not fixed, separate from Defect 2 but same symptom family` — 두 가지를 혼동 없이 분리 기록.

### REALRT-1 — CheckBox가 실 Runtime DOM에서 사라짐 → **FIXED (태그 존재), 값/체크상태/라벨은 별도 미해결**

- 태그 매핑 수정(`xf:selectBoolean`→`w2:checkbox`)으로 **컨트롤이 DOM에 다시 나타나는 것**은 REAL_RUNTIME_VERIFIED로
  확정.
- **단, DOM 존재만으로 완전한 semantic PASS로 처리하지 않음** — 아래 REALRT-2-CheckBox 참고: 실제 체크 상태/값/
  라벨 표시는 여전히 확인되지 않아 OPEN.

### REALRT-2 — 정적 text/value가 XForms 데이터 컨트롤에서 미반영 → **Static/Edit FIXED, CheckBox OPEN**

Static/Edit/CheckBox 3종을 shipped 샘플(공식 문서 attribute 목록 + KMS 실사용 예)에서 개별 조사, control type별
실제 규칙을 확인한 뒤 안전한 것만 적용:

| control | 실제 WebSquare 규칙(shipped docs 확인) | 조치 |
|---|---|---|
| `Static`(`w2:span`) | `value`는 문서화된 속성이 아님. `label`: "컴포넌트의 value를 화면에 출력하려는 텍스트를 지정" — 정확히 이 용도로 문서화됨 | **FIXED**: `copyBasicProperties`가 `w2:span`에는 `label=` 사용 |
| `Edit`/`MaskEdit`(`xf:input`) | `value`는 문서화된 속성이 아님(공식 attribute 목록에 없음). `initValue`: "초기의 input에 지정할 초기값" — 정확히 이 용도로 문서화됨 | **FIXED**: `copyBasicProperties`가 `xf:input`에는 `initValue=` 사용 |
| `CheckBox`(`w2:checkbox`) | 공식 문서: `value`는 "renderType이 'native'일 때만" 적용됨(기본 렌더타입은 "table" — input 태그 없이 table로 체크 아이콘만 그림, `ref=`로 상태를 구동). `renderType="native"`로 진단 전환 시 실제 `<input type="checkbox">`는 생성되나 `getValue()`는 여전히 빈 문자열, 문서 자체도 "native일 때 label 태그는 별도로 생성해야 함"이라고 명시 — 값 반영과 라벨 표시 둘 다 단일 속성 교체로 해결되지 않음 | **수정 안 함, OPEN 유지**: renderType 전환 자체가 이미 checkbox 렌더 구조를 바꾸는 것이고, 값/라벨까지 완성하려면 `ref=` 모델 바인딩(BIND-1과 같은 아키텍처 부류) + 별도 label 요소 생성이 추가로 필요해 "단일 attribute 교체" 수준의 안전한 최소 수정 범위를 벗어남 |

- **fix 위치**: `[WebSquareGenerator] copyBasicProperties` — `target.getTagName()`으로 분기해 `w2:span`→`label`,
  `xf:input`→`initValue`, 그 외(기존 `xf:trigger` 등)는 기존 `value` 유지.
- **real runtime 검증**: `ControlPropertyMatrix.xml` 재배포 후 DOM 확인 — `sta.textContent==="Label"`,
  `edt.value==="ABC"`, `mask.value==="123"`, 중첩 `PopupDiv` 안의 `pop_popSta.textContent==="Popup content"`까지
  전부 정상. `chk`(CheckBox)는 `<table class="w2checkbox_main"></table>`로 여전히 빈 상태 — **OPEN 유지, DOM
  존재만으로 semantic PASS 처리하지 않음**.
- **regression 영향**: 14개 파일에서 `value=` → `label=`/`initValue=` 속성명만 변경(다른 속성/구조 변화 없음,
  전수 diff로 확인). `xf:trigger`(Button)는 전혀 건드리지 않아 기존 정상 동작 유지.

## 5. 전체 영향 Diff (3 라운드 누적)

pristine Phase4-derived 빌드(Defect 1 fix만 적용) 대비, 이번 세션 전체(GRID-1/2 + BIND-1 + REALRT-1 +
REALRT-2-Static/Edit) 반영 후 `sample-phase3-output` 전체(`diff -rq`, conversion-report 제외) 비교 결과 **17개
파일**만 변경:

| 파일(들) | 변경 | 관련 defect |
|---|---|---|
| `Form/GridAdvancedPhase3.xml` | TYPE 컬럼 combo 구조 추가, `w2:footer` 블록 추가 | GRID-1, GRID-2 |
| `Form/DatasetBinding.xml` | `dsMain.setRowPosition(0);` 1줄 추가 | BIND-1 |
| `Form/ControlPropertyMatrix.xml` | `chk` 태그명 변경 + `sta`/`edt`/`mask`/`pop_popSta` 속성명 변경 | REALRT-1, REALRT-2 |
| 그 외 13개 파일(Tab 계열 fixture 내부의 Static/Edit) | `value=` → `label=`/`initValue=` 속성명만 변경 | REALRT-2 |

EXPECTED_CHANGE 17건, UNEXPECTED_CHANGE 0건(각 파일 diff를 전수 검토해 다른 속성/구조 변화가 없음을 확인).
나머지 118개 fixture는 byte-identical.

## 6. 전체 Regression (최종, 1회 수행)

| 항목 | 결과 |
|---|---|
| Java compile (JDK21 호스트, `-source/-target 1.8`) | 0 errors, 132 class |
| project conversion | 149/149 PASS |
| XML parse | 136/136 PASS |
| page JS syntax | 136/136 PASS |
| standalone/common JS syntax | 15/15 PASS |
| runtime finalization mock | 41/41 PASS |
| lifecycle mock | 14/14 PASS |
| Phase1 SHA-256 | (Historical — Round 3 result) 건드리지 않음, UNRESOLVED 유지 — **Round 4에서 PASS로 해소, §11 참고** |
| Target JDK 1.8.0_111 | (Historical — Round 3 result) 건드리지 않음, TARGET_JDK_RUNTIME_REQUIRED 유지 — **Round 4에서 BLOCKED_BY_DISTRIBUTION으로 확정, §11 참고** |
| WRM 로그인→WFrame/TabControl 실 Runtime 재확인 | 변경 없음, 정상(WFrame 4개) |
| Static External Tab 실 Runtime 재확인 | 변경 없음, 정상 |
| Dynamic setUrl(Defect 1) 실 Runtime 재확인 | 변경 없음, 정상 — **Defect 2는 건드리지 않음, OPEN 유지** |
| GridAdvancedPhase3 실 Runtime 재확인 | GRID-1(구조+interaction)/GRID-2 반영 확인, 정상 |

## 7. 최종 판정

| 카테고리 | 판정 |
|---|---|
| CONTROL_STRUCTURE | STATIC_VERIFIED (148 scanned / 146 PASS / 0 MISMATCH / 2 UNSUPPORTED) |
| LAYOUT_GEOMETRY | STATIC_VERIFIED + REAL_RUNTIME_VERIFIED(대표 5개, render/geometry 기준) |
| PARENT_CHILD_HIERARCHY | STATIC_VERIFIED + REAL_RUNTIME_VERIFIED(대표 5개 중 hierarchy 의미 있는 2개 DOM 실측) |
| PROPERTY_MAPPING | STATIC_VERIFIED(position/size/enable/cssclass/tooltiptext) + REAL_RUNTIME_VERIFIED(BIND-1 binding-logic, REALRT-1 DOM존재, REALRT-2-Static/Edit) — (Historical — Round 3 result) CheckBox 값/라벨은 당시 UNRESOLVED로 잔존, **Round 4에서 unbound는 REAL_RUNTIME_VERIFIED[WIDGET/BOOTSTRAP SEMANTICS]로 해소(자동 page-init 발화는 별도 OBSERVED), dataset-bound만 OPEN — §11 참고** |
| GRID_STRUCTURE | GRID-1/GRID-2 REAL_RUNTIME_VERIFIED, GRID-3 UNSUPPORTED_SEMANTIC |
| GRID_BINDING | GRID-1 REAL_RUNTIME_VERIFIED(**STRUCTURE + INTERACTION 둘 다**, work/browser 진단 데이터로 dropdown open/select/CODE↔NAME 왕복까지 확인) |
| DATASET_BINDING | STATIC_VERIFIED(13/13 구조) + REAL_RUNTIME_VERIFIED(BIND-1 binding-logic, 단 자동 onpageload 발화는 별개 관찰 사항) |
| REAL_RUNTIME_RENDERING | 대표 5개 render/geometry/hierarchy 5/5 REAL_RUNTIME_VERIFIED; (Historical — Round 3 result) property는 당시 CheckBox 값/라벨만 UNRESOLVED 잔존, **Round 4에서 unbound 해소 — §11 참고** |

### 핵심 질문에 대한 답

**"XPlatform 화면의 컨트롤, 위치, 크기, property, Div 내부 Grid/Label 등 종속관계가 WebSquare로 실질적으로
얼마나 정확하게 변환되는가?"**

- controls: **148 scanned / 146 PASS / 0 MISMATCH / 2 UNSUPPORTED**
- geometry: **148/148** static + **5/5 대표 화면** real-runtime DOM 위치/크기 일치
- hierarchy: **148/148** static + **2/2 대표 nested 화면** real-runtime DOM 부모-자식 확인
- datasets: **13/13** 구조/컬럼 정확 + BIND-1 binding-logic FIXED(자동 onpageload 발화는 별개 관찰)
- grids: **3/3 element** 구조 정확(GRID-3만 UNSUPPORTED_SEMANTIC), **GRID-1은 구조+interaction 모두
  REAL_RUNTIME_VERIFIED**, GRID-2 FIXED
- representative real-runtime screens: **5/5** render/geometry/hierarchy 정확; property는 Static/Edit 전부
  FIXED, **CheckBox 값/체크상태/라벨만 OPEN 잔존**

## 8. Defect 1 / Defect 2 상태 (건드리지 않음)

- **Defect 1**: FIXED, REAL_RUNTIME_VERIFIED — 재확인만, 코드 변경 없음.
- **Defect 2**: OPEN, REAL_RUNTIME_VERIFIED(재현) — 명시적 수정 금지 지시에 따라 이번 세션도 손대지 않음.

## 9. Production working-candidate 변경 파일 (누적)

| 파일 | 변경 내역 |
|---|---|
| `src/.../converter/GridFormatParser.java` | summCells 필드/getter, band 라우팅(GRID-2) |
| `src/.../converter/GridFormatConverter.java` | `appendFooter`(신규, GRID-2), `appendComboChoices`(신규, GRID-1), `resolveInputType`/`applyCellPresentation`/`getBandRowHeights`/`convert` 수정 |
| `src/.../converter/WebSquareGenerator.java` | `applyBindings`(BIND-1: `rowPositionBootstrapped` + `setRowPosition(0)`), `COMPONENT_MAP`(REALRT-1), `copyBasicProperties`(REALRT-2: 태그별 value 속성명 분기) |
| `src/.../mapping/ComponentMappingRegistry.java` | CheckBox 매핑 `xf:selectBoolean`→`w2:checkbox`(REALRT-1) |
| `sample-phase3-output/**` | 17개 파일 재생성(§5) |
| `work/audit-scripts/xfdl_conversion_audit.py` | combo 탐지를 `w2:choices/w2:itemset` 기준으로, value 비교를 태그별 attribute명 기준으로, CheckBox 매핑, Grid TOTAL 의미 분리 |

## 10. 남은 OPEN/UNSUPPORTED (Historical — Round 3 시점 기준, Round 4에서 갱신됨. 최신은 §11 참고)

- Defect 2 (CONTENT_NOT_READY, OPEN, 수정 금지 지시 — 건드리지 않음)
- GRID-3 다중 Format (UNSUPPORTED_SEMANTIC, 확정)
- REALRT-2-CheckBox (OPEN — 값/체크상태/라벨 렌더링에 `ref=` 모델 바인딩 + 별도 label 요소 생성이 추가로 필요해
  단일 attribute 교체 수준을 벗어남)
- BIND-1/일부 페이지의 `ev:onpageload` 자동 미발화 (OBSERVED, Defect 2와 유사 계열이나 별개, 건드리지 않음)
- Target JDK 1.8.0_111 (TARGET_JDK_RUNTIME_REQUIRED, 건드리지 않음)
- Phase1 SHA-256 (UNRESOLVED, 건드리지 않음)

## 11. Round 4 최신 상태 (2026-08-18, 상세는 `work/results/followup-checkBox-ready-jdk-phase1-final.md`)

위 §6/§7/§10은 Round 3 시점 기록이며, 다음 4가지는 Round 4에서 값이 바뀌었다:

| 항목 | Round 3 (historical) | **Round 4 (최신)** |
|---|---|---|
| Phase1 SHA-256 | UNRESOLVED | **STATIC_VERIFIED / PASS** — 추출 recipe 확정(생성된 출력 XML의 `<script>` 콘텐츠 기준) 및 재현 성공 |
| Target JDK 1.8.0_111 | TARGET_JDK_RUNTIME_REQUIRED | **BLOCKED_BY_DISTRIBUTION** — 신뢰 가능한 portable 바이너리 확보 불가로 확정(대체 안 함) |
| CheckBox — unbound | OPEN(값/체크상태/라벨 전체 미해결) | **REAL_RUNTIME_VERIFIED (WIDGET/BOOTSTRAP SEMANTICS)** — `addItem(value,label)`, click→checked→getValue round-trip 확인. 자동 page-init 발화는 별도로 **AUTO_PAGE_INIT_NOT_VERIFIED/OBSERVED**(과대승격 아님, 두 축 분리 판정) |
| CheckBox — dataset bound | OPEN | **OPEN 유지**(변경 없음, 안전한 일반화 근거 없음) |

Defect 2 / GRID-3 / `ev:onpageload` 자동 발화 신뢰성은 Round 3과 동일하게 OPEN/UNSUPPORTED_SEMANTIC/OBSERVED
유지(변경 없음). 즉 §10의 6개 항목 중 위 표의 2개(Phase1 SHA, Target JDK)는 해소, CheckBox 1개는 unbound만 부분
해소, 나머지 3개(Defect 2, GRID-3, onpageload)는 그대로다.
