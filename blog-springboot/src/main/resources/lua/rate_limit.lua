-- 滑动窗口限流 Lua 脚本
-- KEYS[1]: 限流 key
-- ARGV[1]: 限流阈值（limit）
-- ARGV[2]: 时间窗口大小（period，秒）
-- ARGV[3]: 当前时间戳（毫秒）

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local period = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- 计算时间窗口的起始时间戳
local windowStart = now - period * 1000

-- 移除窗口外的旧数据
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- 获取当前窗口内的请求数量
local current = redis.call('ZCARD', key)

if current < limit then
    -- 未超过限流阈值，记录本次请求
    redis.call('ZADD', key, now, now)
    -- 设置 key 的过期时间（防止内存泄漏）
    redis.call('EXPIRE', key, period)
    return 1  -- 允许通过
else
    -- 超过限流阈值
    return 0  -- 拒绝请求
end
