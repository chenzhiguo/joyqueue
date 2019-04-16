package com.jd.journalq.broker.kafka.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jd.journalq.broker.kafka.KafkaCommandHandler;
import com.jd.journalq.broker.kafka.KafkaCommandType;
import com.jd.journalq.broker.kafka.KafkaContext;
import com.jd.journalq.broker.kafka.KafkaContextAware;
import com.jd.journalq.broker.kafka.KafkaErrorCode;
import com.jd.journalq.broker.kafka.command.TxnOffsetCommitRequest;
import com.jd.journalq.broker.kafka.command.TxnOffsetCommitResponse;
import com.jd.journalq.broker.kafka.coordinator.transaction.TransactionCoordinator;
import com.jd.journalq.broker.kafka.coordinator.transaction.exception.TransactionException;
import com.jd.journalq.broker.kafka.helper.KafkaClientHelper;
import com.jd.journalq.broker.kafka.model.OffsetAndMetadata;
import com.jd.journalq.broker.kafka.model.PartitionMetadataAndError;
import com.jd.journalq.network.transport.Transport;
import com.jd.journalq.network.transport.command.Command;
import com.jd.journalq.network.transport.command.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * TxnOffsetCommitRequestHandler
 * author: gaohaoxiang
 * email: gaohaoxiang@jd.com
 * date: 2019/4/4
 */
public class TxnOffsetCommitRequestHandler implements KafkaCommandHandler, Type, KafkaContextAware {

    protected static final Logger logger = LoggerFactory.getLogger(TxnOffsetCommitRequestHandler.class);

    private TransactionCoordinator transactionCoordinator;

    @Override
    public void setKafkaContext(KafkaContext kafkaContext) {
        this.transactionCoordinator = kafkaContext.getTransactionCoordinator();
    }

    @Override
    public Command handle(Transport transport, Command command) {
        TxnOffsetCommitRequest txnOffsetCommitRequest = (TxnOffsetCommitRequest) command.getPayload();
        String clientId = KafkaClientHelper.parseClient(txnOffsetCommitRequest.getClientId());
        TxnOffsetCommitResponse response = null;

        try {
            Map<String, List<PartitionMetadataAndError>> errors = transactionCoordinator.handleCommitOffset(clientId, txnOffsetCommitRequest.getTransactionId(), txnOffsetCommitRequest.getGroupId(),
                    txnOffsetCommitRequest.getProducerId(), txnOffsetCommitRequest.getProducerEpoch(), txnOffsetCommitRequest.getPartitions());
            response = new TxnOffsetCommitResponse(errors);
        } catch (TransactionException e) {
            logger.warn("commit offset to txn exception, code: {}, message: {}, request: {}", e.getCode(), e.getMessage(), txnOffsetCommitRequest, e);
            response = new TxnOffsetCommitResponse(buildPartitionError(e.getCode(), txnOffsetCommitRequest.getPartitions()));
        } catch (Exception e) {
            logger.error("commit offset to txn exception, request: {}", txnOffsetCommitRequest, e);
            response = new TxnOffsetCommitResponse(buildPartitionError(KafkaErrorCode.exceptionFor(e), txnOffsetCommitRequest.getPartitions()));
        }
        return new Command(response);
    }

    protected Map<String, List<PartitionMetadataAndError>> buildPartitionError(int code, Map<String, List<OffsetAndMetadata>> partitions) {
        Map<String, List<PartitionMetadataAndError>> result = Maps.newHashMapWithExpectedSize(partitions.size());
        for (Map.Entry<String, List<OffsetAndMetadata>> entry : partitions.entrySet()) {
            List<PartitionMetadataAndError> partitionMetadataAndErrors = Lists.newArrayListWithCapacity(entry.getValue().size());
            for (OffsetAndMetadata partition : entry.getValue()) {
                partitionMetadataAndErrors.add(new PartitionMetadataAndError(partition.getPartition(), (short) code));
            }
            result.put(entry.getKey(), partitionMetadataAndErrors);
        }
        return result;
    }

    @Override
    public int type() {
        return KafkaCommandType.TXN_OFFSET_COMMIT.getCode();
    }
}