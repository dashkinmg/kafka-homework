package org.example;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.utils.LoggingConsumer;
import org.example.utils.Utils;
import org.apache.kafka.clients.producer.KafkaProducer;

public class Main {

    public static void main(String[] args) throws Exception {

        Utils.log.info("Пересоздаем топики.");
        Utils.recreateTopics(1, 1, "topic1", "topic2");

        try (
                var producer = new KafkaProducer<String, String>(Utils.createProducerConfig(b -> {
                            b.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "hw3");
                        }
                ))
        ) {

            Utils.log.info("Инициализируем транзакцию.");
            producer.initTransactions();

            Utils.log.info("Открываем транзакцию.");
            producer.beginTransaction();

            Utils.log.info("Отправляем 5 сообщений в оба топика.");
            for (int i = 0; i < 5; ++i) {
                producer.send(new ProducerRecord<>("topic1", "some-tr-" + i));
                producer.send(new ProducerRecord<>("topic2", "other-tr-" + i));
            }

            Utils.log.info("Подтверждаем транзакцию.");
            producer.commitTransaction();

            Utils.log.info("Открываем транзакцию.");
            producer.beginTransaction();

            Utils.log.info("Отправляем 2 сообщения в оба топика.");
            for (int i = 0; i < 2; ++i) {
                producer.send(new ProducerRecord<>("topic1", "some-ab" + i));
                producer.send(new ProducerRecord<>("topic2", "other-ab" + i));
            }

            Utils.log.info("Отменяем транзакцию.");
            producer.abortTransaction();


            Utils.log.info("Читаем первый топик.");
            var consumer1 = new LoggingConsumer("cons1", "topic1", Utils.consumerConfig, true);

            // Ждем, пока консьюмеры прочитают сообщения
            Thread.sleep(10000);

            Utils.log.info("Читаем второй топик.");
            var consumer2 = new LoggingConsumer("cons2", "topic2", Utils.consumerConfig, true);

            // Ждем, пока консьюмеры прочитают сообщения
            Thread.sleep(10000);

        }

    }
}