## Environment
|구분|버전|
|---|---|
|OS|Ubuntu 20.04|
|Docker|latest(작성일 기준 :  24.0.7)|
|Docker-Compose|v2.23.0|

## Docker Install

### package update & http package install

```shell
$ sudo apt-get update
$ sudo apt-get install ca-certificates curl gnupg lsb-release
```

### GPG Ket & Store 추가
```shell 
$ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
```

### Repository 등록
```shell
$ add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
```


### Docker 설치 (최신 버전)
```shell
$ suto apt-get update
$ sudo apt-get install docker-ce docker-ce-cli containerd.io
```
### Docker 설치 (특정 버전)

```shell
$ sudo apt install docker-ce=5:20.10.14~3-0~ubuntu-focal docker-ce-cli=5:20.10.14~3-0~ubuntu-focal containerd.io
```

### 실행 권한 부여

```shell
$ groups # group 목록 확인
$ id # account 정보 확인 일반사용자로 설치하는 경우(예: sysmgr) 권한을 확인하여 docker 그룹 권한을 추가해줘야 함.
$ usermod -aG docker
```

### Docker 버전 확인

```shell
docker --version
```

## Docker-Compose install

```shell
$ sudo curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

$ sudo chmod +x /usr/local/bin/docker-compose
$ docker-compose -v
Docker Compose version v2.23.0
```


#docker
