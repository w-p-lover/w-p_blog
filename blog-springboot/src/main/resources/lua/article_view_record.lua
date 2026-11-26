-- 参数使用StringRedisTemplate；既有Integer成员的JSON表示也是十进制数字。
-- Lua运行时错误不回滚已执行写入，先检查两个键类型，避免错误类型导致半次记录。
for _, key in ipairs(KEYS) do
    local kind = redis.call('TYPE', key).ok
    if kind ~= 'none' and kind ~= 'zset' then
        return redis.error_reply('article view key must be a zset')
    end
end
local current = tonumber(redis.call('ZSCORE', KEYS[1], ARGV[1]) or '0')
local total = math.min(math.max(current, tonumber(ARGV[2])) + 1, 2147483647)
local time = redis.call('TIME')
local now = tonumber(time[1]) * 1000 + math.floor(tonumber(time[2]) / 1000)
redis.call('ZADD', KEYS[1], total, ARGV[1])
-- 首次待同步时间不被高频阅读刷新，避免热点不断移到队尾。
redis.call('ZADD', KEYS[2], 'NX', now, ARGV[1])
return total
