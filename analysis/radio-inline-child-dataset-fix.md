# Radio 실제 root cause 확정: inline child &lt;Dataset&gt; 미지원 (실제 STT00001.xfdl evidence)

## 0. 실제 소스 확보 및 확정 evidence

사용자가 `C:\Users\gwang\OneDrive\바탕 화면\STT00001.xfdl`을 직접
제공했다. `Div01_Radio00`의 실제 원본:

```xml
<Radio id="Radio00" taborder="11" columncount="0" rowcount="0"
    position="absolute 87 -6 367 44" codecolumn="codecolumn"
    datacolumn="datacolumn" direction="vertical" index="0" value="0"
    onitemchanged="Div01_Radio00_onitemchanged">
    <Dataset id="innerdataset">
        <ColumnInfo>
            <Column id="codecolumn" size="256"/>
            <Column id="datacolumn" size="256"/>
        </ColumnInfo>
        <Rows>
            <Row><Col id="codecolumn">0</Col><Col id="datacolumn">기업고객(SOHO)</Col></Row>
            <Row><Col id="codecolumn">1</Col><Col id="datacolumn">개인고객(CB)</Col></Row>
        </Rows>
    </Dataset>
</Radio>
```

**결정적 차이**: `innerdataset="..."` **attribute가 존재하지 않는다.**
대신 Radio의 **직계 자식 element**로 `<Dataset id="innerdataset">`가
인라인 선언돼 있다(`codecolumn`/`datacolumn`은 여전히 Radio 자신의
attribute).

## 1. dcb7dfe 실제 failure point 확정

`BindingAnalyzer.java` 37행(수정 전):

```java
String inner = normalizeDataset(element.getAttribute("innerdataset"));
```

`Radio00` element에는 `innerdataset` **attribute 자체가 없으므로**
`element.getAttribute("innerdataset")`는 DOM 표준 동작대로 빈 문자열
`""`을 반환한다. 따라서 40행의 `if (id.length() > 0 && inner.length() > 0)`가
거짓이 되어 **`model.addItemset(...)`가 전혀 호출되지 않는다.**

```
INLINE_CHILD_DATASET_DETECTED = YES (실제 STT00001.xfdl로 확정, 추측 아님)
ITEMSET_BINDING_CREATED_FOR_INLINE_DATASET = NO (수정 전)
STATIC_CHOICES_BRANCH_ENTERED = NO (수정 전 -- ItemsetBinding 자체가 없어
  WebSquareGenerator.applyBindings의 bindingModel.findItemset()이 null을
  반환, appendStaticChoicesIfLiteralDataset 호출부에 도달하지 못함)
FAILURE_POINT = BindingAnalyzer.java:37 (innerdataset을 attribute로만
  읽고, 직계 자식 <Dataset> element는 전혀 확인하지 않음)
```

## 2~5. H1~H8 판정 (요청된 hypothesis 재판정)

```
H1. nested Div 내부 Radio라 BindingAnalyzer가 itemset을 수집하지 못함
    -> REJECTED(walk()는 재귀적으로 모든 자식을 순회하며 currentPath로
       "Div01.Radio00"을 정확히 구성한다 -- nesting 자체는 문제가 아니다)
H2. innerdataset 값 normalization 후 Dataset id가 달라짐
    -> REJECTED(애초에 attribute가 없어 normalization 이전 단계에서
       이미 빈 문자열이었다 -- normalization 로직 자체의 결함이 아님)
H3. codecolumn/datacolumn attribute가 model에 저장되지 않음
    -> REJECTED(이 두 값은 Radio 자신의 attribute이고, ItemsetBinding
       생성 자체가 안 됐을 뿐 이 두 값 자체는 항상 정상적으로 읽을 수
       있었다 -- 실제로 수정 후 정상적으로 codecolumn/datacolumn 그대로
       사용됨을 확인)
H4. Dataset은 찾지만 literal Rows가 없음
    -> REJECTED(Rows가 실제로 있고 값도 있다 -- 문제는 Dataset을
       "찾는" 단계 이전, ItemsetBinding 자체가 생성 안 되는 단계였다)
H5. Rows는 있지만 Dataset XML 구조가 helper가 기대한 형태와 다름
    -> REJECTED(구조 자체는 <ColumnInfo>/<Rows>/<Row>/<Col id="..">로
       기존 DatasetBinding.xfdl과 동일 -- 문제는 구조가 아니라 Dataset이
       "참조"가 아니라 "인라인 자식"이라는 점)
H6. column name lookup이 실제 Dataset schema와 맞지 않음
    -> REJECTED(codecolumn="codecolumn", datacolumn="datacolumn"이
       Column id와 정확히 일치 -- 수정 후 정상 매칭 확인)
H7. applyBindings가 nested Radio에 호출되지 않음
    -> REJECTED(convertChildren의 재귀 구조는 Div/GroupBox 등 container를
       재귀적으로 처리하며 Radio도 그 안에서 정상적으로 applyBindings까지
       도달한다 -- 로그로 확인: "[UI 변환] Radio Div01.Radio00 -> xf:select1
       id=Div01_Radio00")
H8. appendStaticChoicesIfLiteralDataset 호출 조건이 fixture에만 맞음
    -> CONFIRMED(정확히 이것이 원인이다 -- 이 함수 자체가 아니라 그
       호출에 필요한 ItemsetBinding을 만드는 BindingAnalyzer가 attribute
       참조 패턴만 지원했다)
```

## 6. Nested component 경로/key 일치 확인

```
SOURCE_COMPONENT_RAW_ID = Radio00 (source <Radio id="Radio00">)
CANONICAL_COMPONENT_ID(currentPath, BindingAnalyzer 내부) = "Div01.Radio00"
  (Div01 컨테이너 아래 Radio00이므로 join("Div01","Radio00"))
BINDING_MODEL_KEY = "Div01.Radio00" (ItemsetBinding의 componentId로 저장)
GENERATOR_LOOKUP_KEY = sourcePath = buildSourcePath(parentPath, localId)
  in WebSquareGenerator.convertChildren -- 동일하게 "Div01.Radio00" 생성
  (실제 로그: "[ITEMSET 변환] Div01.Radio00 -> innerdataset label=datacolumn
  value=codecolumn")
KEY_MATCH = YES (수정 후 실제 로그로 확인 -- key 불일치는 원인이
  아니었다. 애초에 ItemsetBinding 자체가 없었을 뿐, 있었다면 key는
  항상 일치했을 것이다)
```

## 3. Generic 지원 구현 (TYPE A / TYPE B 구분)

`BindingAnalyzer.java`의 itemset 처리 분기를 수정했다:

**BEFORE**:
```java
} else {
    String inner = normalizeDataset(element.getAttribute("innerdataset"));
    String code = element.getAttribute("codecolumn");
    String data = element.getAttribute("datacolumn");
    if (id.length() > 0 && inner.length() > 0) {
        model.addItemset(new ItemsetBinding(currentPath, inner, code, data));
        ...
    }
}
```

**AFTER**:
```java
} else {
    String inner = normalizeDataset(element.getAttribute("innerdataset"));
    if (inner.length() == 0) {
        // TYPE B: attribute 참조가 없으면 직계 자식 <Dataset>을 확인
        Element childDataset = findDirectChildDataset(element);
        if (childDataset != null) inner = normalizeDataset(childDataset.getAttribute("id"));
    }
    String code = element.getAttribute("codecolumn");
    String data = element.getAttribute("datacolumn");
    if (id.length() > 0 && inner.length() > 0) {
        model.addItemset(new ItemsetBinding(currentPath, inner, code, data));
        ...
    }
}
```

**신규 헬퍼**:
```java
private Element findDirectChildDataset(Element element) {
    NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
        Node n = children.item(i);
        if (n.getNodeType() != Node.ELEMENT_NODE) continue;
        Element child = (Element) n;
        String tag = localName(child);
        if ("Dataset".equals(tag) || "DataSet".equals(tag)) return child;
    }
    return null;
}
```

**왜 `WebSquareGenerator.java`는 전혀 건드리지 않았는가**:
`appendStaticChoicesIfLiteralDataset`/`findDatasetById`(dcb7dfe에서
이미 구현됨)는 이미 **경로 가정 없이 source document 전체를
`getElementsByTagName("*")`로 스캔**한다(직전 라운드
`analysis/radio-innerdataset-architecture-verification.md`에서 이미
확인). 인라인 자식 Dataset이든 `Objects` 아래 참조 Dataset이든, 일단
`ItemsetBinding.getDatasetId()`에 **정확한 id 문자열**만 들어있으면
`findDatasetById`가 문서 어디에 있든 찾아낸다. 마찬가지로
`WebSquareGenerator.appendDatasets()`(target `<w2:dataList>` 생성부,
런타임 `setNodeSet()`이 참조하는 대상)도 `findDescendants`로 문서
전체를 스캔하므로 인라인 자식 Dataset도 이미 `<w2:dataList
id="innerdataset">`로 정상 변환된다(별도 확인 -- 실제 STT00001
generated XML의 `<xf:model>` 영역에 포함됨). 즉 **문제는 오직
"ItemsetBinding을 만드는 단계"(BindingAnalyzer) 하나뿐**이었고,
그 이후 파이프라인(정적 choices 생성, 런타임 binding, target dataset
변환)은 전부 이미 경로 독립적으로 설계돼 있어 손댈 필요가 없었다.

## 4~5. 기존 REFERENCED_DATASET 지원 보존 + precedence

`if (inner.length() == 0)`으로 감싸 **`innerdataset` attribute가 있으면
그 값을 그대로 쓰고 자식 Dataset 조회는 아예 하지 않는다** -- 기존
TYPE A(참조) 동작을 1바이트도 바꾸지 않았다(같은 corpus/실제
STT00030의 `innerdataset="@ds_BRoombo"`류는 여전히 attribute 경로로만
처리됨, 아래 회귀 확인).

두 방식이 **동시에** 존재하는(즉 `innerdataset` attribute와 직계 자식
`<Dataset>`을 모두 가진) 실제 corpus 사례는 조사 결과 없었다(corpus
전체 + 실제 STT00030/STT00001 grep 결과 0건) -- 따라서 임의
precedence를 만들지 않고, "attribute를 먼저 확인하고 없을 때만 자식을
본다"는 자연스러운 순서만 유지했다(우연한 순서이지 강제 규칙이
아니며, 실제로 둘 다 있는 사례가 나오면 재검토 필요).

## 6. direction/columncount/rowcount 기록만 (변경 없음)

실제 STT00001의 Radio는 `direction="vertical" columncount="0"
rowcount="0"`이다. 이번 fix는 item 생성 자체에만 집중했고, 이
attribute들의 target 매핑은 이번 라운드에서 변경하지 않았다(요청대로
choices 생성 성공을 먼저 확인). 생성된 `xf:select1`에는 현재
`direction`/`columncount`/`rowcount`에 대응하는 target attribute가
없다 -- Studio에서 실제 배치(세로/가로 나열)가 올바른지는 별도 확인
필요(이번 root cause와 분리, 향후 evidence 확보 후 별도 라운드).

## 7. 실제 STT00001.xfdl fresh conversion 결과 (fixture 아님)

```
EXIT=0 (오류 없이 변환 완료)

[ITEMSET 변환] Div01.Radio00 -> innerdataset label=datacolumn value=codecolumn
[ITEMSET 변환] Div01.Radio00 -> 정적 xf:choices 2개 추가(source Dataset 리터럴 Rows 기반, Studio design-time 표현용)
[UI 변환] Radio Div01.Radio00 -> xf:select1 id=Div01_Radio00
```

**실제 생성된 `Div01_Radio00` 전체 block**:

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

label/value가 실제 source `<Rows>`와 정확히 일치한다(기업고객(SOHO)/0,
개인고객(CB)/1).

## 8. BEFORE/AFTER (실제 STT00001, fixture 아님)

**BEFORE(dcb7dfe)**:
```xml
<xf:select1 appearance="full" ev:onchange="scwin.Div01_Radio00_onitemchanged"
    id="Div01_Radio00" renderType="radiogroup"
    style="position:absolute;left:7.6%;top:-15.0%;width:24.5%;height:125.0%;"
    tabIndex="11" value="0"/>
```

**AFTER(이번 fix)**: 7번 항목의 전체 block과 동일.

## 9. Regression

```
clean compile = PASS
149/149 conversion = PASS(136 XML)
XML well-formed = PASS(136/136)
Phase1 SHA verifier = PASS
btn_cm=12 / wq_gvw=3 / w2selectbox_disabled=4 = 전부 무변경
HOLD structural class 유출 = 0(무변경)

INLINE_DATASET_RADIO_TEST = PASS(실제 STT00001.xfdl로 직접 확인,
  corpus 안에는 이 패턴의 fixture가 없어 실제 파일 자체가 유일한
  회귀 증거임)
REFERENCED_DATASET_RADIO_TEST = PASS(DatasetBinding.xfdl의 rdoCode,
  fix 전후 byte-identical)
REAL_STT00001_RADIO_TEST = PASS

149-fixture corpus 전체(136개 생성 XML) BEFORE/AFTER diff = 0개 파일
변경(diff -rq 결과 완전 동일) -- corpus 안에는 inline child dataset
패턴의 fixture가 없어 이 fix로 인한 corpus 변화 자체가 없다(예상된
결과, 회귀 없음의 다른 증거).

NON_RADIO_UNEXPECTED_DIFF_COUNT = 0
```

## 10. 최종 상태

```
INLINE_CHILD_DATASET_SUPPORTED = YES
REFERENCED_DATASET_SUPPORTED = YES

STT00001_GENERATED_CHOICES_PRESENT = YES
STT00001_GENERATED_ITEM_COUNT = 2
STT00001_LABEL_VALUE_MATCH = YES

NON_RADIO_UNEXPECTED_DIFF_COUNT = 0

RADIO_STATIC_CHOICES_FIX = FIX_CANDIDATE
RADIO_STUDIO_REVERIFY_READY = YES
```
