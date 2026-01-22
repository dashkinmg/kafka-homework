1. Запустить Kafka
    Докер образ находится в папке hw3/kafka/
    запустил командой docker compose up -d

2. Создать два топика: topic1 и topic2

Тут два варианта:
    1. Через терминал 
        Войти в образ докер командой docker exec -it kafka1-otuskafka bash. Внутри  найти kafka-topics (командой whereis kafka-topics) войти в директорию /usr/bin/ 
        и выполнить две команды:
            kafka-topics --create --topic topic1 --bootstrap-server kafka1:9191
            kafka-topics --create --topic topic2 --bootstrap-server kafka1:9191
    2. В самой программе топики пересоздаются методом Utils.recreateTopics(1, 1, "topic1", "topic2");

3. Разработать приложение
    Исходный код приложения лежит в папке hw3/HomeWoprk3src/

    В коде я создаю транзакцию, записываю 5 сообщений в оба топика, подтверждаю транзакцию, открываю другую транзакцию, записываю 2 сообщения в оба топика, отменяю транзакцию:

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


    а далее я читаю сообщения:

            Utils.log.info("Читаем первый топик.");
            var consumer1 = new LoggingConsumer("cons1", "topic1", Utils.consumerConfig, true);

            // Ждем, пока консьюмеры прочитают сообщения
            Thread.sleep(10000);

            Utils.log.info("Читаем второй топик.");
            var consumer2 = new LoggingConsumer("cons2", "topic2", Utils.consumerConfig, true);

            // Ждем, пока консьюмеры прочитают сообщения
            Thread.sleep(10000);


В логах:

01:07:01.362 [main] INFO  appl - Пересоздаем топики.
01:07:01.913 [main] INFO  appl - External topics: [topic1, topic2]
01:07:01.940 [main] INFO  appl - SUCCESS
01:07:02.133 [main] INFO  appl - Инициализируем транзакцию.
01:07:02.251 [main] INFO  appl - Открываем транзакцию.
01:07:02.251 [main] INFO  appl - Отправляем 5 сообщений в оба топика.
01:07:02.277 [main] INFO  appl - Подтверждаем транзакцию.
01:07:02.299 [main] INFO  appl - Открываем транзакцию.
01:07:02.299 [main] INFO  appl - Отправляем 2 сообщения в оба топика.
01:07:02.300 [main] INFO  appl - Отменяем транзакцию.
01:07:02.304 [main] INFO  appl - Читаем первый топик.
01:07:02.324 [MessagesReceiver.cons1] INFO  appl - Start!
01:07:02.362 [MessagesReceiver.cons1] INFO  appl - Subscribed
01:07:02.429 [MessagesReceiver.cons1] INFO  appl - Receive null:some-tr-0 at 0
01:07:02.429 [MessagesReceiver.cons1] INFO  appl - Receive null:some-tr-1 at 1
01:07:02.430 [MessagesReceiver.cons1] INFO  appl - Receive null:some-tr-2 at 2
01:07:02.430 [MessagesReceiver.cons1] INFO  appl - Receive null:some-tr-3 at 3
01:07:02.430 [MessagesReceiver.cons1] INFO  appl - Receive null:some-tr-4 at 4
01:07:12.329 [main] INFO  appl - Читаем второй топик.
01:07:12.329 [MessagesReceiver.cons2] INFO  appl - Start!
01:07:12.334 [MessagesReceiver.cons2] INFO  appl - Subscribed
01:07:12.360 [MessagesReceiver.cons2] INFO  appl - Receive null:other-tr-0 at 0
01:07:12.360 [MessagesReceiver.cons2] INFO  appl - Receive null:other-tr-1 at 1
01:07:12.360 [MessagesReceiver.cons2] INFO  appl - Receive null:other-tr-2 at 2
01:07:12.360 [MessagesReceiver.cons2] INFO  appl - Receive null:other-tr-3 at 3
01:07:12.360 [MessagesReceiver.cons2] INFO  appl - Receive null:other-tr-4 at 4

видно что сообщения в отмененной транзакции не были прочитаны.