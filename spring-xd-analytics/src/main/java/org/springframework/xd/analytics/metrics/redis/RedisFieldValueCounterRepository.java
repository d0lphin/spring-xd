
package org.springframework.xd.analytics.metrics.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;

public class RedisFieldValueCounterRepository implements FieldValueCounterRepository {

	protected final String metricPrefix;

	protected final StringRedisTemplate redisTemplate;

	private static final String MARKER = "_marker_";

	public RedisFieldValueCounterRepository(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, "fieldvaluecounters.");
	}

	public RedisFieldValueCounterRepository(RedisConnectionFactory connectionFactory, String metricPrefix) {
		Assert.notNull(connectionFactory);
		Assert.hasText(metricPrefix, "metric prefix cannot be empty");
		this.metricPrefix = metricPrefix;
		redisTemplate = new StringRedisTemplate();
		// avoids proxy
		redisTemplate.setExposeConnection(true);
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();
	}

	@Override
	public <S extends FieldValueCounter> S save(S fieldValueCounter) {
		String counterKey = getMetricKey(fieldValueCounter.getName());
		if (this.redisTemplate.opsForValue().get(counterKey) == null) {
			if (fieldValueCounter.getFieldValueCount().size() > 0) {
				for (Map.Entry<String, Double> entry : fieldValueCounter.getFieldValueCount().entrySet()) {
					increment(fieldValueCounter.getName(), entry.getKey(), entry.getValue());
				}
			}
			else {
				increment(fieldValueCounter.getName(), MARKER, 0);
			}
		}
		// else TODO decide behavior
		return fieldValueCounter;

	}

	@Override
	public <S extends FieldValueCounter> Iterable<S> save(Iterable<S> metrics) {
		List<S> results = new ArrayList<S>();
		for (S m : metrics) {
			results.add(save(m));
		}
		return results;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the FieldValueCounter must not be null");
		this.redisTemplate.delete(getMetricKey(name));
	}

	@Override
	public void delete(FieldValueCounter fieldValueCounter) {
		Assert.notNull(fieldValueCounter, "The FieldValueCounter must not be null");
		this.redisTemplate.delete(getMetricKey(fieldValueCounter.getName()));

	}

	@Override
	public void delete(Iterable<? extends FieldValueCounter> fvcs) {
		for (FieldValueCounter fvc : fvcs) {
			delete(fvc);
		}
	}

	@Override
	public FieldValueCounter findOne(String name) {
		Assert.notNull(name, "The name of the FieldValueCounter must not be null");
		String metricKey = getMetricKey(name);
		if (redisTemplate.hasKey(metricKey)) {
			Map<String, Double> values = getZSetData(metricKey);
			FieldValueCounter c = new FieldValueCounter(name, values);
			return c;
		}
		else {
			return null;
		}
	}

	@Override
	public boolean exists(String s) {
		return findOne(s) != null;
	}

	@Override
	public List<FieldValueCounter> findAll() {
		List<FieldValueCounter> counters = new ArrayList<FieldValueCounter>();
		// TODO asking for keys is not recommended. See
		// http://redis.io/commands/keys
		Set<String> keys = this.redisTemplate.keys(this.metricPrefix + "*");
		for (String key : keys) {
			// TODO remove this naming convention for minute aggregates
			if (!key.matches(this.metricPrefix
					+ ".+?_\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}:\\d{2}")) {
				if (this.redisTemplate.type(key) == DataType.ZSET) {
					Map<String, Double> values = getZSetData(key);
					String name = key.substring(metricPrefix.length());
					FieldValueCounter c = new FieldValueCounter(name, values);
					counters.add(c);
				}
			}
		}
		return counters;
	}

	@Override
	public Iterable<FieldValueCounter> findAll(Iterable<String> keys) {
		List<FieldValueCounter> results = new ArrayList<FieldValueCounter>();

		for (String k : keys) {
			FieldValueCounter value = findOne(k);
			if (value != null) {
				results.add(value);
			}
		}
		return results;
	}

	@Override
	public long count() {
		return findAll().size();
	}

	@Override
	public void deleteAll() {
		Set<String> keys = redisTemplate.keys(metricPrefix + "*");
		if (keys.size() > 0) {
			redisTemplate.delete(keys);
		}
	}

	public void increment(String counterName, String fieldName) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, 1.0);
	}

	public void increment(String counterName, String fieldName, double score) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, score);
	}

	public void decrement(String counterName, String fieldName) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, -1.0);
	}


	public void decrement(String counterName, String fieldName, double score) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, -score);
	}

	public void reset(String counterName, String fieldName) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).remove(fieldName);
	}


	/**
	 * Provides the key for a named metric. By default this appends the name to the metricPrefix value.
	 * 
	 * @param metricName the name of the metric
	 * @return the redis key under which the metric is stored
	 */
	protected String getMetricKey(String metricName) {
		return metricPrefix + metricName;
	}

	protected Map<String, Double> getZSetData(String counterKey) {
		// TODO directly serialize into a Map vs Set of TypedTuples to avoid extra copy
		Set<TypedTuple<String>> rangeWithScore = this.redisTemplate
				.boundZSetOps(counterKey).rangeWithScores(0, -1);
		Map<String, Double> values = new HashMap<String, Double>(
				rangeWithScore.size());
		for (Iterator<TypedTuple<String>> iterator = rangeWithScore.iterator(); iterator
				.hasNext();) {
			TypedTuple<String> typedTuple = iterator.next();
			if (!typedTuple.getValue().equals(MARKER)) {
				values.put(typedTuple.getValue(), typedTuple.getScore());
			}
		}
		return values;
	}

}
