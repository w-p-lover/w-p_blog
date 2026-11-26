-- 先切换失效代，再删除正文；同一个原子脚本中完成，旧装载不能跨代回填。
-- 失效代不设TTL，避免旧代过期/重置后出现ABA；清理需与所有在途装载隔离。
redis.call('SET', KEYS[2], ARGV[1])
return redis.call('DEL', KEYS[1])
