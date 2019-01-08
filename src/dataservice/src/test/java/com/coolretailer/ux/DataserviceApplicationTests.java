package com.coolretailer.ux;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.coolretailer.ux.controller.DataService;
import com.coolretailer.ux.entity.Product;
import com.coolretailer.ux.grpc.system.DataServiceGrpc;
import com.coolretailer.ux.grpc.system.Query;
import com.coolretailer.ux.logic.BQProcessor;

import org.junit.AfterClass;
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
public class DataserviceApplicationTests {

	private static RedisServer redisServer = RedisServer.builder().build();

	@Autowired
	private DataService dataService;

	@Autowired
	private RedisTemplate<String, String> template;

	// marshalling to com.coolretailer.ux.entity.Product
	// not exposed through gRPC calls
	@Autowired
	private BQProcessor bqProcessor;

	private static DataServiceGrpc.DataServiceBlockingStub dataserviceBlockingStub;

	private static final String TEST_CACHE = "SELECT name FROM coolretailer.products LIMIT 5";
	private static final String TEST_EXISTING = "SELECT name FROM coolretailer.products where LOWER(name) like 'ab%' LIMIT 10";
	private static final String TEST_EXISTING_WITH_SKU = "SELECT sku, name FROM coolretailer.products where LOWER(name) like 'ab%' LIMIT 10";
	private static final String TEST_NOT_EXISTING = "SELECT name FROM coolretailer.products where LOWER(name) like 'asdf' LIMIT 10";

	@BeforeClass
	public static void init() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
		dataserviceBlockingStub = DataServiceGrpc.newBlockingStub(managedChannel);
		redisServer.start();
	}

	@AfterClass
	public static void destroy() {
		redisServer.stop();
	}

	@Test
	public void testController() throws Exception {
		assertTrue(dataService.getBuildInfo() != null);
		assertTrue(dataService.healthCheck() == "OK");
		assertTrue(dataService.getEndpointStatus() == "OK");
	}

	@Test
	public void testCacheUpdate() throws Exception {
		// 5 products should be added to cache
		// 0 results from query
		Query query = Query.newBuilder().setQueryString(TEST_CACHE).build();
		assertTrue(dataserviceBlockingStub.updateCache(query).getNameList().size() == 0);
		assertTrue(template.boundZSetOps("autocomplete").zCard() == 5);
	}

	@Test
	public void testExistingFromBQ() throws Exception {
		// This exists.
		Query query = Query.newBuilder().setQueryString(TEST_EXISTING).build();
		assertTrue(dataserviceBlockingStub.runQuery(query).getNameList().size() > 0);

		List<Product> products = bqProcessor.processQuery(TEST_EXISTING_WITH_SKU, Product.class, false);
		assertNotNull(products);
		products.forEach(product -> assertTrue(product.getName() != null && product.getSku() != null));
	}

	@Test
	public void testNonExisting() throws Exception {
		// This doesn't exit.
		Query query = Query.newBuilder().setQueryString(TEST_NOT_EXISTING).build();
		assertTrue(dataserviceBlockingStub.runQuery(query).getNameList().size() == 0);
	}

}
