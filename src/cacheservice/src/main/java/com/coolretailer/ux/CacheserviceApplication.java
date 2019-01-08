package com.coolretailer.ux;

import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import brave.grpc.GrpcTracing;
import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;

@SpringBootApplication
@PropertySource("classpath:git.properties")
public class CacheserviceApplication {


	@GRpcGlobalInterceptor
	ServerInterceptor grpcServerSleuthInterceptor(GrpcTracing grpcTracing) {
		return grpcTracing.newServerInterceptor(); 
	}
	
	@Bean
	ClientInterceptor grpcClientSleuthInterceptor(GrpcTracing grpcTracing) {
		return grpcTracing.newClientInterceptor();
	}
	public static void main(String[] args) {
		SpringApplication.run(CacheserviceApplication.class, args);
	}
}
