package com.anbuz.domain.content.adapter;

import java.util.Optional;

public interface IUrlParserAdapter {

    Optional<String> extractOgImage(String url);

    Optional<String> extractOgTitle(String url);

}
