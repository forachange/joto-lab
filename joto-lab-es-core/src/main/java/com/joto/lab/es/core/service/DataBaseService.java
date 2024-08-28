package com.joto.lab.es.core.service;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.joto.lab.es.core.dto.EsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author joey
 * @description 数据保存抽象类
 * @date Created in 2022/5/19 15:21
 * @modified by
 */
@Slf4j
@Service
public class DataBaseService {

    public static final String TYPE_CREATE = "create";
    public static final String TYPE_UPDATE = "update";
    public static final String TYPE_INDEX = "index";
    public static final String TYPE_DELETE = "delete";
    public static final int BULK_SIZE = 500;
    protected ElasticsearchClient elasticsearchClient;

    @Resource
    public void setElasticsearchClient(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * batch save
     *
     * @param esDataList es data list
     * @return error count
     */
    public int batchSave2es(List<EsData> esDataList) {
        return batchSave2es(esDataList, BULK_SIZE, TYPE_INDEX);
    }

    public int batchSave2es(List<EsData> esDataList, int bulkSize, String type) {
        log.info("{}", this.getClass().getSimpleName());

        int error = 0;

        if (esDataList == null || esDataList.isEmpty()) {
            log.warn("{} data list is empty", this.getClass().getSimpleName());
            return error;
        }

        List<BulkOperation> bulkOperationList = new ArrayList<>(bulkSize + 1);

        for (int i = 0; i < esDataList.size(); i++) {
            final EsData data = esDataList.get(i);

            BulkOperation bulkOperation = createOperation(data, type);

            bulkOperationList.add(bulkOperation);
            if ((i + 1) % bulkSize == 0) {
                error += bulkReq(bulkOperationList);
                bulkOperationList.clear();
            }
        }

        if (!bulkOperationList.isEmpty()) {
            error += bulkReq(bulkOperationList);
            bulkOperationList.clear();
        }
        return error;
    }

    public BulkOperation createOperation(EsData esData) {
        return createOperation(esData, TYPE_INDEX);
    }

    public BulkOperation createOperation(EsData esData, String type) {

        switch (type) {
            case TYPE_CREATE:
                return BulkOperation.of(o -> o
                        .create(a -> {
                            a.index(esData.getIndexName()).document(esData.getBinaryData());
                            if (StrUtil.isNotBlank(esData.getId())) {
                                a.id(esData.getId());
                            }
                            return a;
                        })
                );
            case TYPE_UPDATE:
                return BulkOperation.of(o -> o
                        .update(a -> a
                                .index(esData.getIndexName())
                                .id(esData.getId())
                                .action(b -> b.doc(esData.getBinaryData()))
                        )

                );
            case TYPE_DELETE:
                return BulkOperation.of(o -> o
                        .delete(a -> a
                                .index(esData.getIndexName())
                                .id(esData.getId())
                        )

                );
            default:
                return BulkOperation.of(o -> o
                        .index(a -> {
                            a.index(esData.getIndexName()).document(esData.getBinaryData());
                            if (StrUtil.isNotBlank(esData.getId())) {
                                a.id(esData.getId());
                            }
                            return a;
                        })
                );
        }
    }

    public int bulkReq(List<BulkOperation> bulkOperationList) {
        BulkRequest bulkRequest = BulkRequest.of(a -> a
                .operations(bulkOperationList)
        );
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        try {
            BulkResponse bulk = elasticsearchClient.bulk(bulkRequest);
            log.info("{} is error: {}, size: {}", this.getClass().getSimpleName(),
                    bulk.errors(), bulk.items().size());
            if (bulk.errors()) {
                bulk.items().forEach(i -> {
                    if (i.error() != null) {
                        atomicInteger.incrementAndGet();
                        log.error(i.error().reason());
                    }
                });
            }
        } catch (IOException e) {
            log.error("bulk exception:", e);
        }
        return atomicInteger.get();
    }
}
