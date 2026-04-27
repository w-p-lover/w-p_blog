if (!("global" in globalThis)) {
  (globalThis as typeof globalThis & { global: typeof globalThis }).global = globalThis;
}
