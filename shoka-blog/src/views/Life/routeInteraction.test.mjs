import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const lifePage = readFileSync("src/views/Life/index.vue", "utf8");

assert.ok(
  lifePage.includes("@node-double-click=\"handleNodeDoubleClick\""),
  "route detail should open from a node double-click handler"
);

const singleClickHandler = lifePage.match(/const handleNodeClick = \(\{ node \}: \{ node: RouteElement \}\) => \{(?<body>[\s\S]*?)\n\};/);
assert.ok(singleClickHandler, "single-click handler should exist for selecting the active route node");
assert.ok(
  !singleClickHandler.groups.body.includes("dialogVisible.value = true"),
  "single-click handler should not open the detail dialog"
);

const doubleClickHandler = lifePage.match(/const handleNodeDoubleClick = \(\{ node \}: \{ node: RouteElement \}\) => \{(?<body>[\s\S]*?)\n\};/);
assert.ok(doubleClickHandler, "double-click handler should exist");
assert.ok(
  doubleClickHandler.groups.body.includes("dialogVisible.value = true"),
  "double-click handler should open the detail dialog"
);

assert.match(
  lifePage,
  /\.toolbar-btn\s*\{[^}]*min-width:\s*96px/s,
  "toolbar buttons should have a comfortable minimum width"
);

assert.match(
  lifePage,
  /:deep\(\.route-filters \.el-radio-button__inner\)\s*\{[^}]*min-width:\s*76px/s,
  "route filter buttons should have a comfortable minimum width"
);
