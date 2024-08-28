package com.joto.lab.es.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.cat.nodes.NodesRecord;
import co.elastic.clients.elasticsearch.cat.segments.SegmentsRecord;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeRequest;
import co.elastic.clients.elasticsearch.indices.ForcemergeResponse;
import co.elastic.clients.elasticsearch.indices.IndicesStatsResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.json.jsonb.JsonbJsonpMapper;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.joto.lab.es.core.config.ElasticsearchConfig;
import com.joto.lab.es.core.dto.PitDto;
import com.joto.lab.es.core.dto.SearchAfterEntity;
import com.joto.lab.es.core.dto.SortFiledDto;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author joey
 * @description
 * @date 2022/7/20 10:11
 */
@Slf4j
public final class EsUtil {


    /**
     * 每次获取数据条数
     */
    public static final int FETCH_SIZE = 5000;

    /**
     * 批量操作，每次操作数量
     */
    public static final int BATCH_SIZE = 5000;
    /**
     * pit 过期时长
     */
    private static final String PIT_KEEP_ALIVE = "1m";

    private EsUtil() {
    }

    public static final JacksonJsonpMapper JACKSON_JSONP_MAPPER = new JacksonJsonpMapper(EsBaseUtil.OBJECT_MAPPER);
    public static JacksonJsonpMapper getMapper() {
        return JACKSON_JSONP_MAPPER;
    }

    public static void printRequest(Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> queryFn) {
        SearchRequest searchRequest = queryFn.apply(new SearchRequest.Builder()).build();
        JsonbJsonpMapper mapper = new JsonbJsonpMapper();
        String str = toJson(searchRequest, mapper);
        log.debug("searchRequest: {}", str);
    }

    public static <T> String toJson(T value, JsonpMapper mapper) {
        StringWriter sw = new StringWriter();
        JsonProvider jsonProvider = mapper.jsonProvider();
        JsonGenerator generator = jsonProvider.createGenerator(sw);
        mapper.serialize(value, generator);
        generator.close();
        return sw.toString();
    }

    public static RangeQuery generateDateTimeRangeQuery(LocalDate startDatetime, LocalDate endDatetime) {
        return generateDateTimeRangeQuery("dataTime", startDatetime, endDatetime);
    }

    /**
     * 时间范围过滤
     *
     * @param fieldName
     * @param startDatetime
     * @param endDatetime
     * @return
     */
    public static RangeQuery generateDateTimeRangeQuery(String fieldName, LocalDate startDatetime, LocalDate endDatetime) {
        return RangeQuery.of(r -> r
                .field(fieldName)
                .gte(JsonData.of(DateUtil.toEpochMilli(startDatetime)))
                .lt(JsonData.of(DateUtil.toEpochMilli(endDatetime)))
        );
    }


    /**
     * 根据过滤条件，返回所有满足条件的数据。使用 pit 查询
     *
     * @param elasticsearchClient es client
     * @param indexName           索引名
     * @param queries             查询条件
     * @param clazz               返回数据类型
     * @param <T>                 数据类型
     * @return
     * @throws IOException
     */
    public static <T> CompletableFuture<List<T>> getListCompletableFuture(final ElasticsearchClient elasticsearchClient,
                                                                          final String indexName,
                                                                          final List<Query> queries, Class<T> clazz) throws IOException {
        String pitId = EsUtil.createPit(elasticsearchClient, indexName);

        List<T> retList = CollUtil.newArrayList();

        List<SortOptions> sortOptions = generateSortOptionsStationDataTime();
        // 查询数据
        SearchAfterEntity<T> searchAfterEntity = EsUtil.handlerSearchAfterResponse(elasticsearchClient, pitId, queries, sortOptions, null, clazz);

        while (searchAfterEntity.isResult()) {

            // 返回的数据
            retList.addAll(searchAfterEntity.getDocList());

            if (searchAfterEntity.getDocList().size() < FETCH_SIZE) {
                break;
            }

            searchAfterEntity = EsUtil.handlerSearchAfterResponse(elasticsearchClient, pitId, queries,
                    sortOptions, searchAfterEntity.getSorts(), clazz);
        }

        // 关闭 pit
        closePit(elasticsearchClient, pitId);

        return CompletableFuture.completedFuture(retList);
    }

    /**
     * 创建 pit
     *
     * @param elasticsearchClient client
     * @param indexName           索引名
     * @return
     * @throws IOException
     */
    public static String createPit(final ElasticsearchClient elasticsearchClient, final String indexName) throws IOException {

        OpenPointInTimeRequest ooitRequest = OpenPointInTimeRequest.of(a -> a
                .index(indexName)
                .keepAlive(b -> b
                        .time(PIT_KEEP_ALIVE)
                )
        );

        // 开启一个 pit 查询
        OpenPointInTimeResponse openPointInTimeResponse = elasticsearchClient.openPointInTime(ooitRequest);

        log.debug("pit:{}", openPointInTimeResponse.id());

        return openPointInTimeResponse.id();
    }

    /**
     * 关闭 pit
     * @param elasticsearchClient client
     * @param pitId pit
     * @return
     * @throws IOException
     */
    public static boolean closePit(final ElasticsearchClient elasticsearchClient, final String pitId) throws IOException {
        ClosePointInTimeRequest closePointInTimeRequest = ClosePointInTimeRequest.of(a -> a.id(pitId));

        ClosePointInTimeResponse closePointInTimeResponse = elasticsearchClient.closePointInTime(closePointInTimeRequest);

        if (log.isDebugEnabled()) {
            log.debug("closePointInTimeResponse: {}", closePointInTimeResponse.succeeded());
        }

        return closePointInTimeResponse.succeeded();
    }

    /**
     * 排序条件，电站id及时间
     *
     * @return
     */
    public static List<SortOptions> generateSortOptionsStationDataTime() {
        List<SortOptions> sortOptions = new ArrayList<>();
        sortOptions.add(SortOptions.of(a -> a.field(FieldSort.of(b -> b.field("stationId").order(SortOrder.Asc)))));
        sortOptions.add(SortOptions.of(a -> a.field(FieldSort.of(b -> b.field("dataTime").order(SortOrder.Asc)))));
        return sortOptions;
    }

    /**
     * 构建 pit 查询
     *
     * @param pitId
     * @param queries
     * @param sortOptions
     * @param searchAfterList
     * @param pitKeepAlive
     * @param fetchSize
     * @return
     */
    public static Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> generateBuilderObjectBuilderFunction(
            String pitId, List<Query> queries, List<SortOptions> sortOptions, List<FieldValue> searchAfterList,
            String pitKeepAlive, int fetchSize) {
        if (searchAfterList == null) {
            return a -> a
                    .query(b -> b
                            .bool(c -> c
                                    .filter(queries)
                            )
                    )
                    .pit(b -> b
                            .id(pitId)
                            .keepAlive(c -> c.time(pitKeepAlive))
                    )
                    .sort(sortOptions)
                    .size(fetchSize);
        }
        return a -> a
                .query(b -> b
                        .bool(c -> c
                                .filter(queries)
                        )
                )
                .pit(b -> b
                        .id(pitId)
                        .keepAlive(c -> c.time(pitKeepAlive))
                )
                .sort(sortOptions)
                .searchAfter(searchAfterList)
                .size(fetchSize);
    }

    /**
     * 处理 pit 查询的返回结果
     *
     * @param elasticsearchClient
     * @param pitId               pitId
     * @param queries             查询条件
     * @param sortOptions         排序
     * @param searchAfterList
     * @param clazz
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> SearchAfterEntity<T> handlerSearchAfterResponse(final ElasticsearchClient elasticsearchClient,
                                                                      final String pitId, final List<Query> queries,
                                                                      final List<SortOptions> sortOptions,
                                                                      final List<FieldValue> searchAfterList, Class<T> clazz) throws IOException {

        Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> queryFn =
                EsUtil.generateBuilderObjectBuilderFunction(pitId, queries, sortOptions, searchAfterList, PIT_KEEP_ALIVE, FETCH_SIZE);

        if (log.isDebugEnabled()) {
            EsUtil.printRequest(queryFn);
        }

        SearchResponse<T> response = elasticsearchClient.search(queryFn, clazz);

        SearchAfterEntity<T> searchAfterEntity = new SearchAfterEntity<>();

        if (response.hits().total() == null || response.hits().total().value() == 0
                || CollectionUtil.isEmpty(response.hits().hits())) {
            // 没有数据
            searchAfterEntity.setDocList(new ArrayList<>(0));
            return searchAfterEntity;
        }

        if (log.isDebugEnabled()) {
            log.debug("total => {}, current => {}", response.hits().total().value(), response.hits().hits().size());
        }

        List<T> retList = new ArrayList<>(response.hits().hits().size() + 1);

        response.hits().hits().forEach(h -> retList.add(h.source()));

        searchAfterEntity.setPit(response.pitId());
        searchAfterEntity.setResult(true);
        searchAfterEntity.setDocList(retList);
        searchAfterEntity.setSorts(response.hits().hits().get(response.hits().hits().size() - 1).sort());

        return searchAfterEntity;
    }

    /**
     * 通过 pit 查询数据
     *
     * @param elasticsearchClient es
     * @param pit                 pit
     * @param sorts               上一次查询最后一条数据的排序值
     * @param queries             查询条件
     * @param sortOptions         排序
     * @param clazz               返回数据类型
     * @param <E>                 返回数据类型
     * @return
     * @throws IOException
     */
    public static <E> PitDto<E> queryPit(final ElasticsearchClient elasticsearchClient, final String pit,
                                         final List<SortFiledDto> sorts, final List<Query> queries,
                                         final List<SortOptions> sortOptions, final Class<E> clazz) throws IOException {
        PitDto<E> pitDto = new PitDto<>();
        SearchAfterEntity<E> searchAfterEntity;
        if (sorts == null) {
            searchAfterEntity = EsUtil.handlerSearchAfterResponse(elasticsearchClient, pit, queries,
                    sortOptions, null, clazz);
        } else {
            List<FieldValue> fieldValues = CollUtil.newArrayList();
            sorts.forEach(s -> fieldValues.add(s.convert()));

            searchAfterEntity = EsUtil.handlerSearchAfterResponse(elasticsearchClient, pit, queries,
                    sortOptions, fieldValues, clazz);
        }
        boolean hasMore = searchAfterEntity.getDocList().size() >= EsUtil.FETCH_SIZE;

        pitDto.setList(searchAfterEntity.getDocList());
        pitDto.setMore(hasMore);
        if (hasMore) {
            pitDto.setPit(searchAfterEntity.getPit());
            final List<SortFiledDto> retSorts = CollUtil.newArrayList();
            searchAfterEntity.getSorts().forEach(s -> {
                if (log.isDebugEnabled()) {
                    log.debug("sorts: {} - {}", s._kind(), s._get());
                }
                retSorts.add(SortFiledDto.builder().kind(s._kind()).value(s._get()).build());
            });

            pitDto.setSorts(retSorts);
        } else {
            // 关闭 pit
            ClosePointInTimeRequest closePointInTimeRequest = ClosePointInTimeRequest.of(a -> a.id(pit));

            ClosePointInTimeResponse closePointInTimeResponse = elasticsearchClient.closePointInTime(closePointInTimeRequest);

            if (log.isDebugEnabled()) {
                log.debug("closePointInTimeResponse: {}", closePointInTimeResponse.succeeded());
            }
        }
        return pitDto;
    }

    /**
     * 根据查询时间范围，确定具体索引名称。如果时间范围是同一个月，则直接查询指定月的索引
     *
     * @param elasticsearchClient
     * @param indexName
     * @param startDatetime
     * @param endDatetime
     * @return
     * @throws IOException
     */
    public static String getMonthIndexName(final ElasticsearchClient elasticsearchClient, final String indexName,
                                           final LocalDate startDatetime, final LocalDate endDatetime)
            throws IOException {
        LocalDate endDate = endDatetime.minusDays(1);
        if (DateUtil.isSameYearMonth(startDatetime, endDate)) {
            String tmp = String.format("%s-%s%s", indexName, startDatetime.getYear(),
                    StrUtil.padPre(startDatetime.getMonthValue() + "", 2, "0"));
            checkIndex(elasticsearchClient, tmp);
            return tmp;
        }

        return indexName;
    }

    /**
     * 根据查询时间范围，确定具体索引名称。如果时间范围是同一个年，则直接查询指定年的索引
     *
     * @param elasticsearchClient
     * @param startDatetime
     * @param endDatetime
     * @param indexName
     * @return
     * @throws IOException
     */
    public static String getYearIndexName(final ElasticsearchClient elasticsearchClient, final String indexName,
                                          final LocalDate startDatetime,
                                          final LocalDate endDatetime)
            throws IOException {
        LocalDate endDate = endDatetime.minusDays(1);
        if (startDatetime.getYear() == endDate.getYear()) {
            String tmp = String.format("%s-%s", indexName, startDatetime.getYear());
            checkIndex(elasticsearchClient, tmp);
            return tmp;
        }

        return indexName;
    }

    /**
     * 生成指定年的索引，并校验索引是否存在
     *
     * @param elasticsearchClient
     * @param indexName
     * @param localDate
     * @return
     * @throws IOException
     */
    public static String getYearIndexName(final ElasticsearchClient elasticsearchClient, final String indexName,
                                          final LocalDate localDate)
            throws IOException {
        String tmp = String.format("%s-%s", indexName, localDate.getYear());
        checkIndex(elasticsearchClient, tmp);
        return tmp;
    }

    public static void checkIndex(final ElasticsearchClient elasticsearchClient, String indexName) throws IOException {
        ExistsRequest existsRequest = ExistsRequest.of(a -> a.index(indexName));

        if (!elasticsearchClient.indices().exists(existsRequest).value()) {
            throw new ElasticsearchException("exist", ErrorResponse.of(
                    a -> a.error(
                            b -> b.type("index_not_found_exception")
                                    .reason("no such index [" + indexName + "]"))
                            .status(400)
            ));
        }
    }

    /**
     * 添加 String term query
     * @param queries all queries
     * @param field term field
     * @param termValue term value
     */
    public static void addTermQuery(List<Query> queries, final String field, String termValue) {
        if (StrUtil.isNotBlank(termValue)) {
            TermQuery termQuery = TermQuery.of(t -> t
                    .field(field)
                    .value(termValue)
            );
            queries.add(Query.of(q -> q.term(termQuery)));
        }
    }

    /**
     * 添加 Long term query
     * @param queries all queries
     * @param field term field
     * @param termValue term value
     */
    public static void addTermQuery(List<Query> queries, final String field, Long termValue) {
        if (Objects.nonNull(termValue)) {
            TermQuery termQuery = TermQuery.of(t -> t
                    .field(field)
                    .value(termValue)
            );
            queries.add(Query.of(q -> q.term(termQuery)));
        }
    }

    /**
     * 添加 Integer term query
     * @param queries all queries
     * @param field term field
     * @param termValue term value
     */
    public static void addTermQuery(List<Query> queries, final String field, Integer termValue) {
        if (Objects.nonNull(termValue)) {
            TermQuery termQuery = TermQuery.of(t -> t
                    .field(field)
                    .value(termValue)
            );
            queries.add(Query.of(q -> q.term(termQuery)));
        }
    }

    /**
     * 添加 Double term query
     * @param queries all queries
     * @param field term field
     * @param termValue term value
     */
    public static void addTermQuery(List<Query> queries, final String field, Double termValue) {
        if (Objects.nonNull(termValue)) {
            TermQuery termQuery = TermQuery.of(t -> t
                    .field(field)
                    .value(termValue)
            );
            queries.add(Query.of(q -> q.term(termQuery)));
        }
    }

    /**
     * 添加 String match query
     * @param queries should queries
     * @param field field
     * @param text text value
     */
    public static void addMatchQuery(List<Query> queries, final String field, String text) {
        if (Objects.nonNull(text)) {
            MatchQuery matchQuery = MatchQuery.of(t -> t
                    .field(field)
                    .query(text)
            );
            queries.add(Query.of(q -> q.match(matchQuery)));
        }
    }


    /**
     * 添加 term query
     * @param queries all queries
     * @param field term field
     * @param termValue term value
     */
    public static void addTermQuery(List<Query> queries, final String field, Boolean termValue) {
        if (Objects.nonNull(termValue)) {
            TermQuery termQuery = TermQuery.of(t -> t
                    .field(field)
                    .value(termValue)
            );
            queries.add(Query.of(q -> q.term(termQuery)));
        }
    }

    /**
     * 打印所有索引信息
     *
     * @param elasticsearchClient
     * @throws IOException
     */
    public static void printIndices(final ElasticsearchClient elasticsearchClient) throws IOException {
        final List<IndicesRecord> indicesRecords = elasticsearchClient.cat().indices().valueBody();
        log.info("IndicesInfo Size: {}", indicesRecords.size());

        final List<List<IndicesRecord>> partition = CollUtil.split(indicesRecords, 100);
        partition.forEach(p -> {
            try {
                log.info("IndicesInfo: {}", JsonUtil.toJsonStr(p));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * 打印所有段信息
     *
     * @param elasticsearchClient
     * @throws IOException
     */
    public static void printSegments(final ElasticsearchClient elasticsearchClient) throws IOException {
        final List<SegmentsRecord> segmentsRecords = elasticsearchClient.cat().segments().valueBody();
        log.info("SegmentsInfo Size: {}", segmentsRecords.size());

        final List<List<SegmentsRecord>> partition = CollUtil.split(segmentsRecords, 100);
        partition.forEach(p -> {
            try {
                log.info("SegmentsInfo: {}", JsonUtil.toJsonStr(p));
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        });
    }

    /**
     * 打印集群健康信息
     *
     * @param elasticsearchClient
     * @throws IOException
     */
    public static void printHealth(final ElasticsearchClient elasticsearchClient) throws IOException {
        elasticsearchClient.cat().health().valueBody().forEach(h -> log.info("HealthInfo: cluster name: {}, status: {}, shards: {}", h.cluster(), h.status(), h.shards()));
    }

    /**
     * 打印集群节点信息
     *
     * @param elasticsearchClient
     * @throws IOException
     */
    public static void printNodes(final ElasticsearchClient elasticsearchClient) throws IOException {
        final List<NodesRecord> nodesRecords = elasticsearchClient.cat().nodes().valueBody();
        log.info("NodesInfo: {}", JsonUtil.toJsonStr(nodesRecords));
    }

    /**
     * 打印集群状态信息
     *
     * @throws IOException
     */
    public static void printClusterStats() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        String statsResponse = http("", "/_cluster/stats");

        log.info("ClusterStatsInfo: {}", statsResponse);
    }

    /**
     * @param indexName index name
     * @param path      path
     * @return
     * @throws Exception
     */
    public static String http(final String indexName, final String path)
            throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        // 分割成地址数组
        String[] hostArr = ElasticsearchConfig.getHosts().split(ElasticsearchConfig.COMMA);
        HttpHost[] httpHosts = new HttpHost[hostArr.length];
        int i = 0;
        /* 添加 */
        for (String host : hostArr) {
            String[] hostPort = host.split(ElasticsearchConfig.COLON);
            httpHosts[i++] = new HttpHost(hostPort[0], Integer.parseInt(hostPort[1]), ElasticsearchConfig.getScheme());
        }

        String oriUser = EncryptUtil.decrypt4(ElasticsearchConfig.getUsername(), ElasticsearchConfig.getSecret(), ElasticsearchConfig.getIv());
        String oriPwd = EncryptUtil.decrypt4(ElasticsearchConfig.getPassword(), ElasticsearchConfig.getSecret(), ElasticsearchConfig.getIv());

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(oriUser, oriPwd));

        Certificate trustedCa;
        CertificateFactory factory =
                CertificateFactory.getInstance("X.509");

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final org.springframework.core.io.Resource[] resources = resolver.getResources(ElasticsearchConfig.getCaFile());
        final org.springframework.core.io.Resource resource = resources[0];
        try (InputStream is = resource.getInputStream()) {
            trustedCa = factory.generateCertificate(is);
        }

        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", trustedCa);

        SSLContextBuilder sslBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null);

        final SSLContext sslContext = sslBuilder.build();

        String urlTpl = "{}://{}:{}{}";
        String url = StrUtil.format(urlTpl, ElasticsearchConfig.getScheme(),
                httpHosts[0].getHostName(), httpHosts[0].getPort(), indexName + path);

        final HttpRequest httpRequest = HttpRequest.get(url)
                .setSSLSocketFactory(sslContext.getSocketFactory())
                .basicAuth(oriUser, oriPwd);

        final HttpResponse httpResponse = httpRequest.execute();

        return httpResponse.body();
    }

    /**
     * force merge
     *
     * @param elasticsearchClient es client
     * @param indexName           index name
     * @param onlyExpungeDeletes  10%
     * @param maxNumSegments      fully merge indices, set it to 1
     * @throws IOException
     */
    public static void forceMerge(final ElasticsearchClient elasticsearchClient,
                                  final String indexName,
                                  final boolean onlyExpungeDeletes,
                                  final long maxNumSegments) throws IOException {

        final ForcemergeRequest.Builder builder = new ForcemergeRequest.Builder()
                .index(indexName)
                .waitForCompletion(false);

        builder.maxNumSegments(maxNumSegments);

        if (onlyExpungeDeletes) {
            builder.onlyExpungeDeletes(true);
        }

        final ForcemergeResponse forcemergeResponse = elasticsearchClient.indices().forcemerge(builder.build());
        log.info("forceMerge index:{}, only-expunge-deletes:{}, max-num-segments:{}, response:{}",
                indexName, onlyExpungeDeletes, maxNumSegments,
                forcemergeResponse.toString());
    }

    public static int countSegments(String indexName) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        final String response = http(indexName, "/_stats/segments");

        final IndicesStatsResponse indicesStatsResponse = JSONUtil.toBean(response, IndicesStatsResponse.class);

        final SegmentsStats segments = Objects.requireNonNull(indicesStatsResponse.indices().get(indexName).primaries()).segments();

        return segments != null ? segments.count() : 0;
    }

    public static int getValue(Double dvl) {
        if (dvl == null) {
            return 0;
        }
        return dvl.intValue();
    }
}
