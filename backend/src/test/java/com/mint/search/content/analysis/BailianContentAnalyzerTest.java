package com.mint.search.content.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BailianContentAnalyzerTest {
    private final BailianContentAnalyzer analyzer = new BailianContentAnalyzer(new BailianProperties(), new ObjectMapper());

    @Test
    void parsesOpenAiCompatibleJsonContent() {
        String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"summary\\":\\"百炼摘要\\",\\"tags\\":[\\"AI\\",\\"搜索\\"],\\"recommendReason\\":\\"匹配你的兴趣\\"}"
                      }
                    }
                  ]
                }
                """;

        var result = analyzer.parseResponse(response);

        assertThat(result).isPresent();
        assertThat(result.get().summary()).isEqualTo("百炼摘要");
        assertThat(result.get().tags()).containsExactly("AI", "搜索");
        assertThat(result.get().recommendReason()).isEqualTo("匹配你的兴趣");
        assertThat(result.get().enhanced()).isTrue();
    }

    @Test
    void returnsEmptyWhenModelContentIsNotJson() {
        String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "这不是 JSON"
                      }
                    }
                  ]
                }
                """;

        assertThat(analyzer.parseResponse(response)).isEmpty();
    }
}
