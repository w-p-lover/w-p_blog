package com.ican.controller;

import com.ican.cache.MultiLevelCacheManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 缓存管理控制器
 * 提供缓存统计、清理等管理功能
 *
 * @author ican
 */
@Tag(name = "缓存管理")
@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
public class CacheController {

    private final MultiLevelCacheManager cacheManager;

    @Operation(summary = "获取缓存统计信息")
    @GetMapping("/stats")
    public String getCacheStats() {
        return cacheManager.getStats();
    }

    @Operation(summary = "清空 L1 本地缓存")
    @DeleteMapping("/clear")
    public String clearCache() {
        cacheManager.clear();
        return "L1 缓存已清空";
    }

    @Operation(summary = "删除指定缓存")
    @DeleteMapping("/evict")
    public String evictCache(@RequestParam String key) {
        cacheManager.evict(key);
        return "缓存已删除: " + key;
    }
}
