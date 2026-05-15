import useAppStore from "./modules/app";
import useBlogStore from "./modules/blog";
import useMusicStore from "./modules/music";
import useUserStore from "./modules/user";

const useStore = () => ({
    app: useAppStore(),
    blog: useBlogStore(),
    music: useMusicStore(),
    user: useUserStore(),
});

export default useStore;
