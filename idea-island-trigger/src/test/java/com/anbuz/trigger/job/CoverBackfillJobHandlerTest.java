package com.anbuz.trigger.job;

import com.anbuz.domain.content.service.IContentProcessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CoverBackfillJobHandler")
class CoverBackfillJobHandlerTest {

    @Mock
    private IContentProcessService contentProcessService;

    @Test
    @DisplayName("delegates scheduled cover backfill to content process service")
    void givenScheduledTriggered_whenExecute_thenDelegatesToDomainService() {
        CoverBackfillJobHandler coverBackfillJobHandler = new CoverBackfillJobHandler(contentProcessService, 50);
        coverBackfillJobHandler.execute();

        verify(contentProcessService).backfillMissingCovers(50);
    }
}
