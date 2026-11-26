-- KEYS: 正文缓存、失效代。ARGV: 装载前失效代、序列化正文、TTL秒、无失效代哨兵。
-- 参数沿用 RedisTemplate 的值序列化器，写入的正文与普通缓存读保持兼容。
local generation = redis.call('GET', KEYS[2]) or ARGV[4]
if generation ~= ARGV[1] then
    return 0
end
redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
return 1
