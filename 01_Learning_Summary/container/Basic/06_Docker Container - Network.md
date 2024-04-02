## 도커 네트워크 구조

![Untitled](networks-archi.png)

- 도커 컨테이너 생성시 사용할 기본 네트워크 기정 가능
- 지정하지 않으면 docker0 이라는 브릿지 네트워크 사용
- 컨테이너 내부에서 확인해보면 eth0과 lo가 존재
- docker0이 veth 와 eth 간 다리 역할을 함
- veth 는 컨테이너의 개수만큼 생성 됨

![Untitled](networks-command.png)

## 컨테이너 포트 노출

- 컨테이너 포트를 호스트의 IP:PORT와 연결하여 서비스를 노출

```bash
docker run -p [HOST IP:PORT]:[CONTAINER PORT] [container]

# nginx 컨테이너의 80 포트를 호스트 모든 IP의 80 포트와 연결하여 실행
docker run -d -p 80:80 nginx

# nginx 컨테이너의 80 포트를 호스트 127.0.0.1 IP의 80번 포트와 연결하여 실행
docker run -d -p 127.0.0.1:80:80 nginx

# nginx 컨테이너의 80 포트를 호스트의 사용 가능한 포트와 연결하여 실행 (?)
docker run -d -p 80 nginx
```

## Expose vs Publish

```bash
# expose 옵션은 그저 문서화 용도
docker run -d --expose 80 nginx

# publish 옵션은 실제 포트를 바인딩
docker run -d -p 80 nginx
```

### Expose

```bash
# 실제 80 포트로 보내도 동작하지 않음
curl localhost:80

curl: (7) Failed to connect to localhost port 80 after 0 ms: Connection refused
```

### 도커 네트워크 드라이버

### 네트워크 드라이버 분류

![Untitled](networks-driver.png)

### 동작 방식 별 분류

![Untitled](networks-active.png)

- bridge
    - docker0 : 기본적으로 생성
    - user defined : 사용자가 네트워크 드라이버를 선택하여 새로운 bridge 네트워크 생성가능
- overlay : 각각의 서버에 있는 컨테이너를 연결하는 가상의 네트워크
    - 오케스트레이션 시스템에서 사용
    - docker swarm 을 이용

### 도커의 네트워크 관리를 위한 옵션

```bash
docker network [command]
```

## 실습

### none network

```bash
docker run -i -t --net none ubuntu:focal
```

- IP 정보와 Driver 옵션이 null로 설정 되어 있음
- 해당 도커가 네트워크 기능이 필요 없거나 커스텀 네트워킹이 필요한 경우 사용

### host network

```bash
docker run -d --network=host grafana/grafana
```

- 도커가 제공하는 가상네트워크를 사용하는것이 아니라 직접 호스트 네트워크에 붙어서 사용
- 포트바인딩을 사용하지 않더라도 바로 접근이 가능
- 그라파나는 기본적으로 3000 사용
- IP가 별도로 지정되어있지 않고, 호스트의 호스트 명을 사용

### bridge network

```bash
# bridge network 생성
docker network create --driver=bridge fastcampus

docker run -d --network=fastcampus --net-alias=hello nginx
docker run -d --network=fastcampus --net-alias=grafana grafana/grafana
```

- 사용자 정의 bridge 네트워크 생성
- —network : 생성한 bridge 네트워크와 연결
- —net-alias :
    - bridge network에서만 사용 가능
    - 내부 도메인에 등록하여 컨테이너 ip를 search 가능하도록 함

#docker 