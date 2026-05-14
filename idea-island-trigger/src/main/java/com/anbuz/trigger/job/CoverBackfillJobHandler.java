package com.anbuz.trigger.job;

import com.anbuz.domain.content.service.IContentProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CoverBackfillJobHandler {

    private final IContentProcessService contentProcessService;

    private final int limit;

    public CoverBackfillJobHandler(IContentProcessService contentProcessService,
                                   @Value("${idea-island.jobs.cover-backfill.limit:50}") int limit) {
        this.contentProcessService = contentProcessService;
        this.limit = limit;
    }

    @Scheduled(cron = "${idea-island.jobs.cover-backfill.cron:0 10 4 * * *}",
            zone = "${idea-island.jobs.timezone:Asia/Shanghai}")
    public void execute() {
        log.info("Cover backfill job started limit={}", limit);
        int processed = contentProcessService.backfillMissingCovers(limit);
        log.info("Cover backfill job completed processed={}", processed);
    }
}
