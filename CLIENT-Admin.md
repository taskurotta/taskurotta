# Запуск JVM с клиентски кодом
Для запуска JVM c клиентским кодом необходимо выполнить команду или создать исполняемый файл со следующим содержимым
    
    <путь до java> [дополнительные параметры] -jar <имя jar арихва>.jar -f <путь до конфигурационного файла>
        
Например:

    /opt/jdk1.7.0_40/bin/java -Djava.library.path=/opt/DIGT -jar ru.rr.crypto.signature.impl-1.0-SNAPSHOT.jar -f ru-rr-crypto-signature-mnbs1.yml
    
# Внесение  