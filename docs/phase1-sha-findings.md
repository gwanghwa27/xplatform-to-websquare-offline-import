# Phase 1 SHA-256 Regression — UNRESOLVED

> **HISTORICAL / SUPERSEDED — Historical evidence only.**
> Phase1 SHA는 후속 조사에서 extraction provenance가 확정되어 **STATIC_VERIFIED / PASS**로 해소됨.
> 이 문서는 그 이전 시점(UNRESOLVED)의 조사 기록 보존용이며, **최신 판정 source로 사용 금지**.
> 최신 상태는 `followup-checkBox-ready-jdk-phase1-final.md` §4 및 `FINAL-VERIFICATION-REPORT.md`를 참고.

## Expected (from CURRENT-CHANGES / PHASE4-REGRESSION.md / PHASE3-REGRESSION-RESULT.md)
- Sample.xfdl script SHA-256: f82379cfb619d611ae4137032af43fd10faf3df88f018ca5db3b72c490f4d3fe
- CommentProtection.xfdl script SHA-256: 14f3466acde50241698ccf21edec5807464a2a6e903854c78ed59332c7b2b987

## Actually executed
- Converted sample/Sample.xfdl -> Sample.xml via XfdlToWebSquare (real run, exit 0)
- Converted sample-edge/CommentProtection.xfdl -> CommentProtection.xml via XfdlToWebSquare (real run, exit 0)
- Extracted <script> CDATA content by multiple reasonable methods and hashed:

| Method | Sample.xml | CommentProtection.xml |
|---|---|---|
| XML-parse itertext (LF-normalized by parser) | a3d947cb128a08dd05853df3de95f731487856c9da01e20c8335327d0646f1b2 | ce07af78920e3aeae8b39bbf7ba39cf82702621f809f3d96e157da9c93255671 |
| Raw CDATA bytes, CRLF preserved | ebd254e328403112dbc6470e79ad80923522be4bdac7f17ea879eb06f2949d90 | a032a47d3843ba3a6b642410c26475273b64847a86aad60f61e6df105bb854fe |
| Raw CDATA bytes, CRLF->LF | 51c785ee95788dadbda5a6de40ca3c67ced88b4239c3479d005fefc4e7de62e8 | 9c3d4b22c2f68500d52821c7eccc625a382a3dd1d6461cfa6522e9e047c22678 |
| Raw CDATA bytes, stripped, CRLF | 7ae9b3ab11ca7f8cfbd2fb28ac0cc66e4468f314c0c9b53a9def4846bd270267 | 21c25b590185a999661c8ea6bc755341f33b7812d57dbe23ab2533525fb8ec13 |
| Raw CDATA bytes, stripped, LF | 71cdfcf2d92a8f3f34901fc352abfb0128dea11c0f5870c0acba18d5b1eb6098 | fcfe4d0b9728f2b31d5c1063c0101a5fe88fe39bc00ccd59a805e5c3f33beba6 |
| Whole generated XML file | 4dc9bb7fd9f0f6a0933ed1318d463bc383330ccf72933000f0ab15c8b6dec9d4 | 29432b231982dd075e52dfc75772ce3c9dd1db17124f2569408ff2fbda23eed7 |
| Source XFDL <Script> element only (LF-normalized) | 0d66e47b00ef131c27a5f51492f9406d2687db292dfd911b8b5166110daad116 | b009ed50c8545f3edc12f3d099632cd3b83d2c8e92839c752d68b00c69516f7c |

None of the tried extraction conventions reproduce the documented expected hash.

## Conclusion
No script/tool exists inside either ZIP that documents or reproduces the exact extraction procedure used to
produce the "expected" hash values in CURRENT-CHANGES.md / PHASE3-REGRESSION-RESULT.md / PHASE4-REGRESSION.md.
The actual conversion runs succeeded (exit 0, plausible output), but the SHA-256 comparison against the
documented expected values cannot be confirmed as PASS or FAIL — classification: **UNRESOLVED**, not PASS.
Expected files/docs were not modified to match current results, per instruction.
