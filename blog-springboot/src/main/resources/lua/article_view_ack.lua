-- 读取快照后发生的新阅读必须留待下一轮，不用GET后DEL的分离操作。
local current = redis.call('ZSCORE', KEYS[1], ARGV[1])
if current and tonumber(current) == tonumber(ARGV[2]) then
    return redis.call('ZREM', KEYS[2], ARGV[1])
end
return 0
