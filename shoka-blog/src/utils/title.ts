import {useEventListener} from "@vueuse/core";

export function titleChange() {
    // 动态标题
    let OriginTitle: string = document.title;
    let titleTime: NodeJS.Timeout;
    useEventListener(document, "visibilitychange", () => {
        if (document.hidden) {
            document.title = "(´-ω-｀)呆呆的等着 ";
            clearTimeout(titleTime);
        } else {
            //返回当前页面时标签显示内容
            document.title = "Ciallo～(∠·ω< )⌒★欢迎！";
            //两秒后变回正常标题
            titleTime = setTimeout(() => {
                document.title = OriginTitle;
            }, 2000);
        }
    });
}
