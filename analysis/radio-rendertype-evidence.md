# Radio(xf:select1 appearance="full") renderType Evidence

## 배경

사용자가 실제 WebSquare AI v6 Studio에서 특정 화면(STT00001.xml)의 Radio
컴포넌트(`Div01_Radio00`, source XPlatform `Radio` → target `xf:select1
appearance="full"`) 표현이 이상하다고 보고했다(스크린샷 첨부, 넓은 빈
영역만 보임). 생성된 XML은:

```xml
<xf:select1 appearance="full" ev:onchange="scwin.Div01_Radio00_onitemchanged"
    id="Div01_Radio00" style="position:absolute;left:7.6%;top:-15.0%;width:24.5%;height:125.0%;"
    tabIndex="11" value="0"/>
```

self-closing(자식 없음) -- `<xf:choices>`도 `renderType`도 없다.

## 조사 방법

로컬 devpack(`websquare-devpack-copy/tomcat/webapps/ROOT`)에는 XPlatform
변환물이 아닌, **순수 v6 native로 작성된 실제 업무 화면**(`ui/BM/*.xml`,
`ui/HM/*.xml`, `ui/SP/*.xml`, 총 26개 파일)이 배포돼 있다. 이 corpus
전체에서 `<xf:select1 ...>` 태그를 전수 스캔(정규식, 여러 줄에 걸친 속성
포함)해 `appearance` 값별 `renderType` 존재 여부를 집계했다.

## 결과 (전수, 예외 없음)

```
appearance="full"(Radio 계열):     7/7  100% renderType="radiogroup"
appearance="minimal"(Combo 계열): 47건 중 renderType="native" 3건(6%),
                                    나머지 44건은 renderType 자체가 없음
```

`appearance="full"`인 7건은 전부 아래처럼 `renderType="radiogroup"`을
명시한다(예: `ui/BM/BM001M01.xml:426`):

```xml
<xf:select1 appearance="full" cols="" disabled="" id="" ref="data:dma_search.IS_USE"
    renderType="radiogroup" rows="" selectedIndex="0" style="">
    <xf:choices>
        <xf:item><xf:label><![CDATA[전체]]></xf:label><xf:value><![CDATA[]]></xf:value></xf:item>
        <xf:item><xf:label><![CDATA[사용]]></xf:label><xf:value><![CDATA[Y]]></xf:value></xf:item>
        <xf:item><xf:label><![CDATA[미사용]]></xf:label><xf:value><![CDATA[N]]></xf:value></xf:item>
    </xf:choices>
</xf:select1>
```

동일 파일들에서 `appearance="minimal"`은 `renderType`이 있는 경우
(`renderType="native"`)와 없는 경우가 섞여 있어 -- Combo는 이 attribute
없이도 정상적으로 dropdown shell이 그려지는 것으로 보인다(evidence
부족으로 매핑하지 않음, HOLD).

## 반례 검토

devpack에 포함된 `ui/phase4test/Form/DatasetBinding.xml`(테스트/튜토리얼
성격 fixture, "phase4test"라는 디렉터리명 자체가 업무 화면이 아님을
시사)에는 `renderType` 없이 `appearance="full"` + 런타임
`setNodeSet()`만 쓰는 예(`rdoCode`)가 1건 있다. 그러나 이 파일은 실제
업무 화면(BM/HM, 사용자가 "목표 형태"로 지목한 1~5번 이미지와 동일한
shbox/dfbox/tbbox/btn_cm 구조를 가진 화면들)과 성격이 다르고, 업무 화면
쪽 evidence가 7/7로 100% 일관되므로 업무 화면 evidence를 채택했다.

## 결론 및 조치

`renderType="radiogroup"`을 XPlatform `Radio` → `xf:select1
appearance="full"` 변환 시 함께 emit하도록 수정(`WebSquareGenerator.
applyComponentSpecificProperties`의 Radio 분기, 신규 policy 함수
`resolveTargetRenderType(String targetTag, String appearance)` -- 기존
`resolveVideoEvidenceBaseClass`/`resolveVideoEvidenceDisabledClass`와
동일한 QName(+appearance) 기반 lookup 구조, source 화면명 조건 없음).

itemset이 정적 `<xf:choices>`가 아니라 런타임 `setNodeSet()` JS 호출로만
채워지는 기존 방식(Combo/ListBox/Radio 공통, `WebSquareGenerator.
applyBindings`)은 이번 조사에서 변경하지 않았다 -- devpack
`DatasetBinding.xml`(`cboCode.setNodeSet(...)`/`rdoCode.setNodeSet(...)`)이
동일한 패턴을 실제 devpack 예제로 보여주므로, 이 패턴 자체는 WebSquare가
지원하는 정상 API 사용법으로 판단(변경 불필요). 다만 이 방식은 Studio
Design-time(JS 미실행)에서는 item이 안 보일 수 있다는 한계가 있고, 이는
Radio/Combo 공통이며 이번 화면의 "이상함"과 별개로 renderType 누락이
1차 원인일 가능성이 높다(설계 근거 상 renderType 없이는 위젯 자체가
radiogroup으로 렌더링되지 않을 것으로 판단, item 유무와 무관).

STT00001.xfdl 원본 소스는 이 저장소/세션에 없어(사용자 실사용 프로젝트
파일, 미제공) `Div01_Radio00`의 itemset이 원래 비어 있었는지, dataset
binding이 있었는지는 이번 조사로 확정할 수 없다 -- renderType 수정 반영
후에도 화면이 비어 있다면 그것은 별도의, 이 화면 고유의 itemset binding
문제일 가능성이 있으므로 폐쇄망에서 실제 Studio로 재확인이 필요하다
(화면별 하드코딩 없이 generic fix만 적용했으므로, 폐쇄망 재확인 결과에
따라 추가 화면별 조치가 필요할 수 있다는 뜻이며 이는 이번 fix의 정당성과
무관).

```
RADIO_RENDERING = FIX_CANDIDATE (renderType 누락 → 추가, 7/7 devpack
  evidence 기반, generic, QName+appearance 조건, 화면별 예외 없음)
STUDIO_DESIGN_VERIFIED = 아직 아님(폐쇄망 Studio 재확인 필요)
```

## 후속 -- 폐쇄망 Studio 재검증 결과: renderType 단독으로는 불충분 (2026-08-25)

이 문서의 결론(`renderType="radiogroup"` 추가)을 실제로 커밋(`a5403fa`)
해서 사용자가 폐쇄망 WebSquare Studio에서 재검증했다. 결과:

```
ALL GATES = PASS(정적 regression)
RADIO_RENDERING = STUDIO_FAILED
renderType="radiogroup" = NO_VISIBLE_EFFECT
```

즉 이 문서가 제시한 evidence(native corpus 7/7이 renderType을 가짐)
자체는 여전히 유효하지만(그런 값을 실제로 갖고 있다는 관찰은 사실),
"renderType만 추가하면 Radio가 정상 렌더링된다"는 **가설은 실제
Studio 재현으로 기각됐다.** 같은 native corpus 7/7 전수조사를 더 깊이
반복한 결과, renderType 외에 **정적 `<xf:choices><xf:item>` 구조**가
7/7 전부에 공통으로 있었다는 것을 이번에는 놓쳤다는 것이 확인됐다 --
당시엔 `appearance`/`renderType` 속성값 비교에만 집중해 자식 요소
구조까지 비교하지 못했다.

실제 root cause(item 구조 부재)와 후속 fix는
`analysis/radio-rendering-root-cause.md`에 있다. 이 문서(renderType
evidence 자체)는 삭제하지 않고 그대로 남긴다 -- renderType이 native
패턴의 일부라는 사실 자체는 여전히 유효하고 이번 fix에서도 유지했다
(`RENDERTYPE_RADIOGROUP_POLICY = KEEP_BUT_INSUFFICIENT`).

```
RADIO_RENDERING(이 문서 최초 결론) = FIX_CANDIDATE -> STUDIO_FAILED로
  실측 기각됨(2026-08-25) -> 후속 fix 이후 다시 FIX_CANDIDATE로 재판정
  (analysis/radio-rendering-root-cause.md 12절)
```
