package com.coolretailer.ux;

import static org.junit.Assert.assertTrue;

import com.coolretailer.ux.controller.CacheService;
import com.coolretailer.ux.grpc.system.CacheServiceGrpc;
import com.coolretailer.ux.grpc.system.CacheServiceGrpc.CacheServiceBlockingStub;
import com.coolretailer.ux.grpc.system.Empty;
import com.coolretailer.ux.grpc.system.Limit;
import com.coolretailer.ux.grpc.system.Query;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import redis.embedded.RedisServer;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:test-application.properties")
public class CacheserviceApplicationTests {

	private static final RedisServer redisServer = RedisServer.builder().build();
	private CacheServiceBlockingStub cacheServiceBlockingStub;

	@Autowired
	private CacheService cacheService;

	@Autowired
	private RedisTemplate<String, String> template;

	@BeforeClass
	public static void initStore() {
		redisServer.start();
	}

	@Test
	public void testController() throws Exception {
		assertTrue(cacheService.getBuildInfo() != null);
		assertTrue(cacheService.healthCheck() == "OK");
		assertTrue(cacheService.getEndpointStatus() == "OK");
	}

	@Before
	public void init() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("127.0.0.1", 6566).usePlaintext().build();
		cacheServiceBlockingStub = CacheServiceGrpc.newBlockingStub(managedChannel);
	}

	@Test
	public void testExistingFromDB() {
		// check existing
		Query prefix1 = Query.newBuilder().setQueryString("va").build();
		assertTrue(cacheServiceBlockingStub.getSuggestions(prefix1).getNameList().size() == 3);
	}

	@Test
	public void testClearCache() {
		// clear cache
		cacheServiceBlockingStub.clearCache(Empty.newBuilder().build());
		assertTrue(template.boundZSetOps("autocomplete").zCard() == 0);
	}

	@Test
	public void testProcessCache() {
		// load items to cache
		Limit limit = Limit.newBuilder().setLimit(3).build();
		cacheServiceBlockingStub.processCache(limit);
		assertTrue(template.boundZSetOps("autocomplete").zCard() == 3);
		// Fetch from cache
		assertTrue(cacheServiceBlockingStub.getSuggestions(Query.newBuilder().setQueryString("ab").build()).getName(0)
				.equals("name1"));

	}

	@Test
	public void testNonExisting() {
		// check non-existing
		Query prefix2 = Query.newBuilder().setQueryString("sdf").build();
		cacheServiceBlockingStub.getSuggestions(prefix2);
		// It should now be added to missing list
		assertTrue(template.boundZSetOps("missing").rank("sdf") >= 0);
	}

	@AfterClass
	public static void destroy() {
		redisServer.stop();
	}
}
