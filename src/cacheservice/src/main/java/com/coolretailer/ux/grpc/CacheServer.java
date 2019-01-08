package com.coolretailer.ux.grpc;

import java.util.ArrayList;
import java.util.List;

import com.coolretailer.ux.grpc.system.CacheServiceGrpc.CacheServiceImplBase;
import com.coolretailer.ux.grpc.system.Empty;
import com.coolretailer.ux.grpc.system.Limit;
import com.coolretailer.ux.grpc.system.Query;
import com.coolretailer.ux.grpc.system.Result;
import com.coolretailer.ux.logic.CacheProcessor;

import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.util.CollectionUtils;

import io.grpc.stub.StreamObserver;

/**
 * DataServer
 */
@GRpcService
public class CacheServer extends CacheServiceImplBase {

    @Autowired
    private CacheProcessor cacheProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheServer.class);

    @NewSpan
    @Override
    public void getSuggestions(Query query, StreamObserver<Result> responseObserver) {
        LOGGER.info("Got prefix: " + query.getQueryString());
        try {

            List<String> suggestions = cacheProcessor.getSuggestions(query.getQueryString());
            if (CollectionUtils.isEmpty(suggestions)) {
                suggestions = new ArrayList<String>();
            }
            Result.Builder responseBuilder = Result.newBuilder()
                    .addAllName(suggestions);
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Exception: " + e);
        }
    }

    @NewSpan
    @Override
    public void processCache(Limit limit, StreamObserver<Empty> responseObserver) {
        LOGGER.info("Loading " + limit.getLimit() + " items to cache...");
        try {
            cacheProcessor.processCache(String.valueOf(limit.getLimit()));
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Exception: " + e);
        }
    }

    @NewSpan
    @Override
    public void clearCache(Empty empty, StreamObserver<Empty> responseObserver) {
        LOGGER.info("Clearing cache: ");
        try {
            cacheProcessor.clearCache();
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Exception: " + e);
        }
    }

}