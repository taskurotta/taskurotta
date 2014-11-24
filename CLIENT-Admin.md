# Запуск JVM с клиентски кодом
Для запуска JVM c клиентским кодом необходимо выполнить команду или создать исполняемый файл со следующим содержимым
    
    <путь до java> [дополнительные параметры] -jar <имя jar арихва>.jar -f <путь до конфигурационного файла>
        
Например:

    "c:\Program Files\Java\jdk1.7.0_45\bin\java" -Xmx64m -Xms64m -jar assemble/target/assemble-0.5.0-SNAPSHOT.jar -f assemble/src/main/resources/tests/stress/mem/b-localhost-8081.yml
    
# Внесение параметров приложения в конфигурационный файл
Конфигурационный файл описан в формате [YAML](http://ru.wikipedia.org/wiki/YAML).
Параметры могут быть двух типов: параметры исполнения и параметры подключения к серверу Taskurotta.

## Параметры исполнения
Пример конфигурации

    runtime:
      - MainRuntimeConfig:
          class: ru.taskurotta.spring.configs.RuntimeConfigPathXmlApplicationContext
          instance:
            context: ru/taskurotta/recipes/calculate/RuntimeBeans.xml
            properties:
              sleep: -1
              failChance: 0.3
              varyExceptions: true
              
Все параметры должны быть получены от разработчика и описаны в блоке properties.

## Параметры подключения к серверу Taskurotta
Пример конфигурации

    spreader:
      - MainTaskSpreaderConfig:
          class: ru.taskurotta.spring.configs.SpreaderConfigPathXmlApplicationContext
          instance:
            context: ru/taskurotta/recipes/calculate/SpreaderBeans-jersey.xml
            properties:              
              endpoint: "http://localhost:8811"
              threadPoolSize: 10
              readTimeout: 0
              connectTimeout: 3000
              
Все параметры должны быть описаны в блоке properties, где
    
    endpoint: "http://localhost:8811" - адрес подключения к серверу Taskurotta
    threadPoolSize: 10 - размер пула соединений
    readTimeout: 0 - таймаут чтения
    connectTimeout: 3000 - таймаут подключения
                
