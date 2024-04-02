
Airflow 를 알아보기전에 먼저 설치해보고 간단하게 탐방한번 해봐야겠다.
먼저 `Docker Container` 기반으로 설치할 예정이므로 `docker-compose.yaml` 파일을 구해보자.

### Fetching docker-compose.yaml

> [Running Airflow in Docker](https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html)

공식 홈페이지에서 `docker-compose.yaml` 파일을 다운로드 받는 경로를 구할 수 있다. 아래 명령어는 `curl` 을 이용해서 다운로드를 하고 있다. 

```shell
$ curl -LfO 'https://airflow.apache.org/docs/apache-airflow/2.8.0/docker-compose.yaml'
```

다운로드를 받았다면 yaml 파일이 어떻게 구성되어있는지 한 번 짚고 넘어가자.

```yaml
x-airflow-common:
  &airflow-common
  image: ${AIRFLOW_IMAGE_NAME:-apache/airflow:2.8.0}
  environment:
    &airflow-common-env
    AIRFLOW__CORE__EXECUTOR: CeleryExecutor
    AIRFLOW__DATABASE__SQL_ALCHEMY_CONN: postgresql+psycopg2://airflow:airflow@postgres/airflow
    AIRFLOW__CELERY__RESULT_BACKEND: db+postgresql://airflow:airflow@postgres/airflow
    AIRFLOW__CELERY__BROKER_URL: redis://:@redis:6379/0
    AIRFLOW__CORE__FERNET_KEY: ''
    AIRFLOW__CORE__DAGS_ARE_PAUSED_AT_CREATION: 'true'
    AIRFLOW__CORE__LOAD_EXAMPLES: 'true'
    AIRFLOW__API__AUTH_BACKENDS: 'airflow.api.auth.backend.basic_auth,airflow.api.auth.backend.session'
    AIRFLOW__SCHEDULER__ENABLE_HEALTH_CHECK: 'true'
    _PIP_ADDITIONAL_REQUIREMENTS: ${_PIP_ADDITIONAL_REQUIREMENTS:-}
  volumes:
    - ${AIRFLOW_PROJ_DIR:-.}/dags:/opt/airflow/dags
    - ${AIRFLOW_PROJ_DIR:-.}/logs:/opt/airflow/logs
    - ${AIRFLOW_PROJ_DIR:-.}/config:/opt/airflow/config
    - ${AIRFLOW_PROJ_DIR:-.}/plugins:/opt/airflow/plugins
  user: "${AIRFLOW_UID:-50000}:0"
  depends_on:
    &airflow-common-depends-on
    redis:
      condition: service_healthy
    postgres:
      condition: service_healthy

services:
  postgres:
    image: postgres:13
    environment:
      POSTGRES_USER: airflow
      POSTGRES_PASSWORD: airflow
      POSTGRES_DB: airflow
    volumes:
      - postgres-db-volume:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "airflow"]
      interval: 10s
      retries: 5
      start_period: 5s
    restart: always

  redis:
    image: redis:latest
    expose:
      - 6379
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 30s
      retries: 50
      start_period: 30s
    restart: always

  airflow-webserver:
    <<: *airflow-common
    command: webserver
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "--fail", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    restart: always
    depends_on:
      <<: *airflow-common-depends-on
      airflow-init:
        condition: service_completed_successfully

  airflow-scheduler:
    <<: *airflow-common
    command: scheduler
    healthcheck:
      test: ["CMD", "curl", "--fail", "http://localhost:8974/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    restart: always
    depends_on:
      <<: *airflow-common-depends-on
      airflow-init:
        condition: service_completed_successfully

  airflow-worker:
    <<: *airflow-common
    command: celery worker
    healthcheck:
      test:
        - "CMD-SHELL"
        - 'celery --app airflow.providers.celery.executors.celery_executor.app inspect ping -d "celery@$${HOSTNAME}" || celery --app airflow.executors.celery_executor.app inspect ping -d "celery@$${HOSTNAME}"'
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    environment:
      <<: *airflow-common-env
      DUMB_INIT_SETSID: "0"
    restart: always
    depends_on:
      <<: *airflow-common-depends-on
      airflow-init:
        condition: service_completed_successfully

  airflow-triggerer:
    <<: *airflow-common
    command: triggerer
    healthcheck:
      test: ["CMD-SHELL", 'airflow jobs check --job-type TriggererJob --hostname "$${HOSTNAME}"']
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    restart: always
    depends_on:
      <<: *airflow-common-depends-on
      airflow-init:
        condition: service_completed_successfully

  airflow-init:
    <<: *airflow-common
    entrypoint: /bin/bash
    command:
      - -c
      - |
        if [[ -z "${AIRFLOW_UID}" ]]; then
          echo
          echo -e "\033[1;33mWARNING!!!: AIRFLOW_UID not set!\e[0m"
          echo "If you are on Linux, you SHOULD follow the instructions below to set "
          echo "AIRFLOW_UID environment variable, otherwise files will be owned by root."
          echo "For other operating systems you can get rid of the warning with manually created .env file:"
          echo "    See: https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html#setting-the-right-airflow-user"
          echo
        fi
        one_meg=1048576
        mem_available=$$(($$(getconf _PHYS_PAGES) * $$(getconf PAGE_SIZE) / one_meg))
        cpus_available=$$(grep -cE 'cpu[0-9]+' /proc/stat)
        disk_available=$$(df / | tail -1 | awk '{print $$4}')
        warning_resources="false"
        if (( mem_available < 4000 )) ; then
          echo
          echo -e "\033[1;33mWARNING!!!: Not enough memory available for Docker.\e[0m"
          echo "At least 4GB of memory required. You have $$(numfmt --to iec $$((mem_available * one_meg)))"
          echo
          warning_resources="true"
        fi
        if (( cpus_available < 2 )); then
          echo
          echo -e "\033[1;33mWARNING!!!: Not enough CPUS available for Docker.\e[0m"
          echo "At least 2 CPUs recommended. You have $${cpus_available}"
          echo
          warning_resources="true"
        fi
        if (( disk_available < one_meg * 10 )); then
          echo
          echo -e "\033[1;33mWARNING!!!: Not enough Disk space available for Docker.\e[0m"
          echo "At least 10 GBs recommended. You have $$(numfmt --to iec $$((disk_available * 1024 )))"
          echo
          warning_resources="true"
        fi
        if [[ $${warning_resources} == "true" ]]; then
          echo
          echo -e "\033[1;33mWARNING!!!: You have not enough resources to run Airflow (see above)!\e[0m"
          echo "Please follow the instructions to increase amount of resources available:"
          echo "   https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html#before-you-begin"
          echo
        fi
        mkdir -p /sources/logs /sources/dags /sources/plugins
        chown -R "${AIRFLOW_UID}:0" /sources/{logs,dags,plugins}
        exec /entrypoint airflow version
    environment:
      <<: *airflow-common-env
      _AIRFLOW_DB_MIGRATE: 'true'
      _AIRFLOW_WWW_USER_CREATE: 'true'
      _AIRFLOW_WWW_USER_USERNAME: ${_AIRFLOW_WWW_USER_USERNAME:-airflow}
      _AIRFLOW_WWW_USER_PASSWORD: ${_AIRFLOW_WWW_USER_PASSWORD:-airflow}
      _PIP_ADDITIONAL_REQUIREMENTS: ''
    user: "0:0"
    volumes:
      - ${AIRFLOW_PROJ_DIR:-.}:/sources

  airflow-cli:
    <<: *airflow-common
    profiles:
      - debug
    environment:
      <<: *airflow-common-env
      CONNECTION_CHECK_MAX_COUNT: "0"
    command:
      - bash
      - -c
      - airflow

  flower:
    <<: *airflow-common
    command: celery flower
    profiles:
      - flower
    ports:
      - "5555:5555"
    healthcheck:
      test: ["CMD", "curl", "--fail", "http://localhost:5555/"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 30s
    restart: always
    depends_on:
      <<: *airflow-common-depends-on
      airflow-init:
        condition: service_completed_successfully

volumes:
  postgres-db-volume:

```

### x-ariflow-common

airflow 에 대한 이미지 정보, 환경변수, 볼륨 정보등 공통으로 사용하는 정보를 정의한다.

```yaml
x-airflow-common:
  &airflow-common
  image: ${AIRFLOW_IMAGE_NAME:-apache/airflow:2.8.0}
  environment:
    &airflow-common-env
    AIRFLOW__CORE__EXECUTOR: CeleryExecutor
    AIRFLOW__DATABASE__SQL_ALCHEMY_CONN: postgresql+psycopg2://airflow:airflow@postgres/airflow
    AIRFLOW__CELERY__RESULT_BACKEND: db+postgresql://airflow:airflow@postgres/airflow
    AIRFLOW__CELERY__BROKER_URL: redis://:@redis:6379/0
    AIRFLOW__CORE__FERNET_KEY: ''
    AIRFLOW__CORE__DAGS_ARE_PAUSED_AT_CREATION: 'true'
    AIRFLOW__CORE__LOAD_EXAMPLES: 'true'
    AIRFLOW__API__AUTH_BACKENDS: 'airflow.api.auth.backend.basic_auth,airflow.api.auth.backend.session'
    AIRFLOW__SCHEDULER__ENABLE_HEALTH_CHECK: 'true'
    _PIP_ADDITIONAL_REQUIREMENTS: ${_PIP_ADDITIONAL_REQUIREMENTS:-}
  volumes:
    - ${AIRFLOW_PROJ_DIR:-.}/dags:/opt/airflow/dags
    - ${AIRFLOW_PROJ_DIR:-.}/logs:/opt/airflow/logs
    - ${AIRFLOW_PROJ_DIR:-.}/config:/opt/airflow/config
    - ${AIRFLOW_PROJ_DIR:-.}/plugins:/opt/airflow/plugins
  user: "${AIRFLOW_UID:-50000}:0"
  depends_on:
    &airflow-common-depends-on
    redis:
      condition: service_healthy
    postgres:
      condition: service_healthy
```

여기서 주의해서 봐야 할 곳은 `image: ${AIRFLOW_IMAGE_NAME:-apache/airflow:2.8.0}`과 
`user: "${AIRFLOW_UID:-50000}:0"` 이다 

- `AIRFLOW_IMAGE_NAME` : Airflow 의 도커 이미지 명이다. 기본으로 `apache/airflow:2.8.0` 이미지를 사용하며, 해당 값을 `.env` 파일에 지정하여 도커 이미지를 바꿀 수 있다.
- `AIRFLOW_UID`: Airflow 의 UID (Unique Identifier) 를 지정하는 것으로 입력하지 않으면 기본적으로 `50000` 이 설정된다. 하지만 실행시 `Warning` 이 발생하므로 입력해주는 것을 권장한다.
- `AIRFLOW__CORE__LOAD_EXAMPLES`: Airflow 에서 제공하는 기본 샘플 들이 있어 학습용으로 보기 좋다. 해당 값을 `false` 로 지정하면 Airflow 시작시 샘플 DAG 들이 나오지 않는다.
### services 

실제 동작해야하는 서비스들이 정의 되어 있으며 여기서 주의해서 확인해 봐야 하는 부분은 `ariflow-init` 이다. 

- `postgres`: airflow 에서 사용하는 기본 데이터 베이스이며, `PostgreSQL` 13 버전 을 사용한다.
- `redis`: airflow 에서 사용하는 `In Memory Database` 이며, `Redis` 를 사용한다. 
- `airflow-webserver`: airflow web 을 제공하기 위한 서비스로 `8080` 포트를 기본 포트로 한다. 웹 브라우저로 사용하는 서비스이므로 `https` 프로토콜을 적용하기 위해선 여기에 적용하면 된다.
- `airflow-scheduler`: 스케쥴러는 `DAG` 나 `Task` 들을 모니터링하며, 의존성이 있는 작업이 완료가 되면 `Task Instance`를 `Trigger` 한다.
- `airflow-worker`: 스케쥴러로부터 전달받은 작업을 실행하는 주체이다. 
- `airflow-triggerer`: 지연 가능한 작업에 대해 이벤트 루프를 실행한다.
- `flower`: 모니터링 Tool 로 실행 결과를 확인할 수 있는 서비스를 제공한다.
- `ariflow-init`: 서비스에 대한 초기화를 진행한다.

#### airflow-init

airflow 의 서비스들을 초기화하기 위한 서비스로 common 에서 사용하는 `${ARIFLOW_UID}`를 여기서 있는지 체크한다. 

이때 `airflow webserver` 의 기본 사용자 계정과 패스워드를 지정해줄 수 있다.

```yaml
services:
  airflow-init:
    <<: *airflow-common
    entrypoint: /bin/bash
    # yamllint disable rule:line-length
    command:
      - -c
      - |
        if [[ -z "${AIRFLOW_UID}" ]]; then
          echo
          echo -e "\033[1;33mWARNING!!!: AIRFLOW_UID not set!\e[0m"
          echo "If you are on Linux, you SHOULD follow the instructions below to set "
          echo "AIRFLOW_UID environment variable, otherwise files will be owned by root."
          echo "For other operating systems you can get rid of the warning with manually created .env file:"
          echo "    See: https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html#setting-the-right-airflow-user"
          echo
        fi
        one_meg=1048576
        mem_available=$$(($$(getconf _PHYS_PAGES) * $$(getconf PAGE_SIZE) / one_meg))
        cpus_available=$$(grep -cE 'cpu[0-9]+' /proc/stat)
        disk_available=$$(df / | tail -1 | awk '{print $$4}')
        warning_resources="false"
        if (( mem_available < 4000 )) ; then
          echo
          echo -e "\033[1;33mWARNING!!!: Not enough memory available for Docker.\e[0m"
          echo "At least 4GB of memory required. You have $$(numfmt --to iec $$((mem_available * one_meg)))"
          echo
          warning_resources="true"
        fi
        if (( cpus_available < 2 )); then
          echo
          echo -e "\033[1;33mWARNING!!!: Not enough CPUS available for Docker.\e[0m"
          echo "At least 2 CPUs recommended. You have $${cpus_available}"
          echo
          warning_resources="true"
        fi
        if (( disk_available < one_meg * 10 )); then
          echo
          echo -e "\033[1;33mWARNING!!!: Not enough Disk space available for Docker.\e[0m"
          echo "At least 10 GBs recommended. You have $$(numfmt --to iec $$((disk_available * 1024 )))"
          echo
          warning_resources="true"
        fi
        if [[ $${warning_resources} == "true" ]]; then
          echo
          echo -e "\033[1;33mWARNING!!!: You have not enough resources to run Airflow (see above)!\e[0m"
          echo "Please follow the instructions to increase amount of resources available:"
          echo "   https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html#before-you-begin"
          echo
        fi
        mkdir -p /sources/logs /sources/dags /sources/plugins
        chown -R "${AIRFLOW_UID}:0" /sources/{logs,dags,plugins}
        exec /entrypoint airflow version
    # yamllint enable rule:line-length
    environment:
      <<: *airflow-common-env
      _AIRFLOW_DB_MIGRATE: 'true'
      _AIRFLOW_WWW_USER_CREATE: 'true'
      _AIRFLOW_WWW_USER_USERNAME: ${_AIRFLOW_WWW_USER_USERNAME:-airflow}
      _AIRFLOW_WWW_USER_PASSWORD: ${_AIRFLOW_WWW_USER_PASSWORD:-airflow}
      _PIP_ADDITIONAL_REQUIREMENTS: ''
    user: "0:0"
    volumes:
      - ${AIRFLOW_PROJ_DIR:-.}:/sources

```


`AIRFLOW_UID`, `_AIRFLOW_WWW_USER_USERNAME`, `_AIRFLOW_WWW_USER_PASSWORD` 환경변수 값을 이용해서 정의할 수 있으며 이 값들은 `.env` 파일에 정의하여 사용하면 된다.

```shell
$ # 사용자의 ID 값을 AIRFLOW_UID 값으로 하여 .env 파일을 생성한다.
$ # 이 때 .env 의 경로는 docker-compose.yaml 과 동일한 경로에 있어야 한다.
$ echo -e "AIRFLOW_UID=$(id -u)" > .env 

$ # 사용자 ID 와, Password 를 변경하기 위해 아래와 같은 방법을을 사용하여 .env 파일에 내용을 추가한다.
$ vim .env
AIRFLOW_UID=100
_AIRFLOW_WWW_USER_USERNAME=admin
_AIRFLOW_WWW_USER_PASSWORD=admin

$ # 다른 방법
$ echo '''_AIRFLOW_WWW_USER_USERNAME=admin
> _AIRFLOW_WWW_USER_PASSWROD=admin''' >> .env
```

참고로 기본 사용자 아이디 패스워드는 `airflow/airflow`이다.

### 사전작업

airflow 를 실행하기 전 먼저 디렉터리를 생성하고 시작하자. 물론 서비스를 실행할 때 디렉터리를 생성해주지만 그냥 먼저 생성하고 어떤 역할이 있는지 정리하겠다.

```shell
$ mkdir -p ./dags ./logs ./plugins ./config
```

- `dags`: 여기에 DAG 파일들을 넣을 수 있다.
- `logs`: 작업 실행과 스케쥴에 대하여 저장한다.
- `config`: custom log paser 또는 `airflow_local_settings.py` 에 클러스터 정책을 구성할 수 있다.
- `plugins`: 커스텀 플러그인을 여기에 작성할 수 있다.

> [Custom Plugins](https://airflow.apache.org/docs/apache-airflow/stable/authoring-and-scheduling/plugins.html)

### Install 

```shell
$ mkdir -p ./dags ./logs ./plugins ./config
$ echo -e "AIRFLOW_UID=$(id -u)" > .env
$ docker-compose up airflow-init
[+] Building 0.0s (0/0)                                       docker:default
[+] Running 5/0
 ✔ Network airflow_default              Created                         0.0s 
 ✔ Volume "airflow_postgres-db-volume"  Created                         0.0s 
 ✔ Container airflow-redis-1            Created                         0.0s 
 ✔ Container airflow-postgres-1         Created                         0.0s 
 ✔ Container airflow-airflow-init-1     Created                         0.0s 
Attaching to airflow-airflow-init-1

...

airflow-airflow-init-1  | User "airflow" created with role "Admin"
airflow-airflow-init-1  | 2.8.0
airflow-airflow-init-1 exited with code 0
```

마지막에 이러한 메시지로 끝난다면 정상적으로 된 것이다. (물론 이미 airflow 사용자가 있는 경우 다른 메시지가 날 것이다.)

```shell
$ docker-compose up -d
airflow-airflow-triggerer-1  |   ____________       _____________
airflow-airflow-triggerer-1  |  ____    |__( )_________  __/__  /________      __
airflow-airflow-triggerer-1  | ____  /| |_  /__  ___/_  /_ __  /_  __ \_ | /| / /
airflow-airflow-triggerer-1  | ___  ___ |  / _  /   _  __/ _  / / /_/ /_ |/ |/ /
airflow-airflow-triggerer-1  |  _/_/  |_/_/  /_/    /_/    /_/  \____/____/|__/
airflow-airflow-triggerer-1  | [2023-12-21 04:55:12 +0000] [16] [INFO] Starting gunicorn 21.2.0
airflow-airflow-triggerer-1  | [2023-12-21 04:55:12 +0000] [16] [INFO] Listening at: http://[::]:8794 (16)
airflow-airflow-triggerer-1  | [2023-12-21 04:55:12 +0000] [16] [INFO] Using worker: sync
airflow-airflow-triggerer-1  | [2023-12-21 04:55:12 +0000] [17] [INFO] Booting worker with pid: 17
airflow-airflow-triggerer-1  | [2023-12-21 04:55:12 +0000] [18] [INFO] Booting worker with pid: 18

```

이렇게 시작하면 정상적으로 실행 되었다고 할 수 있다.

브라우저에서 대상 서버로 접속해보자 (나는 여기서 `192.168.0.181` 서버에 구성해 놨다)

http://192.168.0.181:8080

접속하면 아래와 같은 로그인 화면이 보이는 것을 확인할 수 있다.

![airflow_login](./img/01_installing_with_docker/airflow_login.png)

초기 로그인 정보는 `airflow/airflow` 이다

다음장부터 Airflow 가 무엇을 하는지 자세히 알아보자

> **참고**: [Running Airflow with CLI Commands](https://airflow.apache.org/docs/apache-airflow/stable/howto/docker-compose/index.html#running-the-cli-commands:~:text=Accessing%20the%20environment-,Running%20the%20CLI%20commands,-Accessing%20the%20web)

### 주의사항

나는 Ubuntu 20.04 위에 Docker 를 설치하여 실행하였다. 때문에 Python3 이 기본으로 설치돼있어 그냥 사용하였지만 conda 를 이용한 python 가상화하여 DAG 를 작성하는것을 권장한다. 이유는 알거라 생각하겠다.

나는 vscode 를 사용해서 원격으로 접속해 작성할 예정이라 아래 airflow python framework 을 설치했다.
```shell 
$ pip install "apache-airflow[celery]==2.8.0" --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-2.8.0/constraints-3.8.txt"
```

> **참고** : [Airflow Python Framework](https://airflow.apache.org/docs/apache-airflow/stable/installation/installing-from-pypi.html)
> 