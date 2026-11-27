package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MusicControllerPermissionTest {

    @Test
    void writeEndpoints_shouldRequireMusicPermissions() {
        assertPermission("saveMusicItem", "music:item:add");
        assertPermission("updateMusicItem", "music:item:update");
        assertPermission("deleteMusicItems", "music:item:delete");
        assertPermission("importNeteasePlaylist", "music:library:import");
        assertPermission("resetMusicLibrary", "music:library:reset");
    }

    private void assertPermission(String methodName, String permission) {
        Method method = Arrays.stream(MusicController.class.getDeclaredMethods())
                .filter(item -> item.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        SaCheckPermission annotation = method.getAnnotation(SaCheckPermission.class);
        assertEquals(permission, annotation.value()[0]);
    }
}
