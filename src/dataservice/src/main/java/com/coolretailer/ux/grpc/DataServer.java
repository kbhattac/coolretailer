package com.coolretailer.ux.grpc;

import com.coolretailer.ux.grpc.system.DataServiceGrpc.DataServiceImplBase;
import com.coolretailer.ux.grpc.system.Query;
import com.coolretailer.ux.grpc.system.Result;
import com.coolretailer.ux.logic.BQProcessor;

import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import io.grpc.stub.StreamObserver;

/**
 * DataServer
 */
@GRpcService
public class DataServer extends DataServiceImplBase {

    @Autowired
    private BQProcessor bqProcessor;

    private static final Logger LOGGER = LoggerFactory.getLogger(BQProcessor.class);



    @NewSpan
    @Override
    public void runQuery(Query query, StreamObserver<Result> responseObserver) {
        LOGGER.info("Got query: " + query);
        try {
            Result.Builder responseBuilder = Result.newBuilder()
                    .addAllName(bqProcessor.processQuery(query.getQueryString(), String.class, false));
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Exception: " + e);
        }
    }

    @NewSpan
    @Override
    public void updateCache(Query query, StreamObserver<Result> responseObserver) {
        LOGGER.info("Got query: " + query);
        try {
            Result.Builder responseBuilder = Result.newBuilder()
                    .addAllName(bqProcessor.processQuery(query.getQueryString(), String.class, true));
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error("Exception: " + e);
        }
    }

}