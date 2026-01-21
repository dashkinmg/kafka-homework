# Домашняя работа №2. Научиться разворачивать kafka с помощью kraft и самостоятельно настраивать безопасность.

1. Запустить Kafka с Kraft: 
     - Сгенерировать UUID кластера

    **выполнение команды:**
    ./bin/kafka-storage.sh random-uuid
    **результат:**
    IfOBocq4Q22PgYgk26zP9Q
    **скриншот:**
    Снимок экрана 1.png


    - Отформатировать папки для журналов
    **выполнение команды:**
    ./bin/kafka-storage.sh format -t IfOBocq4Q22PgYgk26zP9Q -c config/kraft/server.properties
    **результат:**
    metaPropertiesEnsemble=MetaPropertiesEnsemble(metadataLogDir=Optional.empty, dirs={/tmp/kraft-combined-logs: EMPTY})
    Formatting /tmp/kraft-combined-logs with metadata.version 3.7-IV4.
    **скриншот:**
    Снимок экрана 2.png


    - Запустить брокер
    **выполнение команды:**
    ./bin/kafka-server-start.sh config/kraft/server.properties
    **результат:**
    ...
    [2026-01-20 17:03:29,078] INFO Kafka version: 3.7.0 (org.apache.kafka.common.utils.AppInfoParser)
    [2026-01-20 17:03:29,078] INFO Kafka commitId: 2ae524ed625438c5 (org.apache.kafka.common.utils.AppInfoParser)
    [2026-01-20 17:03:29,078] INFO Kafka startTimeMs: 1768903409078 (org.apache.kafka.common.utils.AppInfoParser)
    [2026-01-20 17:03:29,079] INFO [KafkaRaftServer nodeId=1] Kafka Server started (kafka.server.KafkaRaftServer) 
    **скриншот:**
    Снимок экрана 3.png
    **описание:**
    При запуске использовал настройки config/kraft/server.properties, которые шли вместе с kafka_2.13-3.7.0.tgz
    Сразу проверил работоспособность командами: создать топик, показать список топиков, записать сообщения в топик, прочитать сообщения из топика:
        **выполнение команды создать топик:**
        bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic test-topic
        **результат:**
        Created topic test-topic.
        **выполнение команды показать список топиков:**
        bin/kafka-topics.sh --list --bootstrap-server localhost:9092
        **результат:**
        test-topic
        **выполнение команды записать сообщения в топик:**
        bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic
        >test 1
        >test 2
        **выполнение команды прочитать сообщения из топика:**
        bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic -from-beginning
        **результат:**
        test 1
        test 2
        **выполнение команды показать список топиков:**
        bin/kafka-topics.sh --list --bootstrap-server localhost:9092
        **результат:**
        __consumer_offsets
        test-topic
        **описание:**
        Видим что появился служебный топик __consumer_offsets
        **скриншот:**
        Снимок экрана 4.png

2. Настроить аутентификацию SASL/PLAIN. Создать трёх пользователей с произвольными именами.
    Настраиваем  аутентификацию SASL/PLAIN.

        Создаем файл private/kafka_server_jaas.conf
        **выполнение команды:**
        mkdir -p private
        **выполнение команды:**
        cat > private/kafka_server_jaas.conf << 'EOF'
KafkaServer {
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin"
    password="admin-secret"
    user_admin="admin-secret"
    user_Bob="Bob-secret"
    user_Alice="Alice-secret"
    user_A2="A2-secret";
};
EOF

        Создаем файл config/kraft/server-sasl.properties
        **выполнение команды:**
cat > config/kraft/server-sasl.properties << 'EOF'
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@localhost:9093
listeners=SASL_PLAINTEXT://:9094,CONTROLLER://:9093
advertised.listeners=SASL_PLAINTEXT://localhost:9094
inter.broker.listener.name=SASL_PLAINTEXT
controller.listener.names=CONTROLLER
listener.security.protocol.map=CONTROLLER:PLAINTEXT,SASL_PLAINTEXT:SASL_PLAINTEXT
sasl.enabled.mechanisms=PLAIN
sasl.mechanism.inter.broker.protocol=PLAIN
log.dirs=/tmp/kraft-combined-logs
authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
allow.everyone.if.no.acl.found=true
super.users=User:admin
EOF

        Создаем файл private/client-admin.properties
        **выполнение команды:**
cat > private/client-admin.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required   username="admin"   password="admin-secret";
EOF

        Создаем файл private/client-producer.properties
        **выполнение команды:**
cat > private/client-producer.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required   username="Bob"   password="Bob-secret";
EOF

        Создаем файл private/client-consumer.properties
        **выполнение команды:**
cat > private/client-consumer.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required   username="Alice"   password="Alice-secret";
EOF

        Создаем файл private/client-noaccess.properties
        **выполнение команды:**
cat > private/client-noaccess.properties << 'EOF'
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required   username="A2"   password="A2-secret";
EOF

       Все файлы приложены.


3. Настроить авторизацию. Создать топик. Первому пользователю выдать права на запись в этот топик. Второму пользователю выдать права на чтение этого топика. Третьему пользователю не выдавать никаких прав на этот топик.

    Останавливаем сервер кафка:
        **выполнение команды:**
        ./bin/kafka-server-stop.sh config/kraft/server.properties

    Запускаем сервер кафка:
        **выполнение команды:**
        KAFKA_OPTS="-Djava.security.auth.login.config=$(pwd)/private/kafka_server_jaas.conf"
./bin/kafka-server-start.sh config/kraft/server-sasl.properties

    Создаем топик:
        **выполнение команды:**
        bin/kafka-topics.sh --create --bootstrap-server localhost:9094 --topic test-acl-topic --command-config private/client-admin.properties
        **результат:**
        Created topic test-acl-topic.
        *Проверяем:*
        bin/kafka-topics.sh --list --bootstrap-server localhost:9094 --command-config private/client-consumer.properties
        **результат:**
        __consumer_offsets
        test-acl-topic
        test-topic
        **скриншот:**
        Снимок экрана 5.png

    Первому пользователю выдаем права на запись в этот топик.
        **выполнение команды:**
        bin/kafka-acls.sh --bootstrap-server localhost:9094 \
  --command-config private/client-admin.properties \
  --add --allow-principal User:Bob \
  --operation Write --topic test-acl-topic
        **результат:**
        Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=test-acl-topic, patternType=LITERAL)`: 
        (principal=User:Bob, host=*, operation=WRITE, permissionType=ALLOW) 

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=test-acl-topic, patternType=LITERAL)`: 
        (principal=User:Bob, host=*, operation=WRITE, permissionType=ALLOW) 

    Второму пользователю выдать права на чтение этого топика.
        **выполнение команды:**
        bin/kafka-acls.sh --bootstrap-server localhost:9094 \
  --command-config private/client-admin.properties \
  --add --allow-principal User:Alice \
  --operation Read --topic test-acl-topic
        **результат:**
        Adding ACLs for resource `ResourcePattern(resourceType=TOPIC, name=test-acl-topic, patternType=LITERAL)`: 
        (principal=User:Alice, host=*, operation=READ, permissionType=ALLOW) 

Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=test-acl-topic, patternType=LITERAL)`: 
        (principal=User:Alice, host=*, operation=READ, permissionType=ALLOW)
        (principal=User:Bob, host=*, operation=WRITE, permissionType=ALLOW) 

    Третьему пользователю (A2) не выдавать никаких прав на этот топик.
    Посмотрим список ACL:
    **выполнение команды:**
    bin/kafka-acls.sh --bootstrap-server localhost:9094 --list --command-config private/client-admin.properties
    **результат:**
    Current ACLs for resource `ResourcePattern(resourceType=TOPIC, name=test-acl-topic, patternType=LITERAL)`: 
        (principal=User:Alice, host=*, operation=READ, permissionType=ALLOW)
        (principal=User:Bob, host=*, operation=WRITE, permissionType=ALLOW) 

    **скриншот:**
        Снимок экрана 6.png

4. От имени каждого пользователя выполнить команды:
    Получить список топиков
        **выполнение команды:**
        bin/kafka-topics.sh --list --bootstrap-server localhost:9094 --command-config private/client-admin.properties
        **результат:**
        __consumer_offsets
        test-acl-topic
        test-topic
        **описание:**
        Видим что админ видит все.

        **выполнение команды:**
        bin/kafka-topics.sh --list --bootstrap-server localhost:9094 --command-config private/client-producer.properties
        **результат:**
        __consumer_offsets
        test-acl-topic
        test-topic
        **описание:**
        Видим что Bob видит все.

        **выполнение команды:**
        bin/kafka-topics.sh --list --bootstrap-server localhost:9094 --command-config private/client-consumer.properties
        **результат:**
        __consumer_offsets
        test-acl-topic
        test-topic
        **описание:**
        Видим что Alice видит все.

        **выполнение команды:**
        bin/kafka-topics.sh --list --bootstrap-server localhost:9094 --command-config private/client-noaccess.properties
        **результат:**
        __consumer_offsets
        test-topic
        **описание:**
        Видим что A2 видит только test-topic так как для test-acl-topic настроены права.

        Причина по, которой test-topic видят все в том что allow.everyone.if.no.acl.found=true в файле config/kraft/server-sasl.properties

        **скриншот:**
        Снимок экрана 7.png

    Записать сообщения в топик

        **выполнение команды:**
        bin/kafka-console-producer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --producer.config private/client-admin.properties
        **результат:**
        >admin1
        >admin2
        >admin3
        **описание:**
        Видим что админ может писать в топик.  

        **выполнение команды:**
        bin/kafka-console-producer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --producer.config private/client-producer.properties
        **результат:**
        >Bob 1
        >Bob2
        >Bob 3
        **описание:**
        Видим что Bob может писать в топик.  

        **выполнение команды:**
        bin/kafka-console-producer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --producer.config private/client-consumer.properties
        **результат:**
        >Alice 1
>[2026-01-20 19:11:32,200] ERROR Error when sending message to topic test-acl-topic with key: null, value: 7 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test-acl-topic]
        **описание:**
        Видим что Alice НЕ может писать в топик. 

        **выполнение команды:**
        bin/kafka-console-producer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --producer.config private/client-noaccess.properties
        **результат:**
        >A2 1
[2026-01-20 19:12:48,250] WARN [Producer clientId=console-producer] Error while fetching metadata with correlation id 5 : {test-acl-topic=TOPIC_AUTHORIZATION_FAILED} (org.apache.kafka.clients.NetworkClient)
[2026-01-20 19:12:48,253] ERROR [Producer clientId=console-producer] Topic authorization failed for topics [test-acl-topic] (org.apache.kafka.clients.Metadata)
[2026-01-20 19:12:48,254] ERROR Error when sending message to topic test-acl-topic with key: null, value: 4 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test-acl-topic]
        **описание:**
        Видим что A2 НЕ может писать в топик.  

        **скриншот:**
        Снимок экрана 8.png

    Прочитать сообщения из топика
        **выполнение команды:**
        bin/kafka-console-consumer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --consumer.config private/client-admin.properties -from-beginning
        **результат:**
        admin1
        admin2
        admin3
        Bob 1
        Bob2
        Bob 3
        **описание:**
        Видим что админ может читать сообщения из топика.

        **выполнение команды:**
        bin/kafka-console-consumer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --consumer.config private/client-producer.properties -from-beginning
        **результат:**
        [2026-01-20 19:18:32,651] WARN [Consumer clientId=console-consumer, groupId=console-consumer-97231] Not authorized to read from partition test-acl-topic-0. (org.apache.kafka.clients.consumer.internals.FetchCollector)
[2026-01-20 19:18:32,654] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test-acl-topic]
Processed a total of 0 messages
        **описание:**
        Видим что Bob НЕ может читать сообщения из топика.

        **выполнение команды:**
        bin/kafka-console-consumer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --consumer.config private/client-consumer.properties -from-beginning
        **результат:**
        admin1
        admin2
        admin3
        Bob 1
        Bob2
        Bob 3
        **описание:**
        Видим что Alice может читать сообщения из топика.

        **выполнение команды:**
        bin/kafka-console-consumer.sh --bootstrap-server localhost:9094 --topic test-acl-topic --consumer.config private/client-noaccess.properties -from-beginning
        **результат:**
        [2026-01-20 19:20:22,373] WARN [Consumer clientId=console-consumer, groupId=console-consumer-61311] Error while fetching metadata with correlation id 2 : {test-acl-topic=TOPIC_AUTHORIZATION_FAILED} (org.apache.kafka.clients.NetworkClient)
[2026-01-20 19:20:22,374] ERROR [Consumer clientId=console-consumer, groupId=console-consumer-61311] Topic authorization failed for topics [test-acl-topic] (org.apache.kafka.clients.Metadata)
[2026-01-20 19:20:22,374] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test-acl-topic]
Processed a total of 0 messages
        **описание:**
        Видим что A2 НЕ может читать сообщения из топика.

        **скриншот:**
        Снимок экрана 9.png