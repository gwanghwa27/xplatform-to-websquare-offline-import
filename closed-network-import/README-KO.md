# 폐쇄망 1회 반입 가이드 (One-shot Closed Network Import)

> **폐쇄망 반입 대상은 물리적으로 1개 파일이다**: 이 저장소 루트를 통째로
> 압축한 `closed-network-candidate-<commit>.zip`(예:
> `closed-network-candidate-8443582.zip`, 동봉된 `.zip.sha256`으로 무결성
> 확인) **하나만** 반입한다. **`closed-network-import/` 폴더만 따로
> 복사하지 말 것** -- 이 폴더는 소스를 담고 있지 않다(검증 kit일 뿐이며,
> `src/main/java` 등 실제 소스는 저장소 루트의 다른 디렉터리에 있다). ZIP을
> 풀면 최상위에 `v6-design-structure-alignment-<commit>/` 디렉터리 하나가
> 나오고, 그 안에 `BUILD-CANDIDATE-INFO.txt`(어느 commit인지 식별)와 전체
> editable source가 함께 들어있다.

이 문서는 `candidate/v6-design-structure-alignment` 브랜치를 폐쇄망에
**한 번만** 반입해서 build → conversion → regression → class-policy 검증까지
마치고, 이후 폐쇄망 WebSquare Studio에서 최종 확인할 수 있도록 안내한다.
인터넷 문서 링크에 의존하지 않는다 -- 필요한 모든 것은 이 저장소 안에
있다.

## 0. 중요 -- 이 저장소 자체가 이미 self-contained project다

이 `closed-network-import/` 디렉터리는 별도의 파일 복사본 묶음이
**아니다**. `candidate/v6-design-structure-alignment` 브랜치(git 저장소)
전체가 이미 완전한 독립 프로젝트다(`build.sh`/`build.bat`,
`convert-sample.sh`/`.bat`, `verify-offline.sh`/`.bat`, `src/`,
`sample-phase3-project/`, `analysis/` 등 전부 저장소 root에 존재). 따라서:

**"1회 반입" = 이 git 브랜치(또는 이 커밋 시점의 전체 디렉터리)를 폐쇄망에
한 번 복사/체크아웃하는 것 자체다.** 이 디렉터리는 그 반입 이후 실행할
**무결성 확인 + 빌드/회귀 + class-policy 검증을 자동화하는 kit**이며,
저장소를 다시 쪼개어 담지 않는다(요청사항의 "기존 프로젝트 오염 최소화"
원칙과 "완전한 candidate working-copy" 요건을 동시에 만족하는 방식 --
이미 전체가 working-copy이므로 이중으로 담지 않는다).

## 1. 반입 위치

폐쇄망 Windows WebSquare 개발 환경에서, 기존 candidate 저장소를 두는
위치(예: `C:\work\xplatform-to-websquare-offline-import\`)에 이 브랜치
전체를 복사한다. 경로 자체는 자유롭게 선택 가능 -- 공백/한글 경로도
지원한다(하위 도구들이 전부 quoting을 지킴).

## 2. 기존 project backup 방법

반입 전, 기존에 이미 폐쇄망에 있던 이전 candidate 사본이 있다면:

```
robocopy "C:\work\xplatform-to-websquare-offline-import" "C:\work\xplatform-to-websquare-offline-import.bak-YYYYMMDD" /E
```
(또는 폴더 전체를 다른 이름으로 복사해 두는 것으로 충분 -- git 저장소이므로
`.git` 히스토리 자체가 이미 이전 상태의 백업이기도 하다.)

## 3. 변경 source 적용 방법

이 브랜치(`candidate/v6-design-structure-alignment`, 이번 라운드 기준 최신
커밋)를 그대로 폐쇄망에 복사하면 끝이다. 별도 patch 적용 단계가 없다 --
"몇 개 파일만 덮어쓰기" 방식이 아니라 전체 디렉터리 자체가 최신 상태다.

이번 라운드에서 실제로 바뀐 파일(참고용, 별도 조치 불필요):
- `src/main/java/com/example/xfdltracker/converter/WebSquareGenerator.java`
  (Combo `disabledClass` 결정을 generic policy 함수로 리팩터링)
- `resources/target-websquare/WebContent/assets/css/contents.css`
  (직전 라운드, canonical CSS reference-only copy -- **REFERENCE_ONLY**,
  아래 4번 참고)
- `analysis/*.md`, `analysis/*.diff`(문서, 코드 아님)

## 4. Canonical contents.css에 대해 -- REFERENCE_ONLY, 자동 배포 아님

`resources/target-websquare/WebContent/assets/css/contents.css`는 실제
폐쇄망 WebSquare 프로젝트의 `\WebContent\assets\css\contents.css`를
그대로 보관한 **참조용 사본**이다. contents.css는 이미
`websquare/config.xml`의 `<stylesheet earlyImportList="...">` 설정을 통해
폐쇄망 프로젝트에 전역 로딩되고 있음이 확인됐다(`analysis/
contents-css-integration-audit.md`). **이 candidate 저장소는 실제 운영
`WebContent/assets/css/contents.css`를 자동으로 덮어쓰지 않는다** -- 이
converter는 CSS 파일을 배포하는 코드를 포함하지 않는다. 필요하면 아래
SHA 비교로 두 파일이 같은지만 확인한다:

```
certutil -hashfile "C:\실제프로젝트경로\WebContent\assets\css\contents.css" SHA256
certutil -hashfile "resources\target-websquare\WebContent\assets\css\contents.css" SHA256
```
두 SHA가 같으면(`9634dbcd506d3eeaf1a238e4157059d6c3c4c2facdd85039ba8b46a30c9bcd62`)
동일 파일이다. 다르면 실제 운영 파일이 canonical이며, 이 사본은 참조용일
뿐 실제 파일을 임의로 교체하지 않는다.

## 5. Build 방법

폐쇄망 JDK 1.8.0_111 기준(exact JDK 요구사항, `verify-offline.sh`/`.bat`
참고):

```
cd C:\work\xplatform-to-websquare-offline-import
build.bat
```
(Windows) 또는 `sh build.sh`(WSL/Git Bash 있는 경우).

## 6. Converter 실행 방법

```
convert-sample.bat
```
`sample-phase3-project` 149개 XFDL을 `build\sample-output`으로 변환한다
(136개 XML 생성 기대).

실제 업무 프로젝트를 변환하려면:
```
java -cp build\classes com.example.xfdltracker.project.XPlatformProjectConverter ^
  "<실제 XPlatform 프로젝트 경로>" "<출력 경로>" UTF-8
```

## 7. Regression 방법 (이번 라운드 자동화 스크립트)

```
closed-network-import\BUILD-AND-VERIFY.cmd
```
(Windows, 네트워크 접근 불필요) 또는
```
sh closed-network-import/BUILD-AND-VERIFY.sh
```
(Git Bash/WSL 있는 경우)

한 번 실행으로: MANIFEST.sha256 무결성 → clean compile → 149/149
conversion → class/state policy invariant(`btn_cm=12`/`wq_gvw=3`/
`w2selectbox_disabled=4`) → HOLD structural class 미유출 확인 → XML
well-formed(python 있으면) → Phase1 SHA verifier(python 있으면)까지
전부 수행하고 마지막에 `ALL GATES PASS`/`ONE OR MORE GATES FAILED`를
출력한다.

기존 `verify-offline.sh`/`.bat`(exact JDK 1.8.0_111 gate 포함, 더 엄격한
공식 게이트)도 별도로 실행 가능:
```
sh verify-offline.sh
```

**MANIFEST 관련 참고**: `MANIFEST.sha256`는 텍스트 파일(`.java`/`.md`/
`.sh` 등)의 줄바꿈(LF/CRLF)에 영향을 받는다. git의 `core.autocrlf` 설정이
반입 환경에서 다르면(예: 이 candidate를 만든 개발 환경과 폐쇄망 Windows
환경의 git 설정 차이) MANIFEST 비교에서 **텍스트 파일만** mismatch로
표시될 수 있다 -- 이는 실제 내용 손상이 아니라 줄바꿈 정규화 차이일
가능성이 높다(빌드/실행에는 영향 없음, javac/node/python 전부 CRLF와 LF를
동일하게 처리한다). 반면 `resources/target-websquare/WebContent/assets/
css/contents.css`(REFERENCE_ONLY, 원본이 LF-only)처럼 byte-exact 여부가
중요한 파일은 mismatch가 나오면 반드시 직접 SHA 값을 비교해 확인한다
(4번 항목의 canonical SHA `9634dbcd506d3eeaf1a238e4157059d6c3c4c2facdd85039ba8b46a30c9bcd62`
참고). `.java` 소스 파일 mismatch는 clean compile이 실제로 성공하는지로
교차 확인하면 된다(내용이 실제로 손상됐다면 컴파일이 실패한다).

## 8. STT00030 생성 방법 (실제 화면 sample evidence 재현, 선택)

실제 업무 화면 `STT00030.xfdl`이 있다면(이 저장소 corpus에는 포함돼 있지
않음 -- 실제 업무 데이터이므로 별도 보관):
```
mkdir stt-test\Form
copy "<실제 STT00030.xfdl 경로>" stt-test\Form\STT00030.xfdl
java -cp build\classes com.example.xfdltracker.project.XPlatformProjectConverter stt-test build\stt-output UTF-8
```
생성된 `build\stt-output\Form\STT00030.xml`에서 다음을 확인:
- `Div01`/`Div00`/`Div02`/`Div03` → `xf:group`(무변경)
- `Div02`/`Div03`의 `style`에 `background:...` 보존 여부
- `Div01_MNG_BOCD`(Combo) → `disabledClass="w2selectbox_disabled"` 포함

## 9. Studio에서 확인할 항목

1. `Div01`(Calendar/Combo) 표시 여부
2. `Div00`(조회/엑셀 버튼) 표시 여부
3. `Div02`/`Div03`(우측 버튼 4개씩) 표시 여부 -- 특히 배경색이 이제
   보이는지(`background:#ffEEEfff;`/`background: #ffffffff;` 보존 확인)
4. Combo(`Div01_MNG_BOCD`) disabled 상태일 때 `w2selectbox_disabled`
   스타일(회색 배경, `#bdbeca` 텍스트)이 실제로 적용되는지
5. Button/Grid(`btn_cm`/`wq_gvw`) 기존 스타일이 그대로 유지되는지(회귀
   없음 확인용)
6. 전체 화면 layout(percentage geometry)이 이전 라운드 대비 달라지지
   않았는지

## 10. 실패 시 rollback 방법

- 이 candidate 디렉터리 전체를 지우고 2번에서 만든 backup으로 복원한다.
- 또는 git 저장소라면: `git log --oneline`으로 이전 커밋 확인 후
  `git checkout <이전 커밋 SHA>`로 되돌린다(이 저장소는 각 라운드가
  개별 커밋이라 세밀한 rollback이 가능하다).
- `WebContent/assets/css/contents.css`(실제 운영 파일)는 이 candidate가
  건드리지 않으므로 별도 rollback이 필요 없다(4번 참고).

## 참고 문서

- `analysis/contents-css-integration-audit.md` -- CSS 전역 로딩/base
  widget class 조사
- `analysis/target-class-state-policy-audit.md` -- 이번 라운드 class/
  state policy 리팩터링 상세
- `analysis/freeze-vs-candidate-function-diff.md` -- 전체 함수 단위
  diff 이력(모든 라운드)
- `README-OFFLINE.md`, `OFFLINE-IMPORT-MANIFEST.md` -- 이 candidate
  저장소 자체의 폐쇄망 반입 원칙(기존 문서, freeze 시점부터 존재)
