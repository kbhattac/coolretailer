package com.coolretailer.ux.logic;

import java.util.List;

import javax.annotation.PostConstruct;

import com.coolretailer.ux.grpc.system.CacheServiceGrpc;
import com.coolretailer.ux.grpc.system.CacheServiceGrpc.CacheServiceBlockingStub;
import com.coolretailer.ux.grpc.system.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.stereotype.Component;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

@Component
public class QueryProcessor {

	@Value("${grpc.server.name}")
	private String gRpcServerName;

	@Value("${grpc.server.port}")
	private String gRpcServerPort;

	private CacheServiceBlockingStub cacheServiceBlockingStub;
	private final List<ClientInterceptor> clientInterceptorList;

	public QueryProcessor(List<ClientInterceptor> clientInterceptorList) {
		this.clientInterceptorList = clientInterceptorList;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryProcessor.class);

	@PostConstruct
	private void init() {
		ManagedChannel managedChannel = ManagedChannelBuilder
				.forAddress(gRpcServerName, Integer.parseInt(gRpcServerPort)).usePlaintext()
				.intercept(clientInterceptorList).build();
		cacheServiceBlockingStub = CacheServiceGrpc.newBlockingStub(managedChannel);
	}

	@NewSpan
	public List<String> getSuggestions(String searchPrefix) throws Exception {
		LOGGER.info("Calling cacheservice with: " + searchPrefix);
		Query query = Query.newBuilder().setQueryString(searchPrefix).build();
		return cacheServiceBlockingStub.getSuggestions(query).getNameList();

	}

}
