# Phase 3 Tab Lifecycle Report

- Static LAZY: `alwaysDraw=false`; child lifecycle begins when its Content is rendered/activated.
- Static EAGER: `alwaysDraw=true`; exact parent/child onload ordering remains REAL_RUNTIME_REQUIRED.
- WFrame `onwframeload` means rendering completed, but cross-screen READY additionally requires generated child `__xpRuntimePageReady=true`.
- Generated child wrapper marks READY only after converted Form onload succeeds; onload throw/reject stores `__xpRuntimePageLoadError` and remains non-READY.
- Loaded set_url: WFrame `setSrc(src, dataObject options)` and a new generation; Tab switch alone never calls setSrc.
- Lazy child call: `activateTab` then READY wait. If the caller needs an immediate synchronous value before creation, semantic equivalence is not claimed.

- [WARNING] `TabContent/CircularTabA.xfdl`: TAB SCREEN DEPENDENCY CYCLE: TabContent/CircularTabA.xfdl -> TabContent/CircularTabB.xfdl -> ... -> TabContent/CircularTabA.xfdl
- [WARNING] `TabContent/CircularTabB.xfdl`: TAB SCREEN DEPENDENCY CYCLE: TabContent/CircularTabB.xfdl -> TabContent/CircularTabA.xfdl -> ... -> TabContent/CircularTabB.xfdl
