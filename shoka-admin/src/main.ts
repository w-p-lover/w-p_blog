import Echarts from "@/components/Echarts/index.vue";
import Pagination from "@/components/Pagination/index.vue";
import RightToolbar from "@/components/RightToolbar/index.vue";
import SvgIcon from "@/components/SvgIcon/index.vue";
import createEmojiPlugin from "@kangc/v-md-editor/lib/plugins/emoji/index";
import createTodoListPlugin from "@kangc/v-md-editor/lib/plugins/todo-list/index";
import VMdEditor from "@kangc/v-md-editor";
import Prism from "prismjs";
import VueViewer from "v-viewer";
import vuepressTheme from "@kangc/v-md-editor/lib/theme/vuepress.js";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import * as directive from "@/directive";

import router from "@/router";
import ElementPlus from "element-plus";
import CalendarHeatmap from "@/components/CalendarHeatmap/index.vue";

import "@/assets/styles/index.scss";
import "@/assets/styles/button.scss"
import "element-plus/theme-chalk/index.css";
import "element-plus/theme-chalk/dark/css-vars.css";
import "nprogress/nprogress.css";
import "@/permission";
import "@kangc/v-md-editor/lib/plugins/emoji/emoji.css";
import "@kangc/v-md-editor/lib/plugins/todo-list/todo-list.css";
import "@kangc/v-md-editor/lib/style/base-editor.css";
import "@kangc/v-md-editor/lib/theme/style/vuepress.css";
import "viewerjs/dist/viewer.css";
import "virtual:svg-icons-register";

import {createApp, Directive} from "vue";
import {createPinia} from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";

import App from "./App.vue";
const app = createApp(App);
const pinia = createPinia();

VMdEditor.use(vuepressTheme, {Prism}).use(createEmojiPlugin()).use(createTodoListPlugin());
Object.keys(directive).forEach((key) => {
    app.directive(key, (directive as { [key: string]: Directive })[key]);
});

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}
pinia.use(piniaPluginPersistedstate);
app.use(pinia);
app.use(router);
app.use(VMdEditor);
app.use(VueViewer);
app.component("CalendarHeatmap", CalendarHeatmap);
app.component("svg-icon", SvgIcon);
app.component("Pagination", Pagination);
app.component("RightToolbar", RightToolbar);
app.component("Echarts", Echarts);
app.use(ElementPlus);
app.mount("#app");
