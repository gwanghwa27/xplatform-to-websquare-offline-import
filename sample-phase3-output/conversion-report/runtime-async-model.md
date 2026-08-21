# Phase 3 Runtime Async Model

- Queue scope: TabControl + logical TabPage key.
- READY gate: child `scwin.__xpRuntimePageReady === true`; WFrame existence alone is not READY.
- `setSrc`/`addTab`/`deleteTab`/`activateTab` return values are checked at runtime; thenables are chained, synchronous returns remain synchronous where safe.
- Loaded child function calls preserve synchronous return when no pending page operation exists.
- Lazy child function calls may return a Promise after activation/READY wait; synchronous Component/DataList reads before READY raise `UNSUPPORTED_SYNC_SEMANTIC`.
- Generation/state checks ignore callbacks from removed/replaced instances.
- Runtime operations analyzed: 80; runtime-verification cross-screen refs: 20.
