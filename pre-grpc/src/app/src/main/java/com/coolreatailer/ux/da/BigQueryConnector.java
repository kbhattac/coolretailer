package com.coolreatailer.ux.da;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(BigQueryConnector.class);

	@Autowired
	CredentialsProvider credentialsProvider;

	@Autowired
	GcpProjectIdProvider porjectIdProvider;

	@Bean
	public BigQuery getInstance() throws IOException {
		// projectId needs to be set explicitly even if it's there in the json key!!
		BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId(porjectIdProvider.getProjectId())
				.setCredentials(credentialsProvider.getCredentials()).build().getService();

		// Use the client.
		LOGGER.info("Datasets:");
		for (Dataset dataset : bigQuery.listDatasets().iterateAll()) {
			LOGGER.info(dataset.getDatasetId().getDataset());
		}
		return bigQuery;

	}

}
