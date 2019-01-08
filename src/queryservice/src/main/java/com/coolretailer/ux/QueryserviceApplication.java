package com.coolretailer.ux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import brave.grpc.GrpcTracing;
import io.grpc.ClientInterceptor;

@SpringBootApplication
@PropertySource("classpath:git.properties")
public class QueryserviceApplication {


	@Bean
	ClientInterceptor grpcClientSleuthInterceptor(GrpcTracing grpcTracing) {
		return grpcTracing.newClientInterceptor();
	}
	public static void main(String[] args) {
		SpringApplication.run(QueryserviceApplication.class, args);
	}
}
