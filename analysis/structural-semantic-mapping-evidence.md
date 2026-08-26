# XPlatform → WebSquare structural/business semantic class mapping — evidence 조사 (분석 전용, Production 무변경)

Radio 관련 작업(inline dataset, static choices, label/value literal, TYPE A/B dataList/setNodeSet
suppression)은 이번 라운드 기준 완료 처리한다 -- 상세: `radio-label-literal-corruption-and-innerdataset-scope-policy.md`,
`radio-inline-child-dataset-fix.md`, `radio-rendering-root-cause.md`. 이번 문서는 그 이후 단계인
`shbox`/`dfbox`/`tbbox`(지금까지 HOLD였던 structural/business semantic class) 매핑 가능성을
evidence 기반으로만 조사한다. **이번 라운드는 조사 문서 산출까지만이며 Production 코드는
전혀 수정하지 않았다.**

## 0. 조사 범위와 근본적 제약 -- 먼저 밝힌다

이번 조사의 evidence는 두 갈래로 완전히 분리된다.

| 축 | 위치 | 성격 | 규모 |
|---|---|---|---|
| TARGET(WebSquare v6) 증거 | `websquare-devpack-copy/tomcat/webapps/ROOT/ui/{BM,HM,SP}/*.xml` | **실제 업무 화면**(hand-authored native v6) | 59개 XML |
| TARGET 증거 | `resources/target-websquare/WebContent/assets/css/contents.css` | 실제 운영 CSS(REFERENCE_ONLY 사본, SHA 확인됨) | 1개 파일, 전체 |
| SOURCE(XPlatform) 증거 후보 1 | `sample-phase3-project/**/*.xfdl` | 이 저장소의 **합성 회귀 fixture corpus** | 136개 XFDL |
| SOURCE(XPlatform) 증거 후보 2 | 사용자 제공 실제 업무 화면 `STT00001.xfdl`(저장소 외부, ephemeral) | **실제 업무 화면** | 1개 XFDL |

**가장 중요한 발견(이번 조사의 전제 조건)**: `sample-phase3-project` corpus는 corpus-wide
feature inventory의 재료로 쓰기에 **구조적으로 부적합**하다. 실제 내용 집계 결과:

```
Div: 2개   Grid: 3개   Button: 11개   Static: 10개   Edit: 11개   Combo: 4개   Radio: 3개
class="..." 속성 사용: 전체 corpus에서 단 1건(class="detail-tab", Tab 관련 무관 fixture)
```

136개 파일 대부분은 Tab 전환/Dataset 바인딩/이벤트 처리 같은 **좁은 단위 회귀 시나리오**를
검증하기 위한 합성 fixture이지, "검색조건+Grid+업무 table"로 구성된 실제 업무 화면이 아니다.
이름이 `Search.xfdl`이라서 검색조건 후보로 의심했던 파일도 실제로는 Tab 콘텐츠 로딩 테스트용
(`Button 1개 + Edit 1개`, Static/Label 없음, 반복 패턴 없음)으로 확인되어 evidence에서 제외했다.

따라서 **corpus-wide 통계로 "반복 N회 확인"을 주장할 수 있는 real SOURCE 화면은 사실상
`STT00001.xfdl` 1건뿐**이다. 아래 3~6절의 SOURCE-side 판정은 전부 이 제약 위에서
"n=1 real evidence"로 명시하며, corpus-repeat 요건을 만족하지 못하는 항목은 confidence를
HIGH로 올리지 않는다(요청사항 4/5/6절의 "corpus에서 반복되는 구조인지" 기준을 정직하게
반영한 결과이며, 추측으로 채우지 않는다).

## 1. TARGET semantic inventory -- 실측 재확인

59개 real 업무 화면(BM/HM/SP, phase4test 제외) 기준 실제 사용 빈도:

```
shbox     : 22/59 파일에서 발견
dfbox     : 42/59 파일에서 발견
tbbox     : 29/59 파일에서 발견
wq_gvw    : 38/59 파일에서 발견
```

대표 실측 구조(`ui/BM/BM001M01.xml:363-618`):

```xml
<xf:group class="sub_contents flex_gvw">
  <xf:group class="shbox">
    <xf:group class="shbox_inner" id="tbl_search">
      <xf:group class="w2tb tb" tagname="table">
        <xf:group tagname="colgroup">...</xf:group>
        <xf:group tagname="tr">
          <xf:group class="w2tb_th" tagname="th"><w2:textbox label="검색항목"/></xf:group>
          <xf:group class="w2tb_td" tagname="td"><xf:select1 .../></xf:group>
          <xf:group class="w2tb_th" tagname="th"><w2:textbox label="사용여부"/></xf:group>
          <xf:group class="w2tb_td" tagname="td"><xf:select1 appearance="full" renderType="radiogroup" .../></xf:group>
        </xf:group>
      </xf:group>
    </xf:group>
    <xf:group class="btn_shbox">
      <xf:trigger class="btn_cm sch" id="btn_search"><xf:label>조회</xf:label></xf:trigger>
    </xf:group>
  </xf:group>
  <xf:group class="dfbox">
    <w2:textbox tagname="h3" label="코드그룹"/>
    <xf:group class="fr">
      <w2:textbox class="sum" id="spn_grpCnt"/><w2:textbox label="건"/>
      <xf:trigger class="btn_cm row_add">추가</xf:trigger>
      <xf:trigger class="btn_cm">취소</xf:trigger>
      <xf:trigger class="btn_cm download">엑셀다운로드</xf:trigger>
    </xf:group>
  </xf:group>
  <xf:group class="gvwbox wq_flx"><w2:gridView class="wq_gvw" .../></xf:group>
  <!-- 같은 파일 안에서 dfbox+gvwbox 쌍이 2회 반복(두 번째 Grid) -->
  <xf:group class="btnbox"><xf:group class="fr">...</xf:group></xf:group>
</xf:group>
```

`tbbox` 실측(`ui/BM/BM005M01.xml:324-368`, 업무 데이터 등록/수정 form):

```xml
<xf:group class="tbbox">
  <xf:group class="w2tb tb" id="grp_content" tagname="table">
    <xf:group tagname="tr">
      <xf:group class="w2tb_th" tagname="th"><w2:textbox label="제목"/></xf:group>
      <xf:group class="w2tb_td" tagname="td"><xf:input ref="data:dlt_release.TITLE"/></xf:group>
      <xf:group class="w2tb_th" tagname="th"><w2:textbox label="작성일"/></xf:group>
      <xf:group class="w2tb_td" tagname="td"><w2:inputCalendar ref="data:dlt_release.CREATED_DATE"/></xf:group>
    </xf:group>
    <xf:group tagname="tr">
      <xf:group class="w2tb_th" tagname="th"><w2:textbox label="내용"/></xf:group>
      <xf:group class="w2tb_td" tagname="td"><xf:textarea ref="data:dlt_release.CONTENT"/></xf:group>
    </xf:group>
  </xf:group>
</xf:group>
```

**핵심 관찰**: `shbox_inner`와 `tbbox`는 내부적으로 **동일한 `w2tb tb`(table/tr/th/td) 하위구조를
공유**한다. 둘의 차이는 DOM 모양이 아니라 **문맥(context)**이다 -- `shbox_inner`는 항상
`btn_shbox`(조회 버튼)와 형제이고 그 바로 뒤에 Grid가 온다("검색 후 결과 Grid" 패턴). `tbbox`는
검색 버튼 형제가 없고 단독으로 등장하며 등록/수정용 필드(제목/내용/작성일 등)를 담는다("업무
데이터 입력/표시 form" 패턴). 즉 target 쪽에서도 **구조(structure)만으로는 shbox_inner와
tbbox를 구분할 수 없고, 인접 형제(sibling) 문맥까지 봐야 한다** -- 이는 4~6절의 discriminator를
"컨테이너 하나의 모양"이 아니라 "형제 관계 패턴"으로 정의해야 함을 뜻한다.

## 2. XPlatform corpus 구조 조사 -- 실제 수집 결과

`sample-phase3-project`(136개) 전체에 대해 다음을 수집했다(제약: 위 0절 참고, 대부분
Tab/Dataset/Event 단위 테스트라 반복되는 "업무 화면형" 패턴이 존재하지 않는다):

```
COMPONENT_TAG 분포: Form 135, Tabpage 115, Tab 97, Edit 11, Cell 11, Button 11,
                     Static 10, Combo 4, Radio 3, Grid 3, Div 2
class="..." 사용: 1건(무관)
label-like Static + input 반복 쌍: 0건(검색 가능한 corpus 내에서 발견 안 됨)
button-only child group: 0건(Button은 있으나 전용 sibling 그룹으로 분리된 사례 없음)
grid header/title 형제 그룹: 0건
```

**결론**: 이 corpus만으로는 shbox/dfbox/tbbox 어느 것도 "반복 확인"할 수 없다. 그래서 3~6절은
실제 업무 화면 `STT00001.xfdl` 1건의 실측으로 대체한다(추측 아님 -- 실제 파일 재확인).

### STT00001.xfdl 실측 구조 (전체 193→이번 회차 재확인 193라인, 원본 그대로)

```xml
<Div id="Div01" class="round" position="absolute 8 32 1153 97">
  <!-- Label Static + Data(배경) Static 쌍 반복 + Edit/Calendar/Radio -->
  <Static id="sta_WFDA_Label00" text="기준년월" class="sta_WFDA_Label_01" position="absolute -2 30 78 56"/>
  <Static id="sta_WFDA_Data00" class="sta_WFDA_Data" position="absolute 85 6 197 32"/>
  <Calendar id="BSYM" class="Calendar03" position="absolute 88 33 169 53"/>
  <Static id="sta_WFDA_Label01" text="대상실적" class="sta_WFDA_Label_01" position="absolute -2 6 78 32"/>
  <Radio id="Radio00" position="absolute 87 -6 367 44" .../>
  <Static id="sta_WFDA_Label07" text="고객번호" class="sta_WFDA_Label_01" position="absolute 169 30 269 56"/>
  <Edit id="CUST" class="input_point_00" position="absolute 271 33 393 53"/>
</Div>
<Div id="Div00" position="absolute 0 4 1160 31" taborder="2">
  <!-- class 없음, Button만 2개, Div01보다 y좌표가 위(y:4~31), Div01(y:32~97) 바로 앞 -->
  <Button id="btn_WFSA_Search01" text="엑셀다운" class="btn_WFSA_Search" onclick="btn_excel_onclick"/>
  <Button id="btn_WFSA_Search02" text="조회" class="btn_WFSA_Search" onclick="doSearch"/>
</Div>
<Grid id="STT00001" class="round" position="absolute 8 110 1153 741" binddataset="ds_dtList00"/>
```

수집 feature:

```
COMPONENT_TAG=Div, COMPONENT_ID=Div01, SOURCE_CLASS=round, PARENT_TAG=Layout, geometry=(8,32,1153,97)
  CHILD_COMPONENT_TYPES: Static x5, Edit x1, Calendar x1, Radio x1
COMPONENT_TAG=Div, COMPONENT_ID=Div00, SOURCE_CLASS=(없음), PARENT_TAG=Layout, geometry=(0,4,1160,31)
  CHILD_COMPONENT_TYPES: Button x2 (모두 class="btn_WFSA_Search")
COMPONENT_TAG=Grid, COMPONENT_ID=STT00001, SOURCE_CLASS=round, PARENT_TAG=Layout, geometry=(8,110,1153,741)
```

label-like Static + input 반복 패턴: **확인됨** (`sta_WFDA_Label_01` Static 3개 + 대응
Edit/Calendar/Radio 3개, 1:1 인접 좌표). button-only child group: **확인됨**(Div00). grid 바로
위에 이 두 sibling(Div00, Div01)이 위치. 다만 **이 패턴이 관측된 실제 화면은 1건뿐**이다.

## 3. 기존 XPlatform source class 조사

STT00001에서 실제 관측된 distinct source class:

| SOURCE_CLASS | SOURCE_TAG | OCCURRENCE(이 화면 내) | COMMON_PARENT | COMMON_CHILD | LIKELY_SEMANTIC |
|---|---|---|---|---|---|
| `round` | Div | 1 | Layout | Static/Edit/Calendar/Radio | 명확한 semantic 없음(모서리radius 스킨 클래스로 추정) |
| `round` | **Grid** | 1 | Layout | Formats/Band | **Div와 동일한 class를 Grid가 재사용** |
| `sta_WFDA_Label_01` | Static | 3 | Div | (텍스트만) | 라벨 텍스트 |
| `sta_WFDA_Data` | Static | 5 | Div | (텍스트만) | 데이터 표시 배경/자리표시자 |
| `input_point_00` | Edit | 1 | Div | - | 입력 필드 스킨 |
| `Calendar03` | Calendar | 1 | Div | - | 날짜 입력 스킨 |
| `btn_WFSA_Search` | Button | 2 | Div | - | 버튼 스킨(조회/엑셀다운 공용) |

**결정적 반증**: `class="round"`가 **검색조건 컨테이너(Div01)와 Grid 양쪽 모두**에 붙어 있다.
즉 이 화면 하나만 봐도 이미 "source class 문자열을 그대로 target semantic 판정에 쓸 수 없다"는
사용자 지침(3절 "source class name 자체를 그대로 target class로 복사하지 않는다")이 실측으로
확인된다 -- `round`는 컨테이너의 업무 semantic(검색/그리드 구분)과 무관한 범용 스킨 class로
보인다. `btn_WFSA_Search`도 "조회"(검색 실행)와 "엑셀다운"(그리드 액션)에 동일하게 붙어 있어
버튼의 semantic(검색 트리거 vs 그리드 액션)을 class만으로 구분할 수 없다 -- 오직 **부모 Div의
위치/형제 관계**로만 구분 가능했다(Div00는 Grid 바로 앞, 버튼만 존재).

**source class는 이번 evidence 기준 신뢰 가능한 discriminator가 아니다.** 판정에 쓸 수 있는
것은 (a) 부모-자식 구조, (b) 형제 관계, (c) 컴포넌트 조합 패턴뿐이다.

## 4. Search area(`shbox`) 후보 탐지

| 항목 | 값 |
|---|---|
| SCREEN | STT00001(실제 업무 화면, 1건) |
| SOURCE_CONTAINER_ID | Div00(버튼) + Div01(필드), 두 Div 쌍 |
| SOURCE_CONTAINER_CLASS | Div00=없음, Div01=`round`(신뢰 불가, 위 3절) |
| CHILDREN | Div01: Static(Label x3, Data x5)+Edit+Calendar+Radio / Div00: Button x2 |
| GEOMETRY_PATTERN | Div00(y 4~31) → Div01(y 32~97) → Grid(y 110~741), 세 형제가 이 순서로 인접·비중첩 |
| BUTTON_RELATION | Div00의 Button 중 하나(`onclick="doSearch"`, text="조회")가 검색 트리거로 보임, Grid 바로 위 |
| SHBOX_CONFIDENCE | **LOW** |

**"위치가 상단이라는 이유만으로 HIGH 금지" 요건에 따른 판단 근거**: 이 패턴은 구조적으로
target `shbox`(필드 테이블 + 조회 버튼, 그 뒤 Grid)와 형태가 유사하지만,
1) **corpus 반복이 아니라 단일 실제 화면 1건**에서만 관측됨(4절 요건 "corpus에서 반복되는
구조"를 만족 못함),
2) source class(`round`)가 discriminator로 못 쓰임(3절에서 반증됨),
3) 검색 버튼(`doSearch`)과 그리드 액션 버튼(엑셀다운)이 **같은 Div, 같은 class**로 섞여 있어
"버튼만 있는 Div=조회버튼 그룹"이라는 규칙만으로는 false positive(엑셀 버튼까지 shbox 로
오인)를 유발할 위험이 있음,
4) Label/Data Static 쌍이 XPlatform Grid 전체에서 흔한 범용 패턴(고정폭 배경+라벨)이라 이
자체만으로 "검색조건 영역"과 "단순 정보 표시 영역"을 구분하지 못함(사용자 지침 6절의
"Label Static + Data 배경 Static" 반복 패턴은 tbbox 후보와도 겹치는 모양이라 형태만으론
구분 불가 -- target 쪽 2절 관찰과 동일한 문제).

따라서 HIGH는 물론 MEDIUM도 아직 부여하지 않는다.

## 5. Grid section(`dfbox`) 후보 탐지

`dfbox`가 필요로 하는 native 구조(제목 Static + 건수 Static + 액션 버튼이 하나의 형제
그룹으로 Grid 바로 위에 위치, `.dfbox > .fr`로 우측 정렬)를 STT00001과 대조한 결과:

```
Grid(id=STT00001) 바로 앞 형제 = Div01(검색 필드)뿐. title Static, count Static 없음.
Grid 자체의 attribute에도 caption/count 관련 필드 없음(binddataset만 존재).
```

```
GRID_SECTION_SOURCE_PATTERN_COUNT = 0   (STT00001에는 dfbox에 대응하는 패턴이 아예 없음)
REPEATED_PATTERN_COUNT = 0
DFBOX_SOURCE_DISCRIMINATOR_CANDIDATE = 없음(NOT_FOUND) -- 이번 실제 evidence 범위 내
CONFIDENCE = 판정 불가(NO_EVIDENCE, LOW보다 낮음)
```

이 화면 자체가 "제목+건수" 헤더 패턴을 쓰지 않는 단순 조회 화면이라는 뜻일 수도 있고,
`dfbox`가 필요한 화면 유형(등록/수정 가능한 목록)이 이번 실제 evidence corpus에 아직
없다는 뜻일 수도 있다 -- 어느 쪽이든 **지금 가진 증거로는 discriminator를 만들 근거 자체가
없다.** 목업이나 추측으로 채우지 않는다.

## 6. Business form/table(`tbbox`) 후보 탐지

`tbbox`는 target에서 "Label(th) + Input(td)" 반복을 감싸는 `w2tb tb` 테이블이며, 검색
버튼 형제가 없는 단독 등록/수정 form에서 쓰인다(1절 BM005M01 evidence).

STT00001의 Div01은 Label+Input 반복 요소를 갖고 있지만:
- 검색조건 Div(위 4절의 shbox 후보)와 **완전히 같은 컨테이너**이며 분리된 두 번째 form
  컨테이너가 없다(즉 이 화면에는 "검색 form"과 "업무 데이터 form"이 따로 존재하지 않고
  하나뿐).
- Div01 내부 좌표는 표 형태로 정렬돼 있지 않다(Label/Data/Input의 x좌표가 5개 필드마다
  제각각이며 균등한 컬럼 폭이 아님) -- target `w2tb`의 `colgroup`(고정 컬럼 폭 테이블)으로
  재구성하려면 좌표 기반 컬럼 추론이 필요한데, 이는 정확히 `GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED`
  가 막고 있는 "좌표 정렬만으로 table 병합" 휴리스틱과 동일한 위험을 가진다.

```
TBBOX_HIGH_CONFIDENCE_SOURCE_PATTERN = NO
TABLE_STRUCTURE_RECONSTRUCTION_SAFE = NO
```

**근거**: (a) 실제 evidence corpus(n=1)에 tbbox에 대응하는 "단독 등록/수정 form" 패턴 사례가
아예 없다(shbox 후보와 미분리 상태), (b) 있다고 가정해도 좌표 기반 컬럼 추론이 필요해
`GENERAL_LAYOUT_TABLE_HEURISTIC_PAUSED=true` 정책과 직접 충돌한다. 이 정책은 이번 라운드
해제하지 않는다(요청사항 11절 금지 사항과 일치).

## 7. Structure-preserving vs structure-generating mapping 분리

```
CLASS_ONLY_MAPPING_CANDIDATES (TYPE 1, 이미 구현/안정):
  - xf:trigger(Button) -> class="btn_cm"           (WebSquareGenerator.java:1391-1392)
  - w2:gridView(Grid)  -> class="wq_gvw"            (WebSquareGenerator.java:1394-1395)
  - xf:select1(appearance=minimal) -> disabledClass="w2selectbox_disabled" (line 1411-1413)
  => 공통점: source XPlatform class/id를 전혀 보지 않는다. target QName(+appearance)
     하나만으로 결정되는 고정 매핑이라 "SOURCE-side discriminator"가 애초에 필요 없다.
     이번 조사의 shbox/dfbox/tbbox와는 근본적으로 다른 난이도의 문제다.

STRUCTURE_GENERATING_MAPPING_CANDIDATES (TYPE 2, 이번 조사 대상):
  - shbox / shbox_inner / btn_shbox : 여러 sibling Div를 하나의 DOM 계층으로 재구성 +
    각 필드를 table tr/th/td로 재배치 필요
  - dfbox / fr / count_box          : 현재 evidence 없음(5절)
  - tbbox / w2tb tb                  : 좌표 기반 컬럼 추론 필요, 정책 충돌(6절)
  => 셋 다 "class 부여"가 아니라 "DOM 구조 자체를 새로 만드는" 작업이라 evidence
     기준이 TYPE 1보다 훨씬 높아야 한다(요청사항과 동일한 결론).
```

## 8. contents.css layout-risk 재확인 (실측)

| CLASS | LAYOUT_AFFECTING | 실제 규칙 요약 | ABSOLUTE_POSITION_CONFLICT_RISK |
|---|---|---|---|
| `shbox` | YES | `display:flex; flex-wrap:wrap; display:grid; grid-template-columns:1fr auto; align-items:center` | **HIGH** |
| `shbox_inner` | YES | `flex:1; min-width:60%; position:relative; overflow:hidden` | **HIGH** |
| `dfbox` | YES | `display:flex; flex-wrap:nowrap; column-gap:4px; align-items:center; width:100%; min-height:24px` | **HIGH** |
| `dfbox > .fr` | YES | `margin-left:auto`(우측 정렬 flex item) | **HIGH** |
| `tbbox` | 경미 | `position:relative; margin:0` (자체는 flex/grid 아님, 내부 `.tb`가 table) | MEDIUM |
| `w2tb`/`.tb` | YES | `width:100%`(HTML `<table>` 렌더링, tagname=table/tr/th/td) | **HIGH** |
| `gvwbox` | 경미 | `position:relative; width:100%; margin-top:10px` | LOW |
| `btnbox` | 경미 | 버튼 스타일 위주(`.fr > *+*{margin-left:4px}`) | LOW |

**결론**: `shbox`/`shbox_inner`/`dfbox`/`w2tb`는 전부 `display:flex` 또는 `display:grid` 또는
HTML `<table>` 렌더링(`tagname="table/tr/th/td"`)이다. 현재 converter가 XPlatform Div 자식
전체에 대해 예외 없이 적용하는 **`position:absolute;left:%;top:%;width:%;height:%`** 방식과는
근본적으로 다른 레이아웃 모델이다. flex/grid 부모 안에 `position:absolute` 자식을 그대로 두면
flex/grid가 그 자식의 크기에 관여하지 않게 되어(absolute 요소는 flow에서 빠짐) 의도한 배치가
깨진다. 즉 **class 토큰만 추가하는 방식(TYPE 1과 동일한 접근)으로는 어떤 shbox/dfbox/tbbox
후보도 안전하게 적용할 수 없다** -- DOM 재구성 + 자식 style을 percent-absolute에서
table-flow/flex-flow로 바꾸는 별도 변환 로직이 반드시 함께 필요하다(이번 라운드 evidence
결론이며, 코드 변경은 하지 않았다).

## 9. Mapping evidence matrix

| TARGET_SEMANTIC | SOURCE_DISCRIMINATOR | CORPUS_MATCH_COUNT | FALSE_POSITIVE_RISK | STRUCTURE_CHANGE_REQUIRED | CSS_LAYOUT_RISK | CONFIDENCE | RECOMMENDATION |
|---|---|---|---|---|---|---|---|
| `btn_cm` | target QName(Button)만 | N/A(이미 구현) | 낮음 | 아니오 | LOW | HIGH | **AUTO**(기존 정책) |
| `wq_gvw` | target QName(Grid)만 | N/A(이미 구현) | 낮음 | 아니오 | LOW | HIGH | **AUTO**(기존 정책) |
| `shbox`/`shbox_inner`/`btn_shbox` | (버튼-only Div) + (Label/Data+Input 반복 Div) + Grid로 즉시 이어지는 3-sibling 순서 | **1/1**(real 화면 기준, corpus 미검증) | HIGH(같은 class`round`가 Grid에도 재사용, 같은 button class가 조회/엑셀 양쪽에 재사용) | 예(DOM 계층 재구성+table 변환) | HIGH | **LOW** | **HOLD** |
| `dfbox`/`fr`/`count_box` | 없음(evidence 미발견) | 0/1 | 판정 불가 | 예 | HIGH | **NO_EVIDENCE** | **HOLD** |
| `tbbox`/`w2tb tb` | 없음(shbox 후보와 미분리, 좌표 기반 컬럼 추론 필요) | 0/1(분리된 사례 없음) | 판정 불가 | 예 + 좌표 휴리스틱 필요 | HIGH | **LOW** | **HOLD** |

## 10. Production 수정 기준 대비 현재 상태

이번 조사에서 확인된 3개 구조 semantic(shbox/dfbox/tbbox) 중 어느 것도 10절의 6개 필요조건을
동시에 만족하지 못한다:

```
corpus 반복 evidence 존재        : shbox만 1건(요건 미달, 나머지 0건)
false positive risk 낮음         : 미달(round class 재사용, 버튼 class 재사용 확인됨)
특정 화면 ID 미사용              : 만족(STT00001 ID/Div01/Div00 hardcoding 없이 패턴만 서술)
source class 하나만의 우연한 매칭 아님 : 판정 불가(반증 사례가 오히려 발견됨, 3절)
contents.css layout 충돌 평가 완료 : 완료(8절) -- 결과는 HIGH 위험
기존 geometry/hierarchy regression 가능성 평가 : 완료 -- 구조 변경 필요, 위험 높음
```

**따라서 이번 라운드는 shbox/dfbox/tbbox 중 어느 것도 다음 구현 후보로 확정하지 않는다.**
가장 근접한 것은 shbox이지만(형제 관계 패턴이 실제로 1건 관측됨), n=1이라 corpus 반복
요건을 충족하지 못하고, 같은 화면 안에서 discriminator 후보(class, 버튼 그룹)의 오탐 사례가
이미 나왔다. HIGH confidence는커녕 SAFE_CANDIDATE로도 올리지 않는다.

## 11. 다음 단계 제안 (Production 수정 아님)

구현이 아니라 **evidence 확보** 자체가 다음 단계여야 한다:

1. STT00001 외의 실제 업무 화면(검색+Grid 구조를 가진 것) 원본을 최소 3~5건 추가로 확보해
   `Div00(버튼-only)+Div01(Label/Data+Input)+Grid` 순서 패턴이 반복되는지, `round`/버튼
   class 재사용이 STT00001만의 우연인지 corpus 차원에서 재확인해야 한다.
2. `dfbox`가 필요한 화면 유형(제목+건수+액션버튼 헤더가 있는 목록 화면)의 실제 XPlatform
   원본을 최소 1건이라도 확보해야 discriminator 조사 자체를 시작할 수 있다(현재 0건).
3. `tbbox`는 "검색 form과 분리된 등록/수정 전용 form"이 존재하는 실제 화면을 확보해야
   shbox 후보와 구조적으로 분리해서 판정할 수 있다.

이 3가지가 채워지기 전까지는 shbox/dfbox/tbbox 전부 **HOLD를 유지하는 것이 옳다** --
현재 증거만으로 구현하면 corpus 밖의 다른 real 화면에서 오탐(예: `round` class를 가진 다른
컨테이너를 shbox로 잘못 판정)이 발생할 위험이 이미 실측으로 확인됐기 때문이다.
