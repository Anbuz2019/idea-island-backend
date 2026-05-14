package com.anbuz.infrastructure.cos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BilibiliUrlPreviewAdapter")
class BilibiliUrlPreviewAdapterTest {

    private final BilibiliUrlPreviewAdapter adapter = new BilibiliUrlPreviewAdapter();

    @Test
    @DisplayName("builds public API url from bvid video page")
    void givenBvidVideoPage_whenToApiUrl_thenUsesBvid() {
        assertThat(adapter.toApiUrl("https://www.bilibili.com/video/BV1JFoRBcEoY"))
                .contains("https://api.bilibili.com/x/web-interface/view?bvid=BV1JFoRBcEoY");
    }

    @Test
    @DisplayName("builds public API url from API url")
    void givenViewApiUrl_whenToApiUrl_thenKeepsBvid() {
        assertThat(adapter.toApiUrl("https://api.bilibili.com/x/web-interface/view?bvid=BV1JFoRBcEoY"))
                .contains("https://api.bilibili.com/x/web-interface/view?bvid=BV1JFoRBcEoY");
    }

    @Test
    @DisplayName("builds public API url from av video page")
    void givenAvVideoPage_whenToApiUrl_thenUsesAid() {
        assertThat(adapter.toApiUrl("https://www.bilibili.com/video/av123456"))
                .contains("https://api.bilibili.com/x/web-interface/view?aid=123456");
    }
}
