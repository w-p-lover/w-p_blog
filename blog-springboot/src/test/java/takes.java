
import com.alibaba.fastjson2.JSON;
import com.ican.BlogApplication;
import com.ican.entity.Article;
import com.ican.model.vo.ArticleSearchVO;
import com.ican.service.ArticleService;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.List;

import static com.ican.constant.ElasticConstant.ARTICLE_INDEX;

/**
 * @Author：yep
 * @Project：blog-springboot
 * @name：takees
 * @Date：2024/7/30 17:36
 * @Filename：takees
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BlogApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class takes {
    @Autowired
    ArticleService articleService;
    @Autowired
    private RestHighLevelClient client;

    @Test
    public void takeBluk() {
        List<Article> list = articleService.list();
        for (Article article : list) {
            ArticleSearchVO build = ArticleSearchVO.builder()
                    .id(article.getId())
                    .articleContent(article.getArticleContent())
                    .articleTitle(article.getArticleTitle())
                    .status(article.getStatus())
                    .isDelete(article.getIsDelete())
                    .build();
            String json = JSON.toJSONString(build);
            try {
                IndexRequest indexRequest = new IndexRequest()
                        .index(ARTICLE_INDEX)
                        .id(article.getId().toString())
                        .source(json, XContentType.JSON);
                client.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
