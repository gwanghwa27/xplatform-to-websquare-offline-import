# Offline Document Sanitization Log

이 문서는 `docs/` 아래로 복사된 4개 CURRENT/HISTORICAL 문서(`FINAL-VERIFICATION-REPORT.md`,
`followup-checkBox-ready-jdk-phase1-final.md`, `conversion-quality-audit-final.md`,
`phase1-sha-findings.md`)에 대해 수행한 sanitize 작업을 기록한다.

**원본 문서(`work/results/**`)는 수정하지 않았다.** 이 offline-import 프로젝트의 `docs/` 아래 COPY만 아래
내용으로 sanitize했다.

## 검사 범위

`http://`, `https://`, username/password, token, API key, credential, 개인 홈 절대경로
(`C:\Users\...`), license, private key, DB password 패턴을 4개 문서 전체에서 검사.

## 발견 및 처리 내역

| 파일 | 원본 내용 종류 | 처리 |
|---|---|---|
| `FINAL-VERIFICATION-REPORT.md` (34, 35행) | 개발 PC의 IntelliJ JDK 설치 절대경로(사용자명 미포함) | `<LOCAL_PATH_REDACTED>\.jdks\...`로 치환 |
| `FINAL-VERIFICATION-REPORT.md` (42, 117행) | 개발 PC의 Windows 사용자 홈 아래 Downloads 절대경로(Windows 로그인 사용자명 포함) | `<LOCAL_PATH_REDACTED>\WEBSQUARE_DEV_PACK_SP5_edu`로 치환 |
| `FINAL-VERIFICATION-REPORT.md` (362행) | dev pack DB에 시딩되어 있던 실제 테스트 계정의 사번(EMP_CD)과 비밀번호(PASSWORD) | 둘 다 `<DEV_PACK_TEST_CREDENTIAL_REDACTED>`로 치환 (2차 스캔에서 발견 및 수정, 아래 "이력" 참고) |
| `followup-checkBox-ready-jdk-phase1-final.md` (122행) | 개발 PC 절대경로 3건(위와 동일한 두 종류) | 각각 `<LOCAL_PATH_REDACTED>\...`로 치환 |
| `conversion-quality-audit-final.md` | (해당 없음) | 검사 결과 개인 경로/credential 없음 |
| `phase1-sha-findings.md` | (해당 없음) | 검사 결과 개인 경로/credential 없음 |

원본 문자열 자체(Windows 로그인 사용자명, dev pack 테스트 계정 사번/비밀번호 포함)는 이 로그에도
재기록하지 않는다 — 아래 "처리" 결과인 `<LOCAL_PATH_REDACTED>` / `<DEV_PACK_TEST_CREDENTIAL_REDACTED>`
형태만 최종 반입 문서에 남는다.

## 확인된 사항

- **정정 이력**: 최초 스캔에서는 `password|credential|api[_-]?key|private[_-]?key` 등 일반 키워드
  패턴만 사용해 dev pack DB의 실제 시딩 테스트 계정(사번=EMP_CD 값, `PASSWORD=` 필드값)을 놓쳤다. 이후
  `PASSWORD\s*=` 형태의 값 대입 패턴으로 재스캔해 `FINAL-VERIFICATION-REPORT.md` 362행에서 발견,
  즉시 `<DEV_PACK_TEST_CREDENTIAL_REDACTED>`로 치환했다. 이는 폐쇄망 반입 프로젝트 Freeze 이전에 이
  COPY 문서에서만 수정한 것이며, 원본 `work/results/FINAL-VERIFICATION-REPORT.md`는 여전히 미수정
  상태다.
- 위 항목을 제외하면 dev-pack 로그인 ID/PW, DB user/password, license key, API token, private key는
  4개 문서 어디에도 추가로 발견되지 않았다.
- URL(`http://`, `https://`) 패턴도 발견되지 않았다(4개 문서는 모두 `localhost:8080` 등 로컬 참조만
  포함하며, 이는 실행형 network dependency가 아닌 문서 텍스트일 뿐이다).
- historical 문맥(예: 어떤 PC에 dev pack이 설치되어 있었는지, 어떤 테스트 계정으로 로그인 검증했는지)은
  각각 `<LOCAL_PATH_REDACTED>` / `<DEV_PACK_TEST_CREDENTIAL_REDACTED>` 표시로 대체해 "검증을 실제로
  수행했다"는 사실 자체는 보존하면서 실제 경로/사용자명/계정정보는 제거했다.
