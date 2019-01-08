package com.coolretailer.ux.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.coolretailer.ux.logic.QueryProcessor;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class QueryService {
	@Autowired
	private QueryProcessor queryProcessor;

	@Value("${git.commit.id}")
	private String gitCommitId;

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryService.class);

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

	@NewSpan
	@RequestMapping("/api/fetchProducts")
	public String fetchProducts(@RequestParam("name") String prefix) throws Exception {
		if (prefix.length() < 2) {
			return "";
		}

		String filter = "[^A-Za-z0-9 ()-]";
		LOGGER.info("Fetch suggestions for: " + prefix);
		List<String> suggestions = queryProcessor.getSuggestions(prefix.replaceAll(filter, "").toLowerCase());
		if (!CollectionUtils.isEmpty(suggestions)) {
			LOGGER.info("Found " + suggestions.toString());
			return (new Gson().toJson(suggestions));
		}
		return "[]";

	}

}
