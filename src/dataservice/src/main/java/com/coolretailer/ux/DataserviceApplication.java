package com.coolretailer.ux;

import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import brave.grpc.GrpcTracing;
import io.grpc.ServerInterceptor;

@SpringBootApplication
@PropertySource("classpath:git.properties")
public class DataserviceApplication {

	@GRpcGlobalInterceptor
	ServerInterceptor grpcServerSleuthInterceptor(GrpcTracing grpcTracing) {
		return grpcTracing.newServerInterceptor(); 
	}
	
	public static void main(String[] args) {
		SpringApplication.run(DataserviceApplication.class, args);
	}
}
