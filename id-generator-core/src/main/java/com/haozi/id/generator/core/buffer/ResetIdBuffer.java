package com.haozi.id.generator.core.buffer;

import com.haozi.id.generator.common.ServiceThread;
import com.haozi.id.generator.core.IdGeneratorFactory;
import com.haozi.id.generator.core.rule.RuntimeSequence;
import com.haozi.id.generator.core.rule.repository.SequenceEnum;
import com.haozi.id.generator.core.util.SequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 重置、复位ID
 *
 * @author haozi
 * @date 2019-11-1014:37
 */
@Slf4j
public class ResetIdBuffer extends ServiceThread {
    //15分钟 TODO 加到配置项
    private final static Long DEFAULT_WAIT_INTERVAL = 15L * 60 * 1000L;

    private IdGeneratorFactory idGeneratorFactory;

    public ResetIdBuffer(IdGeneratorFactory idGeneratorFactory) {
        this.idGeneratorFactory = idGeneratorFactory;
    }

    /**
     * 重置、复位ID
     * <p>
     * 通过创建新sequence_key达到复位效果
     * <p>
     * 定时任务频率不能超过生产任务频率，避免重复重置
     */
    public void reset() {
        idGeneratorFactory.getSequenceRuleService().getRunningRule().stream()
                //是否配置重置
                .filter(sequenceRule -> !StringUtils.isEmpty(sequenceRule.getResetRule()))
                //是否到重置时间
                .filter(sequenceRule -> SequenceUtil.isReset(sequenceRule))
                //循环重置
                .forEach(sequenceRule -> {
                    RuntimeSequence runtimeSequence = idGeneratorFactory.getSequenceRuleService().getSequenceRuntime(sequenceRule, SequenceEnum.Runtime.NEXT);
                    //是否已重置
                    if (BufferPool.getBuffer(runtimeSequence.getSequenceKey()) != null) {
                        return;
                    }
                    idGeneratorFactory.getIdProducer().product(runtimeSequence);
                });
    }

    @Override
    public String getServiceName() {
        return "Id-Rest-Thread";
    }

    @Override
    public void run() {
        log.info(this.getServiceName() + " service started");

        while (!this.isStopped()) {
            this.waitForRunning(DEFAULT_WAIT_INTERVAL);
            this.reset();
        }

        log.info(this.getServiceName() + " service end");
    }
}
