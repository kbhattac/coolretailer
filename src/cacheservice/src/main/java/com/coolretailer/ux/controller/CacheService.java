package com.coolretailer.ux.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class CacheService {

	@Value("${git.commit.id}")
	private String gitCommitId;	

	@RequestMapping("/health")
	public String healthCheck() {
		return "OK";
	}

	@RequestMapping("/api/buildInfo")
	public String getBuildInfo() {
		return gitCommitId;
	}

	@RequestMapping("/api")
	public String getEndpointStatus() {
		return "OK";
	}

}
