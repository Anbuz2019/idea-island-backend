package com.anbuz.trigger.job;

import com.anbuz.domain.material.service.IAutoInvalidService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoInvalidJobHandler 自动失效任务入口")
class AutoInvalidJobHandlerTest {

    @Mock
    private IAutoInvalidService autoInvalidService;

    @InjectMocks
    private AutoInvalidJobHandler autoInvalidJobHandler;

    @Nested
    @DisplayName("执行任务")
    class Execute {

        @Test
        @DisplayName("触发 XXL-Job 时，只委托领域服务执行自动失效扫描")
        void givenJobTriggered_whenExecute_thenDelegatesToDomainService() {
            autoInvalidJobHandler.execute();

            verify(autoInvalidService).invalidateExpiredMaterials();
        }
    }
}
