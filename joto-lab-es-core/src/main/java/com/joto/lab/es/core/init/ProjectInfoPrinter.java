package com.joto.lab.es.core.init;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.joto.lab.es.core.utils.EsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/**
 * @author joey
 * @description 打印项目信息
 * @date 2022/10/14 10:24
 */
@Component
@Slf4j
public class ProjectInfoPrinter implements CommandLineRunner {

    @Value("${print.project.enable:true}")
    private boolean printProjectInfo;

    @Value("${print.es.enable:false}")
    private boolean printEsInfo;


    private BuildProperties buildProperties;

    private ElasticsearchClient elasticsearchClient;

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Autowired
    public void setElasticsearchClient(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public void run(String... args) throws Exception {
        if (printProjectInfo) {
            log.info("{} running: {}, {}", buildProperties.getName(), buildProperties.getVersion(), buildProperties.getTime().toString());
        }
        if (printEsInfo) {
            EsUtil.printClusterStats();
            EsUtil.printNodes(elasticsearchClient);
            EsUtil.printHealth(elasticsearchClient);
        }
    }
}
