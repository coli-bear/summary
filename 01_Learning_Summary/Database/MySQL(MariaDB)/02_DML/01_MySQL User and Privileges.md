
먼저 Database 는 [MariaDB with docker](../01_install/MariaDB with docker.md) 를 이용해 설치하였다.

`dbeaver` 나 `data grip` 같은 툴을 이용해서 쉽게 할 수 있겠지만 타자로 처보는게 더 기억에 잘 남아 Shell 에서 작업했다.

## 사용자 관리
### 사용자 확인
`use mysql` 명령어를 입력하여 mysql database 로 이동한다. 

```sql
MariaDB [(none)]> use mysql 
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed
```

사용자와 관련된 테이블들을 확인해보자.
```sql
MariaDB [mysql]> show tables;
+---------------------------+
| Tables_in_mysql           |
+---------------------------+
| column_stats              |
| columns_priv              |
| db                        |
| event                     |
| func                      |
| general_log               |
| global_priv               |
| gtid_slave_pos            |
| help_category             |
| help_keyword              |
| help_relation             |
| help_topic                |
| index_stats               |
| innodb_index_stats        |
| innodb_table_stats        |
| plugin                    |
| proc                      |
| procs_priv                |
| proxies_priv              |
| roles_mapping             |
| servers                   |
| slow_log                  |
| table_stats               |
| tables_priv               |
| time_zone                 |
| time_zone_leap_second     |
| time_zone_name            |
| time_zone_transition      |
| time_zone_transition_type |
| transaction_registry      |
| user                      |
+---------------------------+
31 rows in set (0.000 sec)
```

많은 테이블 들이 존재하지만 일단 `global_priv` 와 `user` 만 알아두자

- global_priv : mariadb 10.4 이후에 user table 을 대신하여 사용자 관리 역할을 한다.
```sql
MariaDB [mysql]> select user, host from global_priv;
+-------------+-----------------------+
| user        | host                  |
+-------------+-----------------------+
| root        | %                     |
| healthcheck | 127.0.0.1             |
| healthcheck | ::1                   |
| healthcheck | localhost             |
| mariadb.sys | localhost             |
| root        | localhost             |
+-------------+-----------------------+
6 rows in set (0.000 sec)
```

- user : mariadb 10.4 이전에 사용하던 사용자 관리 테이블 10.4 이후로는 view 테이블로 변경되었으며, `global_priv` 테이블이 해당 역할을 대신하게 됐다.
> https://mariadb.com/kb/en/mysql-user-table/
```sql
MariaDB [mysql]> select user, host from user;
+-------------+-----------------------+
| User        | Host                  |
+-------------+-----------------------+
| root        | %                     |
| healthcheck | 127.0.0.1             |
| healthcheck | ::1                   |
| healthcheck | localhost             |
| mariadb.sys | localhost             |
| root        | localhost             |
+-------------+-----------------------+
6 rows in set (0.001 sec)
```

### 사용자 추가 
이제 사용자를 추가해보자

형식은 아래와 같은 형식으로 구성되어 있다. 

```sql 
MariaDB [mysql] create user '{USER ID}'@'{HOST}' identified by '{PASSWORD}';
```

|필드|설명|
|---|---|
| {USER_ID} | 사용자의 ID 이다 |
| {HOST} | 접근 가능한 source 호스트로 출발지의 IP 또는 Domain 을 입력 받는다 |
| {PASSWORD} | 로그인을 위한 비밀번호이다 |

이렇게 sample 게정을 생성해봤다.
```sql
MariaDB [mysql]> create user 'sample'@'%' identified by 'P@ssword123';
```
여기서 호스트 부분에 `%` 는 전체를 의미한다. (모든 source 호스트)

다시한번 `user` 뷰 테이블을 조회하면 사용자가 생서오디어 있는것을 확인할 수 있다.
```sql
MariaDB [mysql]> select user, host from user;
+-------------+-----------------------+
| User        | Host                  |
+-------------+-----------------------+
| ...         | ...                   |
| sample      | %                     |
| ...         | ...                   |
+-------------+-----------------------+
7 rows in set (0.002 sec)
```

### 사용자 패스워드 변경
사용자를 등록했으니 패스워드 변경해보자. 
사용자의 패스워드를 변경할 때에는 `alter` 을 이용해서 변경할 수 있다. 아래는 구성 형식이다.

```sql
MariaDB [mysql]> alter user '{USER_ID}'@'{HOST}' identified by '{NEW_PASSWORD}';
```

|필드|설명|
|---|---|
| {USER_ID} | 변경할 사용자의 ID 이다 |
| {HOST} | 접근 가능한 source 호스트로 출발지의 IP 또는 Domain 을 입력 받는다  |
| {PASSWORD} | 로그인을 위한 새로운 비밀번호이다 |

여기서 주의해야 할 점은 `USER_ID` 와 `HOST` 는 사용자 생성시 입력한 정보를 동일하게 입력해 주어야 한다는 것이다. 이것을 유의해서 아까 생성한 sample 의 패스워드를 변경해봤다.
```sql 
MariaDB [mysql]> alter user 'sample'@'%' identified by 'Qwer1234!;
```

### 사용자 삭제 
사용자 삭제 하는 법은 아래 와 같다. 
```sql
MariaDB [(none)]> drop user 'sample'@'%';
```

여기서도 패스워드 변경과 동일하게 사용자의 정보는 생성시 입력한 정보와 동일하게 작성해야 한다는 점이다. 

```sql 
MariaDB [mysql]> select user, host from user;
+-------------+-----------------------+
| User        | Host                  |
+-------------+-----------------------+
| root        | %                     |
| healthcheck | 127.0.0.1             |
| healthcheck | ::1                   |
| healthcheck | localhost             |
| mariadb.sys | localhost             |
| root        | localhost             |
+-------------+-----------------------+
6 rows in set (0.002 sec)
```
삭제 후 사용자 조회하면 목록에서 제거된 것을 확인할 수 있다.
