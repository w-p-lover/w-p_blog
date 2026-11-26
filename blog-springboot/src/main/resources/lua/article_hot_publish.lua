if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return 0
end
local expected = tonumber(ARGV[2])
if expected == 0 then
    -- 空榜是成功查询后的明确结果；依赖查询失败不会走到这里。
    redis.call('DEL', KEYS[3])
else
    if redis.call('ZCARD', KEYS[2]) ~= expected then
        return -1
    end
    redis.call('RENAME', KEYS[2], KEYS[3])
    -- RENAME携带临时TTL，正式榜必须移除它。
    redis.call('PERSIST', KEYS[3])
end
return 1
