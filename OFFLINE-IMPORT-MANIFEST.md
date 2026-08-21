# Offline Import Manifest (Freeze XPWS-OFFLINE-FREEZE-20260820-02)

반입 심사용 manifest. 이전 Freeze(`XPWS-OFFLINE-FREEZE-20260820-01`)의 manifest를 계승하며,
`work/closed-network-support/candidates/v6-root-body-structure/`에서 확정/승격된 1건의 Root/Body
구조 변경(사용자 폐쇄망 최종 검증 통과)을 추가로 반영한다.

## Project

- Project name: `xplatform-to-websquare-offline-import`
- Generated date: 2026-08-20
- Charset: UTF-8
- Target JDK: 1.8.0_111 (exact)
- Source of this project: `XPWS-OFFLINE-FREEZE-20260820-01`(이전 Freeze) →
  `work/closed-network-support/candidates/v6-root-body-structure/working-copy/`(사용자 폐쇄망
  최종 검증 통과 상태) — COPY only, 이전 Freeze는 전혀 수정되지 않음
- Phase4 original ZIP baseline: IMMUTABLE/FROZEN, 이번에도 전혀 접근하지 않음
- `xplatform-to-websquare-offline-import.zip`(SHA-256
  `740ae706966d53d05b69ef6f064eff974d15b0aad9542e78f70b75334d8fe59c`): IMMUTABLE, 이번에도
  전혀 수정하지 않음

## File Counts

| 항목 | 개수 |
|---|---|
| Production Java source | 76 |
| Verifier Java source | 1 |
| XFDL | 135 |
| XJS | 14 |
| Reference XML(`sample-phase3-output/`, 이번 Freeze 생성 시점에 새로 재변환) | 136 |
| Standalone/common JS | 15 |
| .class | 0 |
| .jar | 0 |
| External dependency | 0 |
| Maven/Gradle | none |

## 포함된 Production 변경 16건 (이전 15건 + 이번에 추가된 1건)

이전 Freeze(`XPWS-OFFLINE-FREEZE-20260820-01`)와 동일(변경 없음, 15건) — 상세는
`../FREEZE-MANIFEST-20260820-01.md` 참고.

이번 Freeze에서 새로 반영(`work/closed-network-support/candidates/v6-root-body-structure/`, 1건,
Production diff는 정확히 3개 위치):

| # | Track | [클래스명] 함수명 | 변경 요약 | Final Verification Level |
|---|---|---|---|---|
| 16 | Root/Body 구조 (`V6_STRUCTURE_PARTIAL_ALIGNMENT`) | `[WebSquareGenerator] generate` / `[WebSquareGenerator] appendBody` / `[ComponentLayoutConverter] buildMainAreaStyle`(신규 함수) | `body > grp_content` 단일 루트를 `body > grp_resultArea > grp_main > grp_content`(기존 content 100% 유지)로 확장. `grp_content`는 componentIdMap/Tab-runtime 호환을 위해 그대로 유지 | `FIXED` / `STUDIO_DESIGN_VERIFIED` / `REGRESSION_VERIFIED` / `PATCH_READY`(현재 폐쇄망 v6 환경 기준, 사용자 직접 확인) |

**중요**: 위 검증은 Design/정적 attribute·구조 확인이며, 실제 interaction/runtime 동작까지
확인된 것은 아니다(`REAL_RUNTIME_VERIFIED`는 이번에도 선언하지 않음). Pre-freeze scan 결과 신규
wrapper id(`grp_resultArea`/`grp_main`)에 대한 기존 script/XJS reference는 발견되지 않았다
(`NEW_WRAPPER_SCRIPT_REFERENCE = 0`) — runtime risk가 0으로 증명된 것은 아니며, 단지 기존
script/XJS에서 이 wrapper-ID를 참조하는 코드가 발견되지 않았다는 의미다.

## 현재 known product/runtime gaps (6건, 변경 없음)

1. **Defect 2** — `CONTENT_NOT_READY` false-negative: **OPEN / CONTRACT_LIMITATION**
2. **GRID-3** — 다중 Format(default/alternate) 전환: **UNSUPPORTED_SEMANTIC**
3. **CheckBox dataset-bound**: **OPEN**
4. **`ev:onpageload` reliability**: **OBSERVED**
5. **`V5_RUNTIME_REGRESSION_REQUIRED`** — `TabRuntimeScriptGenerator`의
   `component('grp_content').getScope()`가 `xf:group` root에서 실제 v5 런타임에 동작하는지 미검증
   (이번 Root/Body 구조 변경으로 `grp_content`의 id/namespace/직접 스크립트 계약은 변경되지
   않았으므로 이 gap 자체는 이번 변경으로 새로 발생한 것이 아니다 — 기존 gap 그대로 승계)
6. **`CLASS_MERGE_RUNTIME_REQUIRED`**(`NON_BLOCKING_CURRENT_CORPUS`) — cssclass+btn_cm/wq_gvw
   병합 실사례가 corpus/폐쇄망 확인 대상 화면에 없어 미검증

## 이 Freeze에 포함되지 않은 것

`req`/`tal`/`tac`/`tar`/`w2tb_th`/`w2tb_td`/`w2tb_tb`/`dfbox`/`fl`/`df_tit`/`lybox`/
`ly_column`/`fr`, Calendar 추가 property 7종, Static `w2:textbox` 전환, `xf:anchor` 매핑, Table
구조 변환(`TABLE_LAYOUT_IMPLEMENTATION = DEFERRED`)은 이 Freeze에 포함되지 않는다(evidence
부족/구조적 아키텍처 갭/A-B 테스트 필요로 보류). Static QName은 이 Freeze를 기준으로 별도 A/B
diagnostic candidate(`work/closed-network-support/candidates/v6-static-qname-ab/`)에서
evidence-only로 준비한다 — **Production mapping은 변경하지 않는다**(`STATIC_QNAME_A_B_REQUIRED`).

## Base Freeze 무결성 참고사항 (informational, 이번 Freeze content에는 영향 없음)

직전 Base Freeze(`XPWS-OFFLINE-FREEZE-20260820-01`) 디렉터리에는 이후 별도 read-only 감사
작업 중 발생한 untracked 부산물 파일 `logs/converter.log`가 존재한다
(`BASE_FREEZE_DIRECTORY_CONTAMINATION = UNTRACKED_EXTRA_FILE`). 해당 파일은
`FREEZE-SHA256SUMS-20260820-01.txt`(445 entries, tracked 파일 기준 100% PASS)에 포함되지 않으며,
tracked 파일 전체는 여전히 무결성이 확인된다(`TRACKED_FREEZE_INTEGRITY = PASS`). 이 파일은 이전
Freeze 디렉터리를 직접 수정하지 않기 위해 그대로 두었으며, 이번 Freeze의 content/manifest/ZIP
어디에도 승계되지 않는다.

## Freeze 산출물

- Frozen 디렉터리: `xplatform-to-websquare-offline-import-FROZEN-20260820-02/`
- Frozen ZIP: `xplatform-to-websquare-offline-import-FROZEN-20260820-02.zip`
- Frozen 파일 inventory: `FREEZE-SHA256SUMS-20260820-02.txt`
- Frozen ZIP SHA-256: `xplatform-to-websquare-offline-import-FROZEN-20260820-02.zip.sha256` 참고
