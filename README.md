# distributed-tracing
OpenTelemetryとCloud Traceを使ったPub/Sub経由のトレース計装のサンプル

## How to Use
事前にGoogle Cloudのプロジェクトを作成し、Pub/Subのtopicとsubscriptionを作成すること


.env.exampleを元に.envをコピーして作成する
```
cp .env.example .env
```
.envの環境変数を設定する

publisherの.env.exampleを元に.envをコピーして作成する
```
cd publisher
cp .env.example .env
```
.envの環境変数を設定する

subscriberの.env.exampleを元に.envをコピーして作成する
```
cd subscriber
cp .env.example .env
```
.envの環境変数を設定する

collector-contribのconfig.yaml.exampleを元に、config.yamlをコピーして作成する
```
cd collector-contrib
cp config.yaml.example config.yaml
```
config.yamlのプロジェクトIDの値を設定する

credentialsの中に、今回使用するプロジェクトにアクセス可能な`application_default_credentials.json`ファイルをコピーする
```
cd collector-contrib
cp /path/to/application_default_credentials.json credentials/application_default_credentials.json
```

collector-contribを起動
```
cd collector-contrib
docker-compose up -d
```

publisherをjavaagentで`opentelemetry-javaagent.jar`を渡して起動

subscriberをjavaagentで`opentelemetry-javaagent.jar`を渡して起動

publisherに対して、以下のリクエストを行い、Cloud Traceでトレース伝搬が行われていれば成功
```
curl -X POST -d "message=your_message_here" http://localhost:8080/publish
```
