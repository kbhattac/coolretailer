package com.coolreatailer.ux.controller;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coolreatailer.ux.logic.CacheProcessor;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.autoconfigure.logging.StackdriverTraceConstants;
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
	private CacheProcessor cacheProcessor;

	@Value("${git.commit.id}")
	private String gitCommitId;

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryService.class);

	@RequestMapping("/health")
	public String healthCheck(HttpServletResponse response) throws IOException {
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

	@NewSpan()
	@RequestMapping("/api/fetchProducts")
	public String fetchProducts(@RequestParam("name") String prefix, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (prefix.length() < 2) {
			return "";
		}
		response.setHeader("x-b3-traceid", org.slf4j.MDC.get(StackdriverTraceConstants.MDC_FIELD_TRACE_ID));
		response.setHeader("x-b3-spanid", org.slf4j.MDC.get(StackdriverTraceConstants.MDC_FIELD_SPAN_ID));

		response.setHeader("x-b3-sampled", "1");
		response.setHeader("x-b3-flags", "1");	
		response.setHeader("x-request-id", request.getHeader("x-request-id"));
		response.setHeader("x-client-trace-id", UUID.randomUUID().toString());	


		String filter = "[^A-Za-z0-9 ()-]";
		LOGGER.info("Fetch suggestions for: " + prefix);
		List<String> suggestions = cacheProcessor.getSuggestions(prefix.replaceAll(filter, "").toLowerCase());
		if (!CollectionUtils.isEmpty(suggestions)) {
			LOGGER.info("Found " + suggestions.toString());
			return (new Gson().toJson(suggestions));
		}
		return "[]";

	}
}
