local quantity = redis.call('HGET', KEYS[1], 'quantity')
local lastEventId = redis.call('HGET', KEYS[1], 'lastEventId')

if not quantity then
    return -1
end

if tonumber(quantity) < tonumber(ARGV[1]) then
    return -2
end

local decreased_quantity = redis.call('HINCRBY', KEYS[1], 'quantity', -tonumber(ARGV[1]))

local eventId = math.max(tonumber(lastEventId), tonumber(ARGV[2]))

redis.call('HSET', KEYS[1], 'lastEventId', tostring(eventId))

return decreased_quantity