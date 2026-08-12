package com.ymall.backend.review.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
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
        리뷰의 평가 방향을 보존하고 긍정 의견을 cons에, 부정 의견을 pros에 넣지 마세요.
        원문의 의미를 바꾸거나 근거 없는 표현을 덧붙이지 마세요.
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
            "response_format", Map.of("type", "json_object")
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
        Exception lastException = null;
        List<String> pros = null;
        List<String> cons = null;
        List<String> commonOpinions = null;
        boolean hasPros = false;
        boolean hasCons = false;
        boolean hasCommonOpinions = false;
        for (String json : extractJsonObjects(content)) {
            try {
                JsonNode summaryNode = objectMapper.readTree(json);
                if (!summaryNode.isObject()) {
                    continue;
                }
                if (summaryNode.has("pros")) {
                    pros = normalized(summaryNode.get("pros"));
                    hasPros = true;
                }
                if (summaryNode.has("cons")) {
                    cons = normalized(summaryNode.get("cons"));
                    hasCons = true;
                }
                if (summaryNode.has("commonOpinions")) {
                    commonOpinions = normalized(summaryNode.get("commonOpinions"));
                    hasCommonOpinions = true;
                } else if (summaryNode.has("common_opinions")) {
                    commonOpinions = normalized(summaryNode.get("common_opinions"));
                    hasCommonOpinions = true;
                }
                if (hasPros && hasCons && hasCommonOpinions) {
                    return new Result(pros, cons, commonOpinions, properties.model());
                }
            } catch (Exception exception) {
                lastException = exception;
            }
        }
        throw new IllegalStateException(
            "AI review summary response is not valid JSON.",
            lastException
        );
    }

    private List<String> extractJsonObjects(String content) {
        if (content == null) {
            throw new IllegalStateException("AI review summary response content is missing.");
        }

        List<String> objects = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = 0; index < content.length(); index++) {
            char character = content.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == '"') {
                    inString = false;
                }
                continue;
            }
            if (character == '"') {
                inString = true;
                continue;
            }
            if (character == '{') {
                if (depth == 0) {
                    start = index;
                }
                depth++;
                continue;
            }
            if (character == '}' && depth > 0) {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(content.substring(start, index + 1));
                    start = -1;
                }
            }
        }
        if (objects.isEmpty()) {
            throw new IllegalStateException("AI review summary response has no JSON object.");
        }
        return List.copyOf(objects);
    }

    private List<String> normalized(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .limit(3)
            .toList();
    }

    private List<String> normalized(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("AI review summary section must be an array.");
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (!item.isTextual()) {
                throw new IllegalArgumentException(
                    "AI review summary section items must be strings."
                );
            }
            values.add(item.asText());
        });
        return normalized(values);
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(String content) {
    }

}
