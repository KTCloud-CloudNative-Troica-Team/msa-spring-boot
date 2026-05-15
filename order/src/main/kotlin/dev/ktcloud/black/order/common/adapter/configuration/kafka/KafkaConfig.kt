package dev.ktcloud.black.order.common.adapter.configuration.kafka

import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReleaseRequestMessage
import dev.ktcloud.black.order.common.adapter.infrastructure.kafka.model.InventoryReservedResultMessage
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaConfig(
    @Value("\${spring.kafka.bootstrap-servers}")
    private val bootstrapServers: String,
    @Value("\${spring.kafka.topic.inventory-reserved-result-dlt:inventory-reserved-result-dlt}")
    private val reservedResultDlt: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun inventoryReservedResultConsumerFactory(): ConsumerFactory<String, InventoryReservedResultMessage> {
        val deserializer = JsonDeserializer(InventoryReservedResultMessage::class.java).apply {
            addTrustedPackages("dev.ktcloud.black.*")
            setUseTypeHeaders(false)
        }

        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        )

        return DefaultKafkaConsumerFactory(configProps, StringDeserializer(), deserializer)
    }

    @Bean
    fun inventoryReleaseRequestKafkaTemplate(): KafkaTemplate<String, InventoryReleaseRequestMessage> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            JsonSerializer.ADD_TYPE_INFO_HEADERS to false,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to Int.MAX_VALUE,
            ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG to 120000,
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(configProps))
    }

    @Bean
    fun dltKafkaTemplate(): KafkaTemplate<String, Any> {
        val configProps = mapOf<String, Any>(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            JsonSerializer.ADD_TYPE_INFO_HEADERS to false,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
            ProducerConfig.ACKS_CONFIG to "all",
        )
        return KafkaTemplate(DefaultKafkaProducerFactory(configProps))
    }

    @Bean
    fun inventoryReservedResultContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
        dltKafkaTemplate: KafkaTemplate<String, Any>,
    ): ConcurrentKafkaListenerContainerFactory<String, InventoryReservedResultMessage> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, InventoryReservedResultMessage>()
        val cf = inventoryReservedResultConsumerFactory()

        factory.consumerFactory = cf

        configurer.configure(
            factory as ConcurrentKafkaListenerContainerFactory<Any, Any>,
            cf as ConsumerFactory<Any, Any>,
        )

        @Suppress("UNCHECKED_CAST")
        val recoverer = DeadLetterPublishingRecoverer(
            dltKafkaTemplate as KafkaOperations<Any, Any>,
        ) { record, ex ->
            log.warn(
                "Routing record to DLT: topic={}, partition={}, offset={}, dlt={}, cause={}",
                record.topic(), record.partition(), record.offset(), reservedResultDlt, ex.message,
            )
            TopicPartition(reservedResultDlt, record.partition())
        }
        factory.setCommonErrorHandler(DefaultErrorHandler(recoverer, FixedBackOff(1000L, 3L)))

        return factory
    }
}
