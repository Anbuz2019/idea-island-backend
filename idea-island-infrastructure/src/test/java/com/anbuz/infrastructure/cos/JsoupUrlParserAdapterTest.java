package com.anbuz.infrastructure.cos;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JsoupUrlParserAdapter scenarios")
class JsoupUrlParserAdapterTest {

    private final JsoupUrlParserAdapter adapter = new JsoupUrlParserAdapter();

    @Test
    @DisplayName("prefers twitter image when OG image is missing")
    void givenTwitterImage_whenExtractBestImage_thenReturnsAbsoluteImageUrl() {
        Document doc = Jsoup.parse("""
                <html>
                  <head><meta name="twitter:image" content="/covers/card.jpg"></head>
                  <body><article><img src="/article/inline.jpg" width="1200" height="800"></article></body>
                </html>
                """, "https://example.com/posts/1");

        assertThat(adapter.extractBestImage(doc))
                .hasValue("https://example.com/covers/card.jpg");
    }

    @Test
    @DisplayName("falls back to the strongest article image")
    void givenArticleImages_whenExtractBestImage_thenSkipsLogoAndReturnsLargestArticleImage() {
        Document doc = Jsoup.parse("""
                <html>
                  <body>
                    <img src="/logo.png" class="site-logo" width="600" height="400">
                    <article>
                      <img src="/small.jpg" width="300" height="200">
                      <img src="/hero.webp" width="1200" height="630" alt="cover">
                    </article>
                  </body>
                </html>
                """, "https://example.com/posts/1");

        assertThat(adapter.extractBestImage(doc))
                .hasValue("https://example.com/hero.webp");
    }

    @Test
    @DisplayName("extracts image from JSON-LD image object")
    void givenJsonLdImageObject_whenExtractBestImage_thenReturnsObjectUrl() {
        Document doc = Jsoup.parse("""
                <html>
                  <head>
                    <script type="application/ld+json">
                    {"@type":"NewsArticle","image":{"url":"https://cdn.example.com/jsonld-cover.jpg"}}
                    </script>
                  </head>
                </html>
                """, "https://example.com");

        assertThat(adapter.extractBestImage(doc))
                .hasValue("https://cdn.example.com/jsonld-cover.jpg");
    }

    @Test
    @DisplayName("extracts title from twitter title before document title")
    void givenTwitterTitle_whenExtractBestTitle_thenReturnsMetaTitle() {
        Document doc = Jsoup.parse("""
                <html>
                  <head>
                    <title>Fallback title</title>
                    <meta name="twitter:title" content="Card title">
                  </head>
                </html>
                """, "https://example.com");

        assertThat(adapter.extractBestTitle(doc)).hasValue("Card title");
    }
}
