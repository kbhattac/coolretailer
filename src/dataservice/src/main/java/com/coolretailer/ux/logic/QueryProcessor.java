package com.coolretailer.ux.logic;

import java.util.List;

public interface QueryProcessor {

	public <T> List<T> processQuery(String queryString, Class<T> t, boolean updateCache) throws Exception;
}
