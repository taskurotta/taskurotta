# Оптимизация взаимодействия актеров с сервером

В классической архитектуре клиенты общаются с серверами кластера через балансировщик нагрузки. Этот подход, однако, имеет недостатки когда кластер хранит информацию распределенно, так как запрос может попасть на произвольный узел кластера, который может не обладать нужными данными. В этом случае он производит дополнительный запрос к другому узлу - владельцу информации, что дает дополнительные расходы и снижает производительность. Для решение этой проблемы можно включить оптимизацию взаимодействия актеров с кластером.
 
Данная оптимизация сейчас заключается в двух аспектах. Во-первых, вместе с результатом (или его отсутствием) REST сервиса poll в заголовках передается URL узла кластера, который является владельцем очереди. Это позволяет умному актеру при следующих обращениях ходить напрямую на нужный сервер за задачами, в отличии от первого похода в слепую - через балансировщик нагрузки. Во-вторых, если получена задача, то вместе с ней в заголовках ответа передается URL узла кластера, владельца всех данных о процессе которому принадлежит задача. Это так же позволит оптимизировать действия по сохранению и обработки результата сервером. Не будет осуществляться перенаправление результата на нужный узел кластера и его обработка запустится тут-же синхронно, дополнительно снижая нагрузку на систему в целом. По предварительным тестам скорость выполнения процессов возрастает до 20%.

Но есть ситуации когда данный подход сложно реализуем или не будет работать. Например если актеры работают на физических серверах а суслик в докер контейнерах. Без приседания с бубном суслику не понять по каким адресам докер хостов торчит наружу каждый узел кластера или какой внешний ip адрес ему назначен. Посему в таких гибридных средах фича не поддерживается пока и по умолчанию выключена.

Включить фичу можно с помощью свойства hz.partition-routing.enable: true в yaml конфигурации сервера. Там-же можно управлять портом узлов кластера hz.partition-routing.port: 8811 . По умолчанию порт 8811. Порт в текущей реализации нужен потому, что сам hazelcast ничего не знает про REST сервисы суслика и может сообщить только имя хоста владельца очереди или процесса. Остальное в этой первой реализации фичи мы достраиваем исходя из предположения что все порты узлов кластера одинаковые. Это же теперь является и ограничением для применения фичи.

Актеры должны быть собраны с версией не ниже 12 иначе они не будут пользоваться информацией в заголовках ответа REST сервиса. При создании пула соединений к каждому из узлов кластера используются такие же настройки как к пулу соединений к балансировщику нагрузки.