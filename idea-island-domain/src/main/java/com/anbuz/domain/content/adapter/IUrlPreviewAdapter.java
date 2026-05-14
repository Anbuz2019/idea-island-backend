package com.anbuz.domain.content.adapter;

import com.anbuz.domain.content.model.UrlPreviewMetadata;

import java.util.Optional;

public interface IUrlPreviewAdapter {

    Optional<UrlPreviewMetadata> fetchPreview(String url);

}
