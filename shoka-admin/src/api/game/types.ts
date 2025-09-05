export interface GameVO {
    id: number;
    name: string;
    coverUrl: string;
    description: string;
    isInstalled: boolean;
    playTime: string;
    lastPlayed: string;
    tags: string[];
    rating: number;
    releaseDate: string;
    developer: string;
}

export interface GameForm {
    name: string;
    coverUrl: string;
    description: string;
    isInstalled: boolean;
    playTime: string;
    lastPlayed: string;
    tags: string[];
    rating: number;
    releaseDate: string;
    developer: string;
}

export interface GameQuery {
    current: number;
    size: number;
    keyword?: string;
}
