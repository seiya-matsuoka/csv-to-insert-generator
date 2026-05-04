# syntax=docker/dockerfile:1

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# 依存関係の取得をキャッシュしやすくするため、先にpom.xmlだけコピーする
COPY pom.xml .

RUN mvn -q -DskipTests dependency:go-offline

# ソースコードをコピーしてビルドする
COPY src ./src

# テストはローカルで確認済み前提。Dockerビルドでは速度優先でskipする
# dependency:copy-dependencies で実行時に必要なjarを target/dependency に集める
RUN mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime

# ---- runtime stage ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# RenderのWeb Serviceのデフォルトポートに合わせる
# ローカルでは -p 8080:10000 で確認する
ENV PORT=10000

COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency

EXPOSE 10000

CMD ["sh", "-c", "java -cp 'classes:dependency/*' io.github.seiya_matsuoka.csv_to_insert_generator.app.Main"]
