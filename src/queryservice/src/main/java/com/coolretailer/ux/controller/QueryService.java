package com.coolretailer.ux.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
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

import brave.Tracer;

@CrossOrigin
@RestController
public class QueryService {
	@Autowired
	private QueryProcessor queryProcessor;

	@Autowired
	Tracer tracer;

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
	public String fetchProducts(@RequestParam("name") String prefix, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (prefix.length() < 2) {
			return "";
		}
		processHeaders(request, response);

		String filter = "[^A-Za-z0-9 ()-]";
		LOGGER.info("Fetch suggestions for: " + prefix);
		List<String> suggestions = queryProcessor.getSuggestions(prefix.replaceAll(filter, "").toLowerCase());
		if (!CollectionUtils.isEmpty(suggestions)) {
			LOGGER.info("Found " + suggestions.toString());
			return (new Gson().toJson(suggestions));
		}
		return "[]";

	}

	private void processHeaders(HttpServletRequest request, HttpServletResponse response) {

		getB3Headers().keySet().forEach(header -> {
			if (request.getHeader(header) != null) {
				// copy existing header
				response.setHeader(header, request.getHeader(header));
			} else {
				// set missing header
				response.setHeader(header, getB3Headers().get(header));
			}
		});

	}

	// process Zipkin B3 headers for trace propagation
	private Map<String, String> getB3Headers() {
		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("x-client-trace-id", UUID.randomUUID().toString());
		headerMap.put("x-b3-sampled", "1");
		headerMap.put("x-b3-flags", "1");
		headerMap.put("x-b3-traceid", tracer.currentSpan().context().traceIdString());
		headerMap.put("x-b3-spanid", tracer.currentSpan().context().toString().split("/")[1]);
		return headerMap;
	}
}
