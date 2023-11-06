## 엔트리 포인트와 커맨드

### entrypoint

- 도커 컨테이너가 실행할 때 고정적으로 실행되는 스크립트 혹은 명령어
- 생략 가능하며, 생략될 경우 커맨드에 지정된 명령어로 수행

### command

- 도커 컨테이너가 실행할때 수행할 명령어 혹은 엔트리포인트에 지정된 명령어에 대한 인자 값

## Dockerfile의 엔트리 포인트와 커맨드

```docker
FROM node:12-alpine
RUN apk add --no-cache python3 g++ make
WORKDIR /app
COPY . .
RUN yarn install --production

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["node"]
```

- 위 파일의 엔트리 포인트와 cmd 는 아래 명령어로 실행

```docker
docker-entrypoint.sh node
```

## 도커 명령어의 엔트리 포인트

```bash
docker run --entrypoint sh ubuntu:focal
docker run --entrypoint echo ubuntu:focal hello world
```

#docker 