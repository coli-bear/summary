## 도커 환경변수 주입

- -e 옵션을 활용하여 주입

```bash
docker run -i -t -e MY_HOST=fastcampus.com ubuntu:focal bash

env 
MY_HOST=fastcampust.com
```

## 도커 환경변수 파일 주입

- 운영환경에서 좋음
- —env-file 옵션을 활용하여 주입

### env 파일 생성

```bash
cat > ~/sample.env
MY_HOST=helloworld.com
MY_VAR=123
MY_VAR2=456
```

### env 파일 주입

```bash
docker run -i -t --env-file ~/sample.env ubuntu:focal env

...
MY_HOST=helloworld.com
MY_VAR=123
MY_VAR2=456
...
```

## docker exec

- 실행중인 컨테이너에 명령어를 실행

```bash
docker exec [container] [command]

# my-nginx 컨테이너에 bash shell 접속
docker exec -i -t my-nginx bash
docker exec -it my-nginx bash

# my-nginx 내의 환경변수 조회
docker exec my-nginx env
```

#docker 