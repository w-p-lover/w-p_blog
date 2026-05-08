package com.ican.handler;

import com.ican.BlogApplication;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * ES bulk indexing test - disabled pending ES migration.
 * RestHighLevelClient was removed in Spring Boot 3 / ES 8.x.
 * @Author：yep
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BlogApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Ignore("ES bulk indexing test is disabled pending ElasticsearchClient migration")
public class takes {

    @Test
    public void takeBluk() {
        // TODO: re-implement with ElasticsearchClient (ES 8 Java API)
        // when Elasticsearch is available in the environment.
    }
}
