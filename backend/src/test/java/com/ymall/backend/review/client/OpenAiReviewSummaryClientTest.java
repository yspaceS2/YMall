package com.ymall.backend.review.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.review.config.ReviewSummaryProperties;
import com.ymall.backend.review.service.ReviewSummaryGenerator;

class OpenAiReviewSummaryClientTest {

    private MockRestServiceServer server;
    private OpenAiReviewSummaryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
            .baseUrl("http://review-model.test/engines/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiReviewSummaryClient(
            builder.build(),
            properties(),
            new ObjectMapper()
        );
    }

    @Test
    void sendsOpenAiCompatibleRequestAndParsesJsonContent() {
        server.expect(once(), requestTo(
                "http://review-model.test/engines/v1/chat/completions"
            ))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(
                """
                    {
                        "choices": [
                            {
                                "message": {
                                    "content": "```json\\n{\\"pros\\":[\\"연결이 빠릅니다.\\"],\\"cons\\":[],\\"commonOpinions\\":[]}\\n```"
                                }
                            }
                        ]
                    }
                    """,
                MediaType.APPLICATION_JSON
            ));

        ReviewSummaryGenerator.Result result = client.generate(List.of(
            new ReviewSummaryGenerator.Input(
                5,
                "연결이 빠르고 안정적입니다.",
                LocalDateTime.now()
            )
        ));

        assertThat(result.pros()).containsExactly("연결이 빠릅니다.");
        assertThat(result.cons()).isEmpty();
        assertThat(result.modelVersion()).isEqualTo("test-model");
        server.verify();
    }

    @Test
    void rejectsResponseThatViolatesSummaryContract() {
        server.expect(once(), requestTo(
                "http://review-model.test/engines/v1/chat/completions"
            ))
            .andRespond(withSuccess(
                """
                    {
                        "choices": [
                            {
                                "message": {
                                    "content": "{\\"pros\\":null,\\"cons\\":[],\\"commonOpinions\\":[]}"
                                }
                            }
                        ]
                    }
                    """,
                MediaType.APPLICATION_JSON
            ));

        assertThatThrownBy(() -> client.generate(List.of(
            new ReviewSummaryGenerator.Input(5, "좋습니다.", LocalDateTime.now())
        ))).isInstanceOf(IllegalStateException.class);
    }

    private ReviewSummaryProperties properties() {
        return new ReviewSummaryProperties(
            true,
            URI.create("http://review-model.test/engines/v1"),
            "test-model",
            10,
            100,
            1000,
            6000,
            192,
            Duration.ofSeconds(1),
            Duration.ofSeconds(3),
            Duration.ofMinutes(1)
        );
    }
}
