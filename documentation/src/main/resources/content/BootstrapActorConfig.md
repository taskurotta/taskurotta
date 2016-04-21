# Настройки поведения актеров


## sleepOnServerError

Свойство позволяет указывать время ожидания потока актера после получения ошибки при взаимодействии с сервером. Это позволяет избежать высокого потребления CPU в случае, когда сервер не доступен, а актеры постоянно пытаются с ним соединиться. Пример:

      - FFWorker:
          actorInterface: ru.taskurotta.test.fullfeature.worker.FullFeatureWorker
          count: 25
          sleepOnServerError: 1 seconds