-- 滑动窗口限流 Lua 脚本
-- KEYS[1]: 限流 key
-- ARGV[1]: 限流阈值（limit）
-- ARGV[2]: 时间窗口大小（period，秒）
-- ARGV[3]: 每次调用唯一的请求成员

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local period = tonumber(ARGV[2])
local requestId = ARGV[3]
-- 使用 Redis 时间，避免不同应用实例的时钟偏差改变同一窗口。
local redisTime = redis.call('TIME')
local now = tonumber(redisTime[1]) * 1000 + math.floor(tonumber(redisTime[2]) / 1000)

-- 计算时间窗口的起始时间戳
local windowStart = now - period * 1000

-- 移除窗口外的旧数据
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- 获取当前窗口内的请求数量
local current = redis.call('ZCARD', key)

if current < limit then
    -- 未超过限流阈值，记录本次请求
    redis.call('ZADD', key, now, requestId)
    -- 设置 key 的过期时间（防止内存泄漏）
    redis.call('EXPIRE', key, period)
    return 1  -- 允许通过
else
    -- 超过限流阈值
    return 0  -- 拒绝请求
end
