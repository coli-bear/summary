
Docker, docker-compose 를 이용해서 database 를 생성하였다. 

먼저 파일을 생성하고 `vim` 을 이용해 아래 내용을 작성하였다.

> 참고 : 윈도우 사용자는 메모장을 이용해서 작성하고 docker-compose 를 실행해도 되지만 나는 유닉스 계열의 OS 를 사용하고 있어 vim 을 이용해서 편집하였다. Mac 사용자는 vscode 같은 에디터를 이용해서 사용하면 편하게 사용할 수 있다.

```shell
$ touch docker-compose.yml
$ vim docker-compose.yml
```

아래는 maria database 를 구동하기 위한 `docker-compose.yml` 의 내용이다.

```yaml
version: 3.3

services:
	mariadb:
		image: mariadb:latest
		hostname: mariadb
		container_name: mariadb
		environment: 
			MARIADB_ROOT_PASSWORD: P@ssword123 # 필수 값 
			MARIADB_USER: sample # 선택 : 일반 계정을 생성한다.
			MARIADB_PASSWORD: P@ssword123 # 선택 : 일반 계정의 패스워드
			MARIADB_DATABASE: sample # 선택 : 기본적으로 database 를 하나 생성할 수 있다.
		ports:
			- 3306:3306
		volumes:
			- type: volume
			  source: maridb_vol
			  target: /var/lib/mysql
			  
volumes:
	mariadb_vol:
		
```

환경변수로 지정한 것 왜에 자세한 것을 알고 싶으면 `docker-hub` 에 방문하여 자세한 옵션들을 확인할 수 있다.

아래는 `docker-compose` 를 이용한 실행 방법이다.

```shell
$ # docker-copomse 를 이용한 실행 (docker-compose.yml 이 있는 경로 에서 실행)
$ docker-compose up -d # -d 옵션은 백그라운드에서 실행하도록 하는 옵션이다.
$ docker-compose log mariadb # maridb 는 container name 으로 생략 가능
```

아래는 docker-compose 를 이용하지 않고 사용하는 법이다. 알아만 두자.

```shell
$ docker volume create --name mariadb_vol
$ docker run -p 3306:3306 \ 
-e MARIADB_ROOT_PASSWORD=P@ssword123
-e MARIADB_USER=sample \
-e MARIADB_PASSWORD=P@ssword123 \
-e MARIADB_DATABASE=sample
-v mariadb_vol:/var/lib/mysql
```

여기서 작성한 docker 명령은 위에서 정의한 `docker-compose.yml` 과 동일한 결과를 기대할 수 있다.

