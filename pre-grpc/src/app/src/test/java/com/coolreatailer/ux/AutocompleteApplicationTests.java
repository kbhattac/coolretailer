package com.coolreatailer.ux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.coolreatailer.ux.controller.QueryService;
import com.coolreatailer.ux.logic.CacheProcessor;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import redis.embedded.RedisServer;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:test-application.properties")
public class AutocompleteApplicationTests {

	@Autowired
	private QueryService queryService;

	@Autowired
	private CacheProcessor cacheProcessor;

	private static RedisServer redisServer = RedisServer.builder().build();
	private static MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
	private static MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();

	@BeforeClass
	public static void init() {
		mockHttpServletRequest.addHeader("x-request-id", "12345");
		redisServer.start();
	}

	@AfterClass
	public static void destroy() {
		redisServer.stop();
	}

	@Test
	public void testInit() throws Exception {
		assertNotNull(cacheProcessor.processCache("5"));
	}

	@Test
	public void testExistingFromBQ() throws Exception {
		// This exists.
		assertNotNull(queryService.fetchProducts("ab", mockHttpServletRequest, mockHttpServletResponse));
	}

	@Test
	public void testNonExisting() throws Exception {
		// This doesn't exit.
		assertEquals("[]", queryService.fetchProducts("asdf", mockHttpServletRequest, mockHttpServletResponse));
		// Now it should exist
		assertNotNull(queryService.fetchProducts("asdf", mockHttpServletRequest, mockHttpServletResponse));
	}

}
