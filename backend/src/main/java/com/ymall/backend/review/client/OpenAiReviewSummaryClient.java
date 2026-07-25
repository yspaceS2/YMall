package com.ymall.backend.review.client;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

import com.ymall.backend.review.config.ReviewSummaryProperties;
import com.ymall.backend.review.service.ReviewSummaryGenerator;

@Component
@RequiredArgsConstructor
public class OpenAiReviewSummaryClient implements ReviewSummaryGenerator {

    private static final String SYSTEM_PROMPT = """
        당신은 한국어 쇼핑몰 리뷰 요약기입니다.
        리뷰에 없는 내용을 만들지 말고 JSON 객체만 출력하세요.
        장점은 pros, 단점은 cons, 반복되는 의견은 commonOpinions에 최대 3개씩 작성하세요.
        해당 의견이 없으면 빈 배열을 사용하세요. /no_think
        """;

    private final RestClient reviewSummaryRestClient;
    private final ReviewSummaryProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Result generate(List<Input> reviews) {
        ChatCompletionResponse response = reviewSummaryRestClient.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request(reviews))
            .retrieve()
            .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("AI review summary response has no choices.");
        }
        return parse(response.choices().get(0).message().content());
    }

    private Map<String, Object> request(List<Input> reviews) {
        return Map.of(
            "model", properties.model(),
            "messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt(reviews))
            ),
            "temperature", 0,
            "max_tokens", properties.maximumTokens(),
            "stream", false,
            "response_format", Map.of(
                "type", "json_schema",
                "schema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "pros", stringArraySchema(),
                        "cons", stringArraySchema(),
                        "commonOpinions", stringArraySchema()
                    ),
                    "required", List.of("pros", "cons", "commonOpinions"),
                    "additionalProperties", false
                )
            )
        );
    }

    private Map<String, Object> stringArraySchema() {
        return Map.of(
            "type", "array",
            "items", Map.of("type", "string"),
            "maxItems", 3
        );
    }

    private String userPrompt(List<Input> reviews) {
        StringBuilder prompt = new StringBuilder(
            "다음 리뷰만 근거로 장점, 단점, 반복 의견을 요약하세요.\n"
        );
        for (int index = 0; index < reviews.size(); index++) {
            Input review = reviews.get(index);
            prompt.append("- 리뷰 ")
                .append(index + 1)
                .append(" (")
                .append(review.rating())
                .append("점): ")
                .append(review.content().trim())
                .append('\n');
        }
        return prompt.append("/no_think").toString();
    }

    private Result parse(String content) {
        String json = extractJson(content);
        try {
            SummaryContent summary = objectMapper.readValue(json, SummaryContent.class);
            validate(summary.pros());
            validate(summary.cons());
            validate(summary.commonOpinions());
            return new Result(
                normalized(summary.pros()),
                normalized(summary.cons()),
                normalized(summary.commonOpinions()),
                properties.model()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("AI review summary response is not valid JSON.", exception);
        }
    }

    private String extractJson(String content) {
        if (content == null) {
            throw new IllegalStateException("AI review summary response content is missing.");
        }
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw new IllegalStateException("AI review summary response has no JSON object.");
        }
        return content.substring(firstBrace, lastBrace + 1);
    }

    private void validate(List<String> values) {
        if (values == null || values.size() > 3) {
            throw new IllegalStateException("AI review summary response violates the list contract.");
        }
    }

    private List<String> normalized(List<String> values) {
        return values.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }

    private record SummaryContent(
        List<String> pros,
        List<String> cons,
        List<String> commonOpinions
    ) {
    }
}
