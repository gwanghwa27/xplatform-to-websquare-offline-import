# Phase 3 Tab Instance Model

- Converted XML file is shared; each Tab Content/WFrame remains an independent runtime instance.
- Per logical page state contains generation/state/target/arguments/pending operation metadata.
- Page-scoped queues preserve dependent operation order without globally serializing unrelated Tabs.
- URL replacement increments generation and resets child lifecycle; plain selection retains the loaded instance and its Component/DataList/local state.
- removeTabpage destroys the registry entry and dynamic ID/argument references; the monotonic generation counter is retained only to reject stale callbacks after ID reuse.
- Same XFDL can therefore be loaded by multiple scoped WFrames without sharing instance state.
