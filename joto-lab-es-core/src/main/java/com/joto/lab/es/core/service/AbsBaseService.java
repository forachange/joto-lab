package com.joto.lab.es.core.service;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.util.BinaryData;
import co.elastic.clients.util.ObjectBuilder;
import com.joto.lab.es.core.annotations.EsField;
import com.joto.lab.es.core.dto.*;
import com.joto.lab.es.core.utils.EsUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author joey
 * @date 2024/8/21 15:39
 */
public abstract class AbsBaseService<E extends EsId> {

    protected ElasticsearchClient elasticsearchClient;

    protected DataBaseService dataBaseService;

    @Autowired
    public void setElasticsearchClient(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Autowired
    public void setDataBaseService(DataBaseService dataBaseService) {
        this.dataBaseService = dataBaseService;
    }

    /**
     * 保存单个实体数据
     *
     * @param indexName 索引名称
     * @param id        数据ID，如果不指定，就为空
     * @param entity    数据实体
     * @return 保存结果
     * @throws IOException io
     */
    public Result save(String indexName, String id, E entity) throws IOException {
        IndexRequest<E> request = IndexRequest.of(req -> {
            if (StrUtil.isNotEmpty(id)) {
                req.id(id);
            }
            return req.index(indexName)
                    .opType(OpType.Index)
                    .document(entity);
        });
        final IndexResponse response = elasticsearchClient.index(request);
        return Objects.requireNonNull(response).result();
    }

    /**
     * 批量保存
     *
     * @param indexName 索引名
     * @param entity    数据
     * @return 成功保存的数量
     */
    public Integer bulk(String indexName, List<E> entity) {
        final List<EsData> esDataList = generateEsData(indexName, entity);
        return entity.size() - dataBaseService.batchSave2es(esDataList);
    }

    /**
     * 根据 ID 获取数据
     *
     * @param indexName 索引名
     * @param id        ID
     * @param clazz     数据类型
     * @return 数据
     * @throws IOException id
     */
    public E getById(String indexName, String id, Class<E> clazz) throws IOException {
        GetRequest getRequest = GetRequest.of(req -> req.index(indexName).id(id));
        final GetResponse<E> response = elasticsearchClient.get(getRequest, clazz);
        return Objects.requireNonNull(response).source();
    }

    /**
     * 分页查询，如果数量超过 10000，使用 pit 进行查询
     *
     * @param indexName 索引名称
     * @param paging    查询条件
     * @return 数据集合
     * @throws IOException io
     */
    public <T extends Paging> PagingDto<E> paging(String indexName, T paging, Class<E> clazz) throws IOException {

        PagingDto<E> pagingDto = new PagingDto<>();

        List<Query> queries = generateQuery(paging);

        Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> queryFn = a -> a
                .index(indexName)
                .query(q -> q
                        .bool(b -> b.filter(queries))
                )
                .size(paging.getSize())
                .from(paging.getIndex() * paging.getSize());

        EsUtil.printRequest(queryFn);

        final SearchResponse<E> response = elasticsearchClient.search(queryFn, clazz);

        if (Objects.requireNonNull(response.hits().total()).value() == 0 || response.hits().hits().isEmpty()) {
            return pagingDto;
        }
        final long value = response.hits().total().value();

        List<E> list = new ArrayList<>(Long.valueOf(value).intValue());

        response.hits().hits().forEach(d -> list.add(d.source()));

        pagingDto.setTotal(value);
        pagingDto.setIndex(pagingDto.getIndex());
        pagingDto.setList(list);

        return pagingDto;
    }

    public <T> Long count(String indexName, T pit) throws IOException {
        List<Query> queries = generateQuery(pit);

        Function<CountRequest.Builder, ObjectBuilder<CountRequest>> queryFn = a -> a
                .index(indexName)
                .query(q -> q
                        .bool(b -> b.filter(queries))
                );

        final CountResponse response = elasticsearchClient.count(queryFn);

        return response.count();
    }


    public <T extends Pit> PitDto<E> pit(String indexName, T pit, Class<E> clazz) throws IOException {

        final Long count = count(indexName, pit);
        if (count <= 0) {
            return new PitDto<>();
        }

        List<Query> queries = generateQuery(pit);

        final String pointInTime = StrUtil.isBlank(pit.getPit()) ? EsUtil.createPit(elasticsearchClient, indexName) : pit.getPit();

        // 构建排序规则
        List<SortOptions> sortOptions = new ArrayList<>();
        sortOptions.add(SortOptions.of(a -> a.field(FieldSort.of(b -> b.field("createDatetime").order(SortOrder.Asc)))));

        PitDto<E> pitDto = EsUtil.queryPit(elasticsearchClient, pointInTime,
                pit.getSorts(), queries, sortOptions, clazz);

        pitDto.setTotal(count);

        return pitDto;
    }

    /**
     * 根据 id 删除
     *
     * @param indexName 索引名称
     * @param id        主键 ID
     * @return 删除结果
     * @throws IOException io
     */
    public Result delete(String indexName, String id) throws IOException {
        DeleteRequest request = DeleteRequest.of(req ->
                req.index(indexName).id(id));
        final DeleteResponse response = elasticsearchClient.delete(request);
        return Objects.requireNonNull(response).result();
    }

    private <T> List<Query> generateQuery(T e) {
        List<Query> queries = new ArrayList<>();
        List<Query> shouldQueries = new ArrayList<>();

        final Map<String, Field> fieldMap = ReflectUtil.getFieldMap(e.getClass());


        fieldMap.forEach((key, value) -> {
            final EsField annotation = value.getAnnotation(EsField.class);

            if (annotation == null) {
                return;
            }

            final Object fieldValue = ReflectUtil.getFieldValue(e, value);

            if (Objects.isNull(fieldValue)) {
                return;
            }

            switch (annotation.fieldType()) {
                case LONG:
                    EsUtil.addTermQuery(queries, annotation.fieldName(), (Long) fieldValue);
                    break;
                case DOUBLE:
                    EsUtil.addTermQuery(queries, annotation.fieldName(), (Double) fieldValue);
                    break;
                case SHORT:
                case INTEGER:
                    EsUtil.addTermQuery(queries, annotation.fieldName(), (Integer) fieldValue);
                    break;
                case BOOLEAN:
                    EsUtil.addTermQuery(queries, annotation.fieldName(), (Boolean) fieldValue);
                    break;
                case TEXT:
                    // 单独处理
                    EsUtil.addMatchQuery(shouldQueries, annotation.fieldName(), fieldValue.toString());
                    break;
                default:
                    EsUtil.addTermQuery(queries, annotation.fieldName(), fieldValue.toString());
            }
        });

        queries.add(Query.of(q -> q.bool(builder -> builder.should(shouldQueries))));

        return queries;
    }

    /**
     * 将对象封装成 ES 对象
     *
     * @param indexName 索引名称
     * @param dataList  数据集合
     * @return EsData 集合
     */
    private List<EsData> generateEsData(final String indexName, final List<E> dataList) {
        final List<EsData> esDataList = new ArrayList<>(dataList.size() + 1);

        dataList.forEach(d -> {
            EsData esData = new EsData();

            // 设置主键
            esData.setId(d.generateId());
            // 设置索引
            esData.setIndexName(indexName);

            // 设置数据，直接使用 BinaryData, 减少一次数据转换
            esData.setBinaryData(BinaryData.of(d, EsUtil.getMapper()));

            esDataList.add(esData);
        });

        return esDataList;
    }
}
