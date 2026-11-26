import VMdPreview from "@kangc/v-md-editor/lib/preview";
import vuepressTheme from "@kangc/v-md-editor/lib/theme/vuepress.js";
import "@kangc/v-md-editor/lib/theme/style/vuepress.css";
import createTodoListPlugin from "@kangc/v-md-editor/lib/plugins/todo-list/index";
import "@kangc/v-md-editor/lib/plugins/todo-list/todo-list.css";
import createKatexPlugin from "@kangc/v-md-editor/lib/plugins/katex/cdn";
import Prism from "prismjs";

VMdPreview.use(vuepressTheme, {Prism})
    .use(createTodoListPlugin())
    .use(createKatexPlugin());

export default VMdPreview;
