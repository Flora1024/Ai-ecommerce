package com.quanxiaoha.vector.store.service;

import com.quanxiaoha.vector.store.model.SearchResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author: 犬小哈
 * @Date: 2025/7/30 12:13
 * @Version: v1.0.0
 * @Description: 页面内容提取
 **/
public interface SearchResultContentFetcherService {


    /**
     * 并发批量获取搜索结果页面的内容
     *
     * @param searchResults
     * @param timeout
     * @param unit
     * @return
     */
    CompletableFuture<List<SearchResult>> batchFetch(List<SearchResult> searchResults,
                                                     long timeout,
                                                     TimeUnit unit);
}

