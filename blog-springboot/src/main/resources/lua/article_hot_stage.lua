-- 每批至多100篇，构建阶段从不改读者正在读取的正式榜单。
if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return 0
end
for i = 2, #ARGV, 2 do
    local score = tonumber(ARGV[i + 1])
    if not score or score ~= score or score == math.huge or score == -math.huge then
        return redis.error_reply('invalid hot score')
    end
end
for i = 2, #ARGV, 2 do
    redis.call('ZADD', KEYS[2], ARGV[i + 1], ARGV[i])
    -- 首条创建后立即挂TTL；进程中断不会留下无限期临时榜单。
    if i == 2 then
        redis.call('EXPIRE', KEYS[2], 120)
    end
end
return 1
