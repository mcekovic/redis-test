package org.strangeforest.test.redis;

import java.util.*;

import redis.clients.jedis.*;

public class TxRedisTwoWayMap implements TwoWayMap {

	private final Jedis jedis;

	public TxRedisTwoWayMap(Jedis Jedis) {
		this.jedis = Jedis;
	}

	@Override public Set<String> get1(String key1) {
		return jedis.smembers(redisKey1(key1));
	}

	@Override public Set<String> get2(String key2) {
		return jedis.smembers(redisKey2(key2));
	}

	@Override public void put(String key1, String... keys2) {
		var tx = jedis.multi();
		tx.sadd(redisKey1(key1), keys2);
		for (var key2 : keys2)
			tx.sadd(redisKey2(key2), key1);
		tx.exec();
	}

	@Override public void remove1(String key1) {
		var pipeline = jedis.pipelined();
		var redisKey1 = redisKey1(key1);
		pipeline.watch(redisKey1);
		var keys2 = pipeline.smembers(redisKey1);
		pipeline.sync();
		var tx = jedis.multi();
		for (var key2 : keys2.get())
			tx.srem(redisKey2(key2), key1);
		tx.del(redisKey1);
		tx.exec();
	}

	@Override public void remove2(String key2) {
		var pipeline = jedis.pipelined();
		var redisKey2 = redisKey2(key2);
		pipeline.watch(redisKey2);
		var keys1 = pipeline.smembers(redisKey2);
		pipeline.sync();
		var tx = jedis.multi();
		for (var key1 : keys1.get())
			tx.srem(redisKey1(key1), key2);
		tx.del(redisKey2);
		tx.exec();
	}

	private String redisKey1(String key1) {
		return "txmap/1/" + key1;
	}

	private String redisKey2(String key2) {
		return "txmap/2/" + key2;
	}
}
