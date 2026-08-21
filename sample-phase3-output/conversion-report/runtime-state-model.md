# Phase 3 Runtime State Model

States: `NOT_CREATED → CREATED → LOADING → LOADED → READY`; replacement uses `REPLACING`, removal uses `DESTROYING → DESTROYED`, failures use `ERROR`.

Each logical page tracks runtime ID, source/target, generation, arguments, selected flag, last operation, pending operation count and error. A URL replacement changes generation; a plain Tab switch does not replace the child instance. Remove clears instance/dynamic ID/argument registries while generation counters remain monotonic to reject stale callbacks.
