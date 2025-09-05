
/**
 * 游戏
 */
export interface Game {
    id: number;
    name: string;
    coverUrl: string;
    description?: string;
    isInstalled?: boolean;
    playTime?: string;
    lastPlayed?: string;
    tags?: string[];
    rating?: number;
    releaseDate?: string;
    developer?: string;
}

