package com.coolretailer.ux;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.coolretailer.ux.controller.QueryService;
import com.coolretailer.ux.logic.JsonProcessor;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:test-application.properties")
@ActiveProfiles("JSON")
public class QueryserviceApplicationTests {

	@Autowired
	private JsonProcessor jsonProcessor;

	@Autowired
	private QueryService queryService;

	private static final String inFile = System.getProperty("user.dir") + "/src/test/resources/test.json";
	private static final String outFile = System.getProperty("user.dir") + "/src/test/resources/test.nd.json";

	@Test
	public void testGetSuggestions() throws Exception {
		// check existing
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		assertTrue(new Gson().fromJson(queryService.fetchProducts("va", request, response), List.class).size() == 3);
		response.getHeaderNames().forEach(header -> {
			assertTrue(response.getHeader(header) != null);
		});

	}

	@Test
	public void testController() throws Exception {
		assertTrue(queryService.getBuildInfo() != null);
		assertTrue(queryService.healthCheck() == "OK");
		assertTrue(queryService.getEndpointStatus() == "OK");
	}

	@Test
	public void testJsonProcessor() throws Exception {
		ApplicationArguments args = new ApplicationArguments() {

			@Override
			public String[] getSourceArgs() {
				return null;
			}

			@Override
			public List<String> getOptionValues(String name) {
				List<String> optionValues = new ArrayList<String>();
				optionValues.add(inFile);
				return optionValues;
			}

			@Override
			public Set<String> getOptionNames() {
				Set<String> optionNames = new HashSet<String>();
				optionNames.add("--input.json");
				return optionNames;
			}

			@Override
			public List<String> getNonOptionArgs() {
				return null;
			}

			@Override
			public boolean containsOption(String name) {
				if (name.equals("input.json")) {
					return true;
				}
				return false;
			}
		};
		jsonProcessor.run(args);
		assertTrue(Files.lines(Paths.get(outFile)).count() == 3);
	}

}
