package com.coolreatailer.ux.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class CacheProcessor implements ApplicationRunner {
	@Autowired
	private QueryProcessor qProcessor;

	@Autowired
	private RedisTemplate<String, String> template;

	private BoundZSetOperations<String, String> getZSetOps() {
		return template.boundZSetOps("autocomplete");
	}

	private BoundZSetOperations<String, String> getZSetOpsForMissingProducts() {
		return template.boundZSetOps("missing");
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CacheProcessor.class);

	@PostConstruct
	private void init() {
		LOGGER.info("#records: " + getZSetOps().zCard());
	}

	public void run(ApplicationArguments args) throws Exception {
		if (args.containsOption("clear-cache")) {
			clearCache();
		}

		if (args.containsOption("process-cache")) {
			List<String> cacheOptions = args.getOptionValues("process-cache");
			if (cacheOptions.size() == 1) {
				processCache(cacheOptions.get(0));
			} else {
				processCache(null);
			}
		}
	}

	@NewSpan
	public List<String> getSuggestions(String searchPrefix) throws Exception {

		// Check in missing list first
		if (getZSetOpsForMissingProducts().rank(searchPrefix) != null) {
			return null;
		}

		Set<String> suggestions = getZSetOps().rangeByLex(Range.range().gte(searchPrefix).lte(searchPrefix + "xff"),
				Limit.limit().offset(0).count(10));
		// not found in cache
		if (suggestions.size() == 0) {
			String queryString = "SELECT name FROM coolretailer.products where LOWER(name) like '"
					+ searchPrefix.toLowerCase() + "%' LIMIT 10";
			LOGGER.info("Cache miss :-( hitting BQ..");
			List<String> results = fetchFromStore(queryString, false);
			// not found in BQ
			if (CollectionUtils.isEmpty(results)) {
				LOGGER.info("No results from BQ. Adding to cache as missing product..");
				getZSetOpsForMissingProducts().add(searchPrefix.toLowerCase(), 0);
			}
			return results;
		}
		List<String> results = new ArrayList<>();
		Iterator<String> iterator = suggestions.iterator();
		while (iterator.hasNext()) {
			String suggestion = iterator.next();
			// product name is after ":"
			suggestion = suggestion.split(":")[1];
			results.add(suggestion);
		}
		return results;
	}

	@NewSpan
	public List<String> processCache(String limit) throws Exception {
		LOGGER.info("Calling BigQuery...");
		LOGGER.info("Starting to index...");
		StringBuffer qb = new StringBuffer("SELECT name FROM coolretailer.products");
		if (limit != null) {
			qb.append(" LIMIT " + limit);

		}
		return fetchFromStore(qb.toString(), true);

	}

	@NewSpan
	public void clearCache() {
		template.delete("autocomplete");
		LOGGER.info("Cleared cache.");
	}

	private List<String> fetchFromStore(String query, boolean updateCache) throws Exception {
		return qProcessor.runQuery(query.toString(), String.class, updateCache);
	}

}
