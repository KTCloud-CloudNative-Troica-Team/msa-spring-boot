local lastEventId = redis.call('HGET', KEYS[1], 'lastEventId')

if lastEventId and tonumber(lastEventId) >= tonumber(ARGV[2]) then
    return -3
end

redis.call('HSET', KEYS[1], 'quantity', ARGV[1], 'lastEventId', ARGV[2])

return tonumber(ARGV[1])