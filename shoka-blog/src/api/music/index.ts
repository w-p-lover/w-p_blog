import { Result } from "@/model";
import request from "@/utils/request";
import { AxiosPromise } from "axios";
import type { MusicFilter, MusicItem, MusicLibraryPayload } from "@/views/Music/musicModel";

export type MusicQuery = Partial<MusicFilter> & {
  sortType?: string;
};

export type MusicItemForm = Partial<MusicItem> & {
  title: string;
  artist: string;
};

export interface MusicImportForm {
  source: string;
  playlistName?: string;
}

export function getMusicLibrary(params: MusicQuery = {}): AxiosPromise<Result<MusicLibraryPayload>> {
  return request({
    url: "/music/library",
    method: "get",
    params,
  });
}

export function addMusicItem(data: MusicItemForm): AxiosPromise<Result<MusicItem>> {
  return request({
    url: "/music/item",
    method: "post",
    data,
  });
}

export function updateMusicItem(id: string, data: MusicItemForm): AxiosPromise<Result<MusicItem>> {
  return request({
    url: `/music/item/${id}`,
    method: "put",
    data,
  });
}

export function deleteMusicItems(ids: string[]): AxiosPromise<Result<null>> {
  return request({
    url: "/music/item",
    method: "delete",
    data: ids,
  });
}

export function importNeteasePlaylist(data: MusicImportForm): AxiosPromise<Result<MusicLibraryPayload>> {
  return request({
    url: "/music/import/netease",
    method: "post",
    data,
  });
}

export function resetMusicLibrary(): AxiosPromise<Result<null>> {
  return request({
    url: "/music/library/reset",
    method: "delete",
  });
}
