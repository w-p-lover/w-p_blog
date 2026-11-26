import "@/assets/fonts/font.css";
import "@/assets/styles/index.scss";
import SvgIcon from "@/components/SvgIcon/index.vue";
import * as directive from "@/directive";
import "@/permission";
import router from "@/router";
import {titleChange} from "@/utils/title";
import "nprogress/nprogress.css";
import {createPinia} from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";
import "swiper/css";
import "swiper/css/autoplay";
import "swiper/css/mousewheel";
import "swiper/css/navigation";
import "swiper/css/pagination";
import VueViewer from "v-viewer";
import "viewerjs/dist/viewer.css";
import "virtual:svg-icons-register";
import {createApp, defineAsyncComponent, Directive} from "vue";
import lazyPlugin from "vue3-lazy";

import App from "./App.vue";
import error from "./assets/images/404.gif";
import loading from "./assets/images/load.gif";
import axios from "axios";

axios.defaults.withCredentials = true// Cookie跨域
const app = createApp(App);
Object.keys(directive).forEach((key) => {
    app.directive(key, (directive as { [key: string]: Directive })[key]);
});
const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);
app.component("v-md-preview", defineAsyncComponent(() => import("@/components/MarkdownPreview")));
app.use(pinia);
app.use(router);
app.use(VueViewer);
app.use(lazyPlugin, {
    loading,
    error,
});
app.component("svg-icon", SvgIcon);
app.mount("#app");
titleChange();
