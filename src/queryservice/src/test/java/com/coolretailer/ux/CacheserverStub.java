package com.coolretailer.ux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.coolretailer.ux.grpc.system.CacheServiceGrpc.CacheServiceImplBase;
import com.coolretailer.ux.grpc.system.Query;
import com.coolretailer.ux.grpc.system.Result;

import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;

/**
 * CacheserverStub
 */
@GRpcService
public class CacheserverStub extends CacheServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheserverStub.class);

    @Override
    public void getSuggestions(Query query, StreamObserver<Result> responseObserver) {
        LOGGER.info("Got query: " + query.getQueryString());

        List<String> names = new ArrayList<String>();
        names.add("value1");
        names.add("value2");
        names.add("value3");
        names.add("option1");
        names.add("option2");

        names = names.stream()
            .filter(name -> name.startsWith(query.getQueryString()))
            .collect(Collectors.toList());
            
        try {
            Result.Builder responseBuilder = Result.newBuilder().addAllName(names);
            LOGGER.info("list: " + names);
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}