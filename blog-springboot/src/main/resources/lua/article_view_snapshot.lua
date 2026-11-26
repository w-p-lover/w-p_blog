local ids = redis.call('ZRANGE', KEYS[2], 0, tonumber(ARGV[1]) - 1)
local snapshots = {}
for _, id in ipairs(ids) do
    local total = redis.call('ZSCORE', KEYS[1], id)
    if total then
        table.insert(snapshots, id)
        table.insert(snapshots, total)
    else
        -- 累计值被外部清除时无法还原尚未持久化的PV，移除孤立标记。
        redis.call('ZREM', KEYS[2], id)
    end
end
return snapshots
