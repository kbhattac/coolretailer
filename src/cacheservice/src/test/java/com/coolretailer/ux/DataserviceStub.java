package com.coolretailer.ux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.coolretailer.ux.grpc.system.DataServiceGrpc.DataServiceImplBase;
import com.coolretailer.ux.grpc.system.Query;
import com.coolretailer.ux.grpc.system.Result;

import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import io.grpc.stub.StreamObserver;

/**
 * DataserviceStub
 */
@GRpcService
public class DataserviceStub extends DataServiceImplBase {

    @Autowired
    private RedisTemplate<String, String> template;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataserviceStub.class);

    @Override
    public void runQuery(Query query, StreamObserver<Result> responseObserver) {
        LOGGER.info("Got query: " + query.getQueryString());

        List<String> names = new ArrayList<String>();
        names.add("value1");
        names.add("value2");
        names.add("value3");
        names.add("option1");
        names.add("option2");
        if (query.toString().toUpperCase().contains("LIKE")) {
            LOGGER.info("Extracting LIKE parameter from SQL query...");
            String prefix = query.getQueryString().split("'")[1].substring(0, 2);
            LOGGER.info("Got prefix: " + prefix);
            names = names.stream().filter(name -> name.startsWith(prefix)).collect(Collectors.toList());
        }
        try {

            Result.Builder responseBuilder = Result.newBuilder().addAllName(names);
            LOGGER.info("list: " + names);
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void updateCache(Query query, StreamObserver<Result> responseObserver) {
        LOGGER.info("Got query: " + query.getQueryString());
        try {
            template.boundZSetOps("autocomplete").add("ab:name1", 0);
            template.boundZSetOps("autocomplete").add("ac:name2", 0);
            template.boundZSetOps("autocomplete").add("ad:name3", 0);
            Result.Builder responseBuilder = Result.newBuilder().addAllName(new ArrayList<String>());
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}