package com.coolreatailer.ux.logic;

import java.util.List;

public interface QueryProcessor {

	public <T> List<T> runQuery(String queryString, Class<T> t, boolean updateCache) throws Exception;
}
