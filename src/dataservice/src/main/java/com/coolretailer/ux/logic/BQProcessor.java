package com.coolretailer.ux.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.coolretailer.ux.entity.Product;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BQProcessor implements QueryProcessor {
	@Autowired
	private RedisTemplate<String, String> template;

	@Autowired
	private BigQuery bqInstance;

	private BoundZSetOperations<String, String> getZSetOps() {
		return template.boundZSetOps("autocomplete");
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(BQProcessor.class);

	@NewSpan()
	public <T> List<T> processQuery(String queryString, Class<T> t, boolean updateCache) throws Exception {

		QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(queryString).build();
		// Create a job ID so that we can safely retry.
		JobId jobId = JobId.of(UUID.randomUUID().toString());
		Job queryJob = bqInstance.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());

		// Wait for the query to complete.
		queryJob = queryJob.waitFor();

		// Check for errors
		if (queryJob == null) {
			throw new RuntimeException("Job no longer exists");
		} else if (queryJob.getStatus().getError() != null) {
			// You can also look at queryJob.getStatus().getExecutionErrors() for all
			// errors, not just the latest one.
			throw new RuntimeException(queryJob.getStatus().getError().toString());
		}

		// Get the results.
		TableResult result = queryJob.getQueryResults();
		// init counters
		long count = 0;
		long total = result.getTotalRows();
		LOGGER.info("Fetched " + total + " products.");
		long start = System.currentTimeMillis();
		// Print all pages of the results.
		List<T> results = new ArrayList<T>();
		// filter
		String filter = "[^A-Za-z0-9 ()-]";
		while (result != null) {
			for (List<FieldValue> row : result.iterateAll()) {
				Object type = t.getConstructor().newInstance();
				String productName = null;
				// query for sku, name
				if (type instanceof Product) {
					productName = row.get(1).getValue() != null
							? row.get(1).getStringValue().replaceAll(filter, "").trim()
							: "";
					if (!updateCache) {
						Product product = new Product();
						product.setSku(row.get(0).getValue().toString());

						product.setName(productName);
						results.add(t.cast(product));
					}
					// query for name
				} else if (type instanceof String) {
					productName = row.get(0).getValue() != null
							? row.get(0).getStringValue().replaceAll(filter, "").trim()
							: "";
					if (!updateCache) {
						results.add(t.cast(productName));
					}
				}

				if (updateCache) {
					getZSetOps().add(productName.toLowerCase() + ":" + productName, 0);
				}
				count++;
			}
			LOGGER.info("Processed " + count + " records..");
			result = result.getNextPage();
		}
		if (updateCache) {
			long actual = getZSetOps().zCard();
			LOGGER.info("Indexing completed for " + count + " products.");
			LOGGER.info("Products in cache: " + actual);
			LOGGER.info("Duplicate product names: " + (count - actual));
			LOGGER.info("Time taken: " + (System.currentTimeMillis() - start) / 1000 + "s.");
		}

		return results;

	}

}
