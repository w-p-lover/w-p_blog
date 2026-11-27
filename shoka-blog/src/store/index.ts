import useAppStore from "./modules/app";
import useBlogStore from "./modules/blog";
import useUserStore from "./modules/user";
import useMusicStore from "./modules/music";

const useStore = () => ({
    app: useAppStore(),
    blog: useBlogStore(),
    user: useUserStore(),
    music: useMusicStore(),
});

export default useStore;
