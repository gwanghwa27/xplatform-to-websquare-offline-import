# Radio label/value literal corruption 조사 + innerdataset scope 정책 (TYPE A/B)

## PART 1 — label/value literal corruption

### 1-0. 핵심 발견: 이 저장소 코드는 corruption을 재현하지 않는다

폐쇄망에서 보고된 현상:
```
기업고객(SOHO) → 기업고객_SOHO_
0 → _ (숫자값도 언더스코어 계열로 변형)
```

이 저장소(candidate 브랜치, 커밋 `8082746`)를 clean compile해서 **동일한
실제 `STT00001.xfdl`을 다시 fresh conversion**했다. 결과(byte-level
`xxd` 확인 포함):

```
FINAL_XML_LABEL = 기업고객(SOHO)  (hex: ...28 53 4f 48 4f 29 = "(SOHO)" 그대로)
FINAL_XML_VALUE = 0
```

**corruption이 재현되지 않는다.** `appendStaticChoicesIfLiteralDataset`
(dcb7dfe에서 도입)의 실제 코드를 처음부터 다시 추적했다:

```
SOURCE_LABEL_LITERAL = "기업고객(SOHO)" (Col id="datacolumn"의 getTextContent())
SOURCE_VALUE_LITERAL = "0" (Col id="codecolumn"의 getTextContent())

LABEL_AFTER_DATASET_READ = sanitizeXml10("기업고객(SOHO)")
VALUE_AFTER_DATASET_READ = sanitizeXml10("0")
```

`sanitizeXml10`(`WebSquareGenerator.java:1909`)의 실제 구현:
```java
private String sanitizeXml10(String value) {
    ...
    for (int i = 0; i < value.length();) {
        int cp = value.codePointAt(i);
        if (isValidXml10Character(cp)) { out.append(Character.toChars(cp)); }
        i += Character.charCount(cp);
    }
    return out.toString();
}
```
이 함수는 **XML 1.0에서 허용되지 않는 제어문자만 제거**한다(탭/개행/
0x20~0xD7FF 등). 괄호·공백·숫자는 전부 유효한 XML 1.0 문자라 **완전히
그대로 통과**한다. 즉:
```
LABEL_AFTER_NORMALIZATION = "기업고객(SOHO)" (무변화)
VALUE_AFTER_NORMALIZATION = "0" (무변화)
```
그 다음 `appendCDataSafe`(CDATA 텍스트 노드 생성, `]]>` 이스케이프만
처리)를 거쳐 그대로 `<xf:label>`/`<xf:value>`에 들어간다.

```
FINAL_XML_LABEL = 기업고객(SOHO)
FINAL_XML_VALUE = 0
```

### 1-1. identifier sanitizer 오용 여부 -- 실제 함수 실행으로 검증

Production 전체에서 identifier-normalization 계열 함수를 검색한 결과,
정확히 일치하는 것을 찾았다: `WebSquareGenerator.java:1585`
**`sanitizeJsIdentifier(String value)`**:

```java
private static String sanitizeJsIdentifier(String value) {
    if (value == null || value.length() == 0) return "tab";
    StringBuilder sb=new StringBuilder();
    for(int i=0;i<value.length();i++){char c=value.charAt(i);
        if((i==0&&Character.isJavaIdentifierStart(c))||(i>0&&Character.isJavaIdentifierPart(c)))
            sb.append(c); else sb.append('_');}
    return sb.toString();
}
```

**실제로 이 함수를 컴파일해서 정확히 같은 입력으로 실행**했다(추측이
아니라 real execution):

```
sanitizeJsIdentifier("기업고객(SOHO)") -> "기업고객_SOHO_"
sanitizeJsIdentifier("0")              -> "_"
sanitizeJsIdentifier("개인고객(CB)")   -> "개인고객_CB_"
sanitizeJsIdentifier("1")              -> "_"
```

**폐쇄망에서 보고된 corruption과 정확히 100% 일치한다.** 한글
음절(Hangul)은 `Character.isJavaIdentifierStart/Part`가 true를 반환해
그대로 보존되지만, `(`/`)`는 identifier 문자가 아니라 `_`로 치환되고,
숫자 하나짜리 값(`0`,`1`)은 **position 0에서 `isJavaIdentifierStart`가
숫자에 대해 false**(Java 식별자는 숫자로 시작 불가)라 전체가 `_`로
치환된다.

```
RADIO_LITERAL_CORRUPTION_ROOT_CAUSE = IDENTIFIER_SANITIZER_MISUSE
  (패턴 확정, 실제 helper 실행으로 재현 -- 추측 아님)
```

### 1-2. 그러나 이 함수는 label/value 경로에서 호출되지 않는다

```
grep -n "sanitizeJsIdentifier(" WebSquareGenerator.java
  1530:  String safeTarget = sanitizeJsIdentifier(targetId);   <- 유일한 호출부
  1585:  private static String sanitizeJsIdentifier(...) {     <- 정의부
```

호출부는 **단 1곳**(1530행)이며, Tab 관련 이벤트 어댑터 이름 생성
로직 안에 있다(`findOrCreateTabEventAdapter` 주변) -- Radio
label/value와는 완전히 무관한 코드 경로다. `appendStaticChoicesIfLiteralDataset`/
`applyBindings`의 itemset 처리 블록 어디에도 이 함수에 대한 호출이
없다(직접 확인, grep 결과 1건뿐).

```
IDENTIFIER_SANITIZER_APPLIED_TO_LABEL = NO (이 저장소 코드 기준)
IDENTIFIER_SANITIZER_APPLIED_TO_VALUE = NO (이 저장소 코드 기준)
```

### 1-3. 결론 -- 다시 한번 source-sync 문제

지난 라운드("Radio 누적 Production 변경 의존성 검증")와 정확히 같은
패턴이다: **이 저장소(candidate branch, HEAD 기준)의 실제 코드는 이번
라운드에서도 재확인 결과 문제를 재현하지 않는다.** 두 차례에 걸쳐
실제 파일로 fresh conversion + byte-level 검증을 했고, 매번 정확한
결과가 나왔다.

corruption 패턴이 `sanitizeJsIdentifier`와 정확히 일치한다는 사실은
오히려 **폐쇄망이 이 저장소의 실제 코드를 그대로 반입한 것이 아니라,
누군가 별도로(또는 예전 시도에서) label/value 생성 로직을 직접
재구현하면서 마침 갖고 있던 "안전한 식별자 만들기" 계열 helper
(`sanitizeJsIdentifier` 자체이거나, 매우 유사한 동작을 하는 다른 구현)를
잘못 재사용했을 가능성**을 시사한다. 이 저장소의 `appendStaticChoicesIfLiteralDataset`은
그런 함수를 호출하지 않으므로, 이 저장소 코드 자체에는 수정할 버그가
없다.

```
IDENTIFIER_SANITIZER_APPLIED_TO_LABEL(이 저장소) = NO
IDENTIFIER_SANITIZER_APPLIED_TO_VALUE(이 저장소) = NO
LABEL_LITERAL_PRESERVATION_GATE = PASS(이 저장소 실제 실행 결과)
VALUE_LITERAL_PRESERVATION_GATE = PASS(이 저장소 실제 실행 결과)
```

**권장 확인 방법**: 폐쇄망의 실제 `WebSquareGenerator.java`에서
`appendStaticChoicesIfLiteralDataset` 함수 본문 전체를 열어, label/value
literal에 `sanitizeJsIdentifier`(또는 이와 동등한 정규식/식별자
치환 로직)이 호출되는지 직접 확인해야 한다. 이 저장소의 파일을 그대로
복사해 덮어쓰는 것이 가장 확실한 해결책이다(이전 라운드와 동일한
"버전 동기화" 문제일 가능성이 매우 높음).

이번 라운드에서는 **PART 1에 대한 Production 코드 변경을 하지 않았다**
-- 재현되지 않는 버그를 "예방"하는 코드를 추가하는 것은 오히려 실제
원인(source sync)을 가리는 결과가 될 수 있다.

### 1-4. 다른 itemset component 영향 확인

`appendStaticChoicesIfLiteralDataset`는 현재 Radio 및 (PART 2에서
확장한) inline-dataset Combo/ListBox에서 공유 호출되며, 전부 같은
`sanitizeXml10`+`appendCDataSafe` 경로만 사용한다(식별자 sanitizer
없음). CheckBox(`w2:checkbox`, 단일값)/AutoComplete(corpus에 아예 없음)는
itemset 처리 대상이 아니다(`applyBindings`의 `"Combo".equals ||
"ListBox".equals || "Radio".equals` 조건 자체에 포함 안 됨, 이번
라운드에 변경 없음).

```
STATIC_ITEM_LITERAL_EMITTER_SHARED = YES(Radio/Combo/ListBox 전부
  appendStaticChoicesIfLiteralDataset 하나를 공유, PART 2 참고)
AFFECTED_COMPONENT_TYPES = Radio, Combo(inline인 경우), ListBox(inline인 경우)
  -- 전부 동일한 sanitizeXml10 경로이므로 동일하게 안전함(식별자
  sanitizer 없음)
```

---

## PART 2 — innerdataset scope 정책: TYPE A(inline)/TYPE B(referenced) 구분

### 2-0. 원칙

XPlatform 컴포넌트의 item 목록 전용 인라인 `<Dataset>`(TYPE A)은 다른
컴포넌트/script/transaction이 공유하는 업무 Dataset이 아니다. 따라서
target에 불필요한 `<w2:dataList>`/런타임 `setNodeSet()`을 만들지
않는다. 반대로 `innerdataset="dsCode"`처럼 attribute로 **참조**하는
Dataset(TYPE B)은 다른 곳에서도 쓰일 수 있으므로 기존 동작(dataList +
setNodeSet 유지)을 그대로 둔다.

### 2-1. 판정 기준 -- id 문자열이 아니라 부모 컴포넌트 타입

금지된 방식(`if dataset.id == "innerdataset": skip`)을 쓰지 않았다.
대신 신규 함수:

```java
private boolean isComponentLocalItemsetDataset(Element dataset) {
    if (dataset == null) return false;
    Node parent = dataset.getParentNode();
    if (!(parent instanceof Element)) return false;
    String parentTag = getSourceTagName((Element) parent);
    return "Radio".equals(parentTag) || "Combo".equals(parentTag) || "ListBox".equals(parentTag);
}
```

**Dataset의 직계 부모가 itemset-capable 컴포넌트(Radio/Combo/ListBox)인지만
본다** -- id 값이 무엇이든(우연히 "innerdataset"이 아니어도) 동일하게
판정된다. 반대로 `<Objects>` 아래 있는 Dataset은 부모가 컴포넌트가
아니므로 절대 TYPE A로 오판되지 않는다.

### 2-2. 지원할 itemset component 실제 corpus 조사

corpus(149-fixture) + 실제 `STT00030.xfdl`/`STT00001.xfdl` 전체에서
itemset-capable 후보(Radio/Combo/ListBox/CheckBox/CheckComboBox/
AutoComplete/Spin) 각각에 대해 `<Comp ...><Dataset...` 인라인 자식
패턴을 정규식으로 전수 스캔했다:

| SOURCE_COMPONENT | INLINE_CHILD_DATASET_COUNT | TARGET_ITEM_STRUCTURE | TARGET_DATALIST_REQUIRED |
|---|---|---|---|
| Radio | 1 (실제 STT00001.xfdl `Radio00`) | `xf:choices`(정적) | NO |
| Combo | 0 (corpus/실제 파일 어디에도 인라인 사례 없음, 참조형만 존재) | 해당 없음(참조형은 기존 동작 유지) | 해당 없음 |
| ListBox | 0 (사용 사례 자체가 corpus에 없음) | 해당 없음 | 해당 없음 |
| CheckBox/CheckComboBox/AutoComplete/Spin | 0 | 해당 없음 | 해당 없음 |

**Radio 전용 hardcoding을 피하기 위해**, `isComponentLocalItemsetDataset`과
정적 choices 생성 호출은 Combo/ListBox도 architecturally 커버하도록
구현했다(2-3 참고) -- 다만 실제 evidence(inline 사례)는 이번 corpus에
Radio 1건뿐이다.

### 2-3. Production 구현

**`appendDatasets`** (target `<w2:dataList>` 생성부): TYPE A Dataset을
skip.

```java
for (int i = 0; i < datasets.size(); i++) {
    Element ds = datasets.get(i);
    if (isComponentLocalItemsetDataset(ds)) {
        System.out.println("[DATA TODO] component-local inline itemset Dataset -> "
                + "w2:dataList 생성 생략(정적 xf:choices 전용): " + sanitizeXml10(ds.getAttribute("id")));
        continue;
    }
    ...
}
```

**`applyBindings`** (itemset 처리부): TYPE A면 `setNodeSet()`을 만들지
않고, sourceTag와 무관하게(Radio/Combo/ListBox 전부) 정적 choices를
유일한 item source로 만든다 -- **그렇지 않으면 item이 완전히 비는
회귀가 생기기 때문**(TYPE A는 dataList 자체가 없으므로 setNodeSet도
못 만들고, 원래 Radio 전용이던 정적 choices도 안 만들면 Combo/ListBox의
inline case는 item이 0개로 남는다):

```java
Element itemsetDataset = findDatasetById(itemset.getDatasetId());
boolean inlineDataset = isComponentLocalItemsetDataset(itemsetDataset);
if (!inlineDataset) {
    // TYPE B: 기존 동작 그대로(dataList + setNodeSet 유지)
    pageLoadStatements.add(targetId + ".setNodeSet(...)");
} else {
    // TYPE A: dataList도 setNodeSet도 만들지 않음
}
if ("Radio".equals(sourceTag) || inlineDataset) {
    appendStaticChoicesIfLiteralDataset(out, target, itemsetDataset, itemset, sourcePath);
}
```

`appendStaticChoicesIfLiteralDataset`의 시그니처를 `Element dataset`을
직접 받도록 바꿔(기존에는 함수 내부에서 `findDatasetById`를 다시
호출했음) `applyBindings`가 TYPE A/B 판정에 쓴 것과 같은 조회 결과를
재사용하도록 정리했다(중복 조회 제거, 로직 자체 변경 없음).

### 2-4. Referenced(TYPE B) Dataset은 무조건 제거하지 않음 -- usage 확인

`DatasetBinding.xfdl`의 `dsCode`(`Combo cboCode` + `Radio rdoCode`가
**같이** 참조)로 실제 확인했다:

```
DATASET_REFERENCED_BY_ITEMSET_COUNT = 2 (cboCode, rdoCode 둘 다 참조)
DATASET_USED_BY_SCRIPT = NO(이 fixture는 별도 script 없음)
DATASET_USED_BY_TRANSACTION = NO
DATASET_USED_BY_GRID = NO
DATASET_USED_BY_BIND = NO
```

두 컴포넌트가 **같은** Dataset을 공유하는 실제 사례이므로, 이런
경우 dataList를 지우면 두 컴포넌트 모두의 런타임 binding이 깨진다 --
정확히 이런 이유로 TYPE B는 그대로 유지했다(실제 회귀 검증 결과도
`dsCode` dataList/두 setNodeSet 호출 전부 무변경, 아래 4절).

### 2-5. 실제 STT00001 fresh conversion 결과 (수정 후)

```
[DATA TODO] component-local inline itemset Dataset -> w2:dataList 생성 생략(정적 xf:choices 전용): innerdataset
[ITEMSET 변환] Div01.Radio00 -> innerdataset (component-local inline dataset, 정적 xf:choices만 사용, 런타임 setNodeSet/w2:dataList 생성 안 함)
[ITEMSET 변환] Div01.Radio00 -> 정적 xf:choices 2개 추가(source Dataset 리터럴 Rows 기반, Studio design-time 표현용)
```

**생성된 `Div01_Radio00` 전체 block**:
```xml
<xf:select1 appearance="full" ev:onchange="scwin.Div01_Radio00_onitemchanged"
    id="Div01_Radio00" renderType="radiogroup"
    style="position:absolute;left:7.6%;top:-15.0%;width:24.5%;height:125.0%;"
    tabIndex="11" value="0">
    <xf:choices>
        <xf:item>
            <xf:label><![CDATA[기업고객(SOHO)]]></xf:label>
            <xf:value><![CDATA[0]]></xf:value>
        </xf:item>
        <xf:item>
            <xf:label><![CDATA[개인고객(CB)]]></xf:label>
            <xf:value><![CDATA[1]]></xf:value>
        </xf:item>
    </xf:choices>
</xf:select1>
```

```
STT00001_RADIO_CHOICES_PRESENT = YES
STT00001_RADIO_ITEM_COUNT = 2
STT00001_ITEM_1_LABEL = 기업고객(SOHO)   STT00001_ITEM_1_VALUE = 0
STT00001_ITEM_2_LABEL = 개인고객(CB)     STT00001_ITEM_2_VALUE = 1

STT00001_INLINE_INNERDATASET_DATALIST_COUNT = 0
  (grep -c 'id="innerdataset"' 결과 0 -- w2:dataList 완전히 사라짐)
STT00001_INLINE_INNERDATASET_SETNODESET_COUNT = 0
  (grep -c "innerdataset" 전체 파일 결과 0 -- script에도 흔적 없음)

STT00001의 다른 6개 실제 업무 Dataset(ds_dtList/dsTransInfo/ds_param/
ds_dtList00/ds_combo/ds_dtList01, 전부 <Objects> 레벨)은 전부 정상
w2:dataList로 그대로 생성됨(무변화 확인) -- component-local 판정이
Objects-level Dataset을 실수로 건드리지 않음을 실제로 확인했다.
```

### 2-6. Regression

```
clean compile = PASS
149/149 conversion = PASS(136 XML)
XML well-formed = PASS(136/136)
Phase1 SHA verifier = PASS
btn_cm=12 / wq_gvw=3 / w2selectbox_disabled=4 = 전부 무변경
HOLD structural class 유출 = 0(무변경)

149-fixture corpus 전체(136개 생성 XML) BEFORE/AFTER diff = 0개 파일
변경(diff -rq 완전 동일) -- corpus 안에는 inline child dataset
패턴의 fixture가 없어 예상된 결과.

REFERENCED_DATASET_RADIO_TEST(DatasetBinding.xfdl rdoCode/cboCode,
dsCode 공유) = PASS(dataList/두 setNodeSet 호출/rdoCode 정적 choices
전부 fix 전후 byte-identical)
INLINE_DATASET_RADIO_TEST(실제 STT00001.xfdl) = PASS

INLINE_CHILD_DATASET_COUNT = 1 (실제 corpus 전체 기준, Radio00 유일)
INLINE_CHILD_DATASET_TARGET_DATALIST_COUNT = 0 (목표 달성)
INLINE_ITEMSET_STATIC_CHOICES_COUNT = 1 (Div01_Radio00)
REFERENCED_DATASET_DATALIST_REMOVED_COUNT = 0 (TYPE B는 하나도 제거 안 함)
SHARED_DATASET_ACCIDENTAL_REMOVAL_COUNT = 0 (STT00001의 다른 6개 Dataset +
  DatasetBinding.xfdl의 dsCode 전부 정상 유지 확인)

RADIO_ITEM_COUNT = 2
RADIO_LABEL_VALUE_MATCH = PASS
NON_RADIO_UNEXPECTED_DIFF_COUNT = 0
NON_ITEMSET_DATASET_UNEXPECTED_DIFF_COUNT = 0
```

### 2-7. 변경 파일

`src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`
1개 파일만 수정(`appendDatasets`, `applyBindings`,
`appendStaticChoicesIfLiteralDataset` 시그니처, 신규
`isComponentLocalItemsetDataset`). `BindingAnalyzer.java`는 이번
라운드에 재수정하지 않았다(요청대로 inline Dataset 감지 로직 자체는
그대로 유지).

## 최종 상태

```
RADIO_ITEM_STRUCTURE = PASS

SOURCE_LABEL_1 = 기업고객(SOHO)     GENERATED_LABEL_1 = 기업고객(SOHO)
SOURCE_VALUE_1 = 0                  GENERATED_VALUE_1 = 0
SOURCE_LABEL_2 = 개인고객(CB)       GENERATED_LABEL_2 = 개인고객(CB)
SOURCE_VALUE_2 = 1                  GENERATED_VALUE_2 = 1

IDENTIFIER_SANITIZER_APPLIED_TO_LABEL = NO (이 저장소 코드 기준)
IDENTIFIER_SANITIZER_APPLIED_TO_VALUE = NO (이 저장소 코드 기준)
RADIO_LITERAL_CORRUPTION_ROOT_CAUSE = IDENTIFIER_SANITIZER_MISUSE
  (패턴은 실제 helper 실행으로 확정됐으나, 이 저장소 코드에는 그
  helper 호출이 없음 -- 폐쇄망 source-sync 문제로 재분류)

CHANGED [WebSquareGenerator] method = appendDatasets(TYPE A skip 추가),
  applyBindings(TYPE A/B 분기 추가), appendStaticChoicesIfLiteralDataset
  (시그니처 변경, Element dataset 직접 수신), isComponentLocalItemsetDataset(신규)

LABEL_LITERAL_PRESERVATION_GATE = PASS
VALUE_LITERAL_PRESERVATION_GATE = PASS

STT00001_RADIO_LITERAL_MATCH = YES
NON_RADIO_UNEXPECTED_DIFF_COUNT = 0

INLINE_CHILD_DATASET_POLICY = TYPE A(부모가 itemset-capable 컴포넌트) ->
  정적 xf:choices만 생성, w2:dataList/runtime setNodeSet 생성 안 함
INLINE_DATASET_GLOBAL_DATALIST_GENERATION = NO
INLINE_DATASET_RUNTIME_BINDING_GENERATION = NO
REFERENCED_DATASET_POLICY = 무변경(TYPE B는 기존 dataList+setNodeSet 유지)

STT00001_INLINE_DATALIST_PRESENT = NO
STT00001_RADIO_CHOICES_PRESENT = YES

LABEL_VALUE_LITERAL_PRESERVATION = PASS
SHARED_DATASET_ACCIDENTAL_REMOVAL_COUNT = 0

ADDITIONAL_PRODUCTION_FILES_CHANGED = 0
  (WebSquareGenerator.java 1개 파일만 수정, BindingAnalyzer.java 무변경)

RADIO_STUDIO_REVERIFY_READY = YES
```
