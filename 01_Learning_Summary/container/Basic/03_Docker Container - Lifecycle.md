## 컨테이너 라이프 사이클

![Untitled](lifecycle1.png)

- 복잡한 버전

![Untitled](lifecycle2.png)

## 컨테이너 시작

- create/run 명령어 모두 이미지가 없는 경우 자동으로 pull을 먼저 수행하여 이미지를 다운로드 받음

### 컨테이너 생성 시작

```bash
docker run [image]
```

### 컨테이너 생성

```bash
docker create [image]
```

## 컨테이너 시작

```bash
docker start [container]
```

### 실행중 컨테이너 목록

```bash
docker ps
```

### 전체 컨테이너 목록

```bash
docker ps -a
```

### 컨테이너 시작 주요 옵션

![Untitled](container-options.png)

|옵션|설명|
|---|---|
|-i|-t 와 한쌍 (보통 같이 사용) 호스트의 입력이 컨테이너에 전달|
|-t|tty를 할당하여 터미널 명령어 수행이 정상적으로 되도록 하는 옵션|
||- 위 두 옵션은 docker container 에 shell 실행하기 위한 명령어|
|—rm|컨테이너 샐행 후 자동으로 삭제|
|-d|백그라운드에서 daemon 형태로 실행|
|-name [name]|컨테이너의 이름을 지정 (지정하지 않으면 도커엔진에서 자동으로 부여)|
|-p|호스트와 컨테이너간 네트워크 포트 바인딩|
|-v|호스트와 컨테이너간 파일시스템(볼륨) 바인딩|

## 간단한 실습

### ubuntu 컨테이너 실행

```bash
docker run ubuntu:focal

Unable to find image 'ubuntu:focal' locally
focal: Pulling from library/ubuntu
d5fd17ec1767: Pull complete 
Digest: sha256:47f14534bda344d9fe6ffd6effb95eefe579f4be0d508b7445cf77f61a0e5724
Status: Downloaded newer image for ubuntu:focal
```

### 컨테이너 확인

```bash
docker ps
CONTAINER  ID  IMAGE  COMMAND  CREATED  STATUS  PORTS  NAMES

docker ps -a
CONTAINER ID   IMAGE         COMMAND  CREATED              STATUS                          PORTS  NAMES
3042e6cd7458   ubuntu:focal  "bash"   About a minute ago   Exited (0) About a minute ago
```

- 종료된 이유는 bash가 기본적으로 표준 입력을 필요로 하는 어플리케이션이기 때문에 -i와 -t 를 이용하여 접근해야 함

```bash
docker run -i -t ubuntu:pocal

# 종료하지 않고 나오는 법
# 도커 배쉬에서
# ctrl + p, q
```

## 컨테이너 상태 확인

![Untitled](container-status.png)

- 컨테이너에 문제가 발생하면 inspect 를 사용해서 확인 가능

### 컨테이너 종료

![Untitled](container-kill.png)

- kill 옵션으로 종료하면 강제 종료 되기 때문에 로그가 남지 않음

#### docker container id 목록 조회

```bash
docker ps -a -q

31d83100ca95
9f6936d65360
42dbb13bd9b5
3042e6cd7458
22659b290e8c
4107a1bad5df
98ec2db7a370
```

### 컨테이너 삭제

![Untitled](container-remove.png)

#docker 