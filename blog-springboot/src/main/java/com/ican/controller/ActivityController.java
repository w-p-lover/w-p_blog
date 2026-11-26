package com.ican.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.ican.annotation.OptLogger;
import com.ican.annotation.RateLimit;
import com.ican.model.dto.ActivityRegistrationDTO;
import com.ican.model.dto.ActivitySaveDTO;
import com.ican.model.vo.*;
import com.ican.service.impl.ActivityRegistrationExecutionService;
import com.ican.service.impl.ActivityService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.ican.constant.OptTypeConstant.ADD;
import static com.ican.constant.OptTypeConstant.UPDATE;

@RestController
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "blog.registration", name = "enabled", havingValue = "true")
public class ActivityController {
    private final ActivityService activities;
    private final ActivityRegistrationExecutionService execution;

    @GetMapping("/activities")
    public Result<PageResult<ActivityVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(activities.list(false, page, size));
    }

    @GetMapping("/activities/{id}")
    public Result<ActivityVO> detail(@PathVariable @Positive Integer id) {
        return Result.success(activities.detail(id));
    }

    @SaCheckLogin
    @RateLimit(key = "api:activity:register:#{#id}", limit = 100, period = 1)
    @PostMapping("/activities/{id}/registration")
    public Result<ActivityRegistrationVO> register(@PathVariable @Positive Integer id,
            @Validated @RequestBody ActivityRegistrationDTO dto) {
        return Result.success(activities.submit(id, StpUtil.getLoginIdAsInt(), dto.getRequestId()));
    }

    @SaCheckLogin
    @GetMapping("/activities/{id}/registration")
    public Result<ActivityRegistrationVO> mine(@PathVariable @Positive Integer id) {
        return Result.success(activities.mine(id, StpUtil.getLoginIdAsInt()));
    }

    @SaCheckLogin
    @DeleteMapping("/activities/{id}/registration")
    public Result<ActivityRegistrationVO> cancel(@PathVariable @Positive Integer id) {
        return Result.success(execution.cancel(id, StpUtil.getLoginIdAsInt()));
    }

    @SaCheckPermission("activity:event:list")
    @GetMapping("/admin/activities")
    public Result<PageResult<ActivityVO>> adminList(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(activities.list(true, page, size));
    }

    @SaCheckPermission("activity:registration:list")
    @GetMapping("/admin/activities/{id}/registrations")
    public Result<PageResult<AdminActivityRegistrationVO>> registrations(@PathVariable @Positive Integer id,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(activities.registrations(id, status, page, size));
    }

    @OptLogger(ADD)
    @SaCheckPermission("activity:event:add")
    @PostMapping("/admin/activities")
    public Result<ActivityVO> create(@Validated @RequestBody ActivitySaveDTO dto) {
        return Result.success(activities.create(dto, StpUtil.getLoginIdAsInt()));
    }

    @OptLogger(UPDATE)
    @SaCheckPermission("activity:event:update")
    @PutMapping("/admin/activities/{id}")
    public Result<?> update(@PathVariable @Positive Integer id, @Validated @RequestBody ActivitySaveDTO dto) {
        activities.update(id, dto);
        return Result.success();
    }

    @OptLogger(UPDATE)
    @SaCheckPermission("activity:event:publish")
    @PostMapping("/admin/activities/{id}/publish")
    public Result<?> publish(@PathVariable @Positive Integer id) {
        activities.publish(id);
        return Result.success();
    }

    @OptLogger(UPDATE)
    @SaCheckPermission("activity:registration:retry")
    @PostMapping("/admin/activities/registrations/{requestId}/retry")
    public Result<ActivityRegistrationVO> retry(@PathVariable
            @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                    message = "requestId必须为UUID") String requestId) {
        return Result.success(execution.retry(UUID.fromString(requestId).toString()));
    }
}
