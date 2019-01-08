package com.coolretailer.ux.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class DataService {

	@Value("${git.commit.id}")
	private String gitCommitId;

	private static final Logger LOGGER = LoggerFactory.getLogger(DataService.class);

	@RequestMapping("/health")
	public String healthCheck() {
		return "OK";
	}

	@RequestMapping("/api/buildInfo")
	public String getBuildInfo() {
		LOGGER.info("Build info: " + gitCommitId);
		return gitCommitId;
	}

	@RequestMapping("/api")
	public String getEndpointStatus() {
		return "OK";
	}
}
