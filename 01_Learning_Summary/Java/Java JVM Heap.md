#JVM #JAVA #HeapMemory

회사에서 갑작스레 OOM(Out Of Memory) 발생했다는 메일 받아서 정리한다.

### 환경 정보

Host OS 아래에 Docker Container 기반의 운영을 하고 있다.

|구분|버전|
|---|---|
|Host OS|Ubuntu 20.04|
|Docker|24.0.6 |
|Guest Os|Ubuntu 22.03|
|Java|OpenJDK 11.0.20.1|
|Spring Boot|2.7.12|
|Docker Compose|v2.23.0|

먼저 heap size 를 확인하자. 


### JVM Heap Default 확인

``` shell 
$ java -XX:+PrintFlagsFinal -version | grep -iE 'HeapSize|PermSize|ThreadStackSize'
     intx CompilerThreadStackSize                  = 1024                                   {pd product} {default}
   size_t ErgoHeapSizeLimit                        = 0                                         {product} {default}
   size_t HeapSizePerGCThread                      = 43620760                                  {product} {default}
   size_t InitialHeapSize                          = 130023424                                 {product} {ergonomic}
   size_t LargePageHeapSizeThreshold               = 134217728                                 {product} {default}
   size_t MaxHeapSize                              = 2080374784                                {product} {ergonomic}
    uintx NonNMethodCodeHeapSize                   = 5830732                                {pd product} {ergonomic}
    uintx NonProfiledCodeHeapSize                  = 122913754                              {pd product} {ergonomic}
    uintx ProfiledCodeHeapSize                     = 122913754                              {pd product} {ergonomic}
   size_t ShenandoahSoftMaxHeapSize                = 0                                      {manageable} {default}
     intx ThreadStackSize                          = 1024                                   {pd product} {default}
     intx VMThreadStackSize                        = 1024                                   {pd product} {default}

```

#### 현재 할당된 HeapSize

- InitialHeapSize => 130023424 Byte(124MB)
- MaxHeapSize     => 2080374784 Byte(1984MB)
- ThreadStackSize => 1024 KB (1MB)

#### JVM 에서 기본적으로 HeapSize를 할당하는 방식
##### Ergonomics Algorithm

JVM 에서 heap memory size 의 기본값을 설정하기 위한 알고리즘

- Initial heap size = Memory/64 = InitialHeapSize
- Maximum heap size = Memory/4  = MaxHeapSize

``` shell
# 현재 메모리 
$ cat /proc/meminfo | grep MemTotal
# MemTotal:        8125192 kB

```

알고리즘에 따라 계산한 값은 아래와 같으며, 먼저 조회한 결과와 근사치이다.

- InitialHeapSize : 7.7Gi/64  = 130003072 Byte (약 124 MiB)
- MaxHeapSize     : 7.7GI/4   = 2080049152 Byte (약 2 GiB)


### Spring Boot Embedded Tomcat JVM 

Spring Boot Embedded Tomcat HeapSize의 기본값은 JVM Default HeapSize 할당하는 `Ergonomics Algorithm` 을 따른다.

그러면 OOM 의 원인 분석을 위해 현재 사용중인 Heap Memory 를 확인해보자

#### 실행중인 JVM Heap Memory 확인

```shell
$ ps -ef | grep java
# root    1  0  7 Oct25 ?   01:06:12 java -jar  sample.jar
```

별도의 설정을 확인해주지 않아 위에 정리한 기본 HeapSize 를 따른다.

##### jps

`jps` 를 이용해서 현재 실행되고 있는 JVM 프로세스를 확인한다.

- `-v` 옵션을 사용하면 프로세스 실행 시 전달 된 옵션 정보도 같이 보여준다. 

```shell
$ jps -v
# 1     sample.jar 
```

##### jmap/jhsdb

`jmap`는 jvm 의 맵을 보여주는 기능을 제공하는 기본 분석도구이다. 하지만 Open JDK 에서는 jmap 대신 jhsdb 를 사용해서 분석해야 한다.

```shell
# jmap
# jmap -heap {PID}
$ jmap -heap 1

# jhsdb
$ jhsdb jmap
    <no option>             To print same info as Solaris pmap.
    --heap                  To print java heap summary.
    --binaryheap            To dump java heap in hprof binary format.
    --dumpfile <name>       The name of the dump file.
    --histo                 To print histogram of java object heap.
    --clstats               To print class loader statistics.
    --finalizerinfo         To print information on objects awaiting finalization.
    --pid <pid>             To attach to and operate on the given live process.
    --core <corefile>       To operate on the given core file.
    --exe <executable for corefile>
    --connect [<id>@]<host> To connect to a remote debug server (debugd).

    The --core and --exe options must be set together to give the core
    file, and associated executable, to operate on. They can use
    absolute or relative paths.
    The --pid option can be set to operate on a live process.
    The --connect option can be set to connect to a debug server (debugd).
    --core, --pid, and --connect are mutually exclusive.

    Examples: jhsdb jmap --pid 1234
          or  jhsdb jmap --core ./core.1234 --exe ./myexe
          or  jhsdb jmap --connect debugserver
          or  jhsdb jmap --connect id@debugserver
         
```

---
**[주의사항]**
docker 환경에서 힙 모니터링을 위해서는 아래 옵션을 활성화 해야한다.

```shell 
docker run -d `--cap-add=SYS_PTRACE` {image}
```

나는 docker-compose 를 이용해서 하므로 docker-compose.yml 에 아래와 같이 추가했다.

```yml
# docker-compose.yml 
version: '3.3'
services:
	sample-service:
		...
		cap_add:  
		  - SYS_PTRACE
		...
```

---

아래는 실행 결과이다.

```shell
$ jhsdb jmap --pid 1
Attaching to process ID 1, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 11.0.20.1+1-post-Ubuntu-0ubuntu122.04
0x0000562783ce1000 14K /usr/lib/jvm/java-11-openjdk-amd64/bin/java
0x00007f782807f000 213K /usr/lib/jvm/java-11-openjdk-amd64/lib/libsunec.so
0x00007f785000e000 14K /usr/lib/x86_64-linux-gnu/libdl.so.2
0x00007f785001b000 36K /usr/lib/jvm/java-11-openjdk-amd64/lib/libmanagement_ext.so
0x00007f7850024000 28K /usr/lib/jvm/java-11-openjdk-amd64/lib/libmanagement.so
0x00007f785002b000 106K /usr/lib/jvm/java-11-openjdk-amd64/lib/libnet.so
0x00007f7850043000 92K /usr/lib/jvm/java-11-openjdk-amd64/lib/libnio.so
0x00007f78502d6000 44K /usr/lib/jvm/java-11-openjdk-amd64/lib/libzip.so
0x00007f78502e9000 39K /usr/lib/jvm/java-11-openjdk-amd64/lib/libjimage.so
0x00007f78502f2000 213K /usr/lib/jvm/java-11-openjdk-amd64/lib/libjava.so
0x00007f7850320000 67K /usr/lib/jvm/java-11-openjdk-amd64/lib/libverify.so
0x00007f7850331000 14K /usr/lib/x86_64-linux-gnu/librt.so.1
0x00007f7850436000 122K /usr/lib/x86_64-linux-gnu/libgcc_s.so.1
0x00007f7850456000 918K /usr/lib/x86_64-linux-gnu/libm.so.6
0x00007f785053d000 2207K /usr/lib/x86_64-linux-gnu/libstdc++.so.6.0.30
0x00007f7850769000 22703K /usr/lib/jvm/java-11-openjdk-amd64/lib/server/libjvm.so
0x00007f7851a3c000 2164K /usr/lib/x86_64-linux-gnu/libc.so.6
0x00007f7851c64000 80K /usr/lib/jvm/java-11-openjdk-amd64/lib/jli/libjli.so
0x00007f7851c77000 106K /usr/lib/x86_64-linux-gnu/libz.so.1.2.11
0x00007f7851c94000 16K /usr/lib/jvm/java-11-openjdk-amd64/lib/libextnet.so
0x00007f7851c9d000 235K /usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2

# 사용량 조회를 위해 아래 명령어를 이용해서 조회한다.
# jhsdb jmap --pid 1 명령어의 2행의 값을 다 더해서 KB, MB 단위로 보여주는 명령어다 (K 는 무시된다.)
$ jhsdb jmap --pid 1 | awk '{sum += $2} {gb = sum / 1024} END {print sum "(KB)" " / " gb "(MB)" }'
29431(KB) / 28.7412(MB)
```

흠.. 테스트 서버에서는 힙 크기에 대해서 큰 문제가 없다. 운영서버에서 직접 해봤지만 위와 같은 결과가 나왔다. 그래서 cron 을 이용해서 지속적으로 체크해보기로 했다.

#### Heap Memory Size Check

먼저 스크립트를 하나 만들었다

- docker container 내부에 만들었으며 파일의 경로는 `/opt/app/files/bin/heapcheck.sh` 이다.

```shell 
$ mkdir -p /opt/app/files/bin/ 
$ touch /opt/app/files/bin/heapcheck.sh
$ vim /opt/app/files/bin/heapcheck.sh 
```

아래는 Shell 의 내용이다.

```shell
#!/bin/bash
LOG_FILE_DATE=`date -d '+9 hour' "+%Y%m%d-%H"`
LOGFILE=/opt/app/files/logs/heap.log.$LOG_FILE_DATE

function retrieve_heap_dump() {
	SAMPLE_PID=`jps | awk '/sample/ {print $1}'` # sample.jar 파일의 PID 조회
	jhsdb jmap --pid $SAMPLE_PID | awk '{sum += $2} {gb = sum / 1024} END {print sum "(KB)" " / " gb "(MB)" }' # 사용중인 heap 사용량 조회
}

RES=$(retrieve_heap_dump)

echo "["`date -d '+9 hour' '+%Y-%m-%d %H:%M:%S'`"] Start Check Heap : " $RES >> $LOGFILE # 결과를 로그 파일로 전달
```

원래 crontab 에 저장해 5초마다 사용하려 하였으나 정상 동작하지 않아 아래와 같이 실행했다

```shell
$ export TMOUT=0 # session timeout 설정하지 않는다. (세션이 끊기지 않음)
$ while true
$ do 
$     docker exec -it sample_container /opt/app/files/bin/heapcheck.sh 
$     sleep 5 # 5초마다 실행하기 위해
$ done

########################################################################
# 아래는 모니터링을 로그 모니터링을 위해 사용한거다. (도커와 로컬의 시간차가 있어 +9 시간을 안더했다)

tail -f heap.log.`date  "+%Y%m%d-%H"`

```


#### Heap Memory Dump

```shell
$ jhsdb jmap --heap --pid `jps | awk '/sample/ {print $1}'`

Attaching to process ID 1, please wait...
Debugger attached successfully.
Server compiler detected.
JVM version is 11.0.20.1+1-post-Ubuntu-0ubuntu122.04

using thread-local object allocation.
Garbage-First (G1) GC with 4 thread(s)

Heap Configuration:
   MinHeapFreeRatio         = 40
   MaxHeapFreeRatio         = 70
   MaxHeapSize              = 2080374784 (1984.0MB)
   NewSize                  = 1363144 (1.2999954223632812MB)
   MaxNewSize               = 1247805440 (1190.0MB)
   OldSize                  = 5452592 (5.1999969482421875MB)
   NewRatio                 = 2
   SurvivorRatio            = 8
   MetaspaceSize            = 21807104 (20.796875MB)
   CompressedClassSpaceSize = 1073741824 (1024.0MB)
   MaxMetaspaceSize         = 17592186044415 MB
   G1HeapRegionSize         = 1048576 (1.0MB)

Heap Usage:
G1 Heap:
   regions  = 1984
   capacity = 2080374784 (1984.0MB)
   used     = 292967424 (279.3955078125MB)
   free     = 1787407360 (1704.6044921875MB)
   14.082434869581654% used

G1 Young Generation:
Eden Space:
   regions  = 181
   capacity = 226492416 (216.0MB)
   used     = 189792256 (181.0MB)
   free     = 36700160 (35.0MB)

   83.79629629629629% used

Survivor Space:
   regions  = 17
   capacity = 17825792 (17.0MB)
   used     = 17825792 (17.0MB)
   free     = 0 (0.0MB)

   100.0% used

G1 Old Generation:
   regions  = 83
   capacity = 155189248 (148.0MB)
   used     = 85349376 (81.3955078125MB)
   free     = 69839872 (66.6044921875MB)

   54.99696473817568% used

# 아래 명령어를 실행하면 heap.bin 파일이 생성된다.
$ jhsdb jmap --binaryheap --pid `jps | awk '/sample/ {print $1}'`


```

여기서 간단하게 VisualVM 을 이용해서 분석해보자
분석하는 방법은 https://jupiny.com/2019/07/15/java-heap-dump-analysis/ 참고

https://www.samsungsds.com/kr/insights/1232762_4627.html

https://dongwooklee96.github.io/post/2021/04/04/gcgarbage-collector-%EC%A2%85%EB%A5%98-%EB%B0%8F-%EB%82%B4%EB%B6%80-%EC%9B%90%EB%A6%AC.html
https://m.blog.naver.com/2feelus/220618444491
https://inkim0927.tistory.com/101

https://inpa.tistory.com/entry/JAVA-%E2%98%95-%EA%B0%80%EB%B9%84%EC%A7%80-%EC%BB%AC%EB%A0%89%EC%85%98-GC-%ED%8A%9C%EB%8B%9D-%EB%A7%9B%EB%B3%B4%EA%B8%B0

https://inpa.tistory.com/entry/JAVA-%E2%98%95-%EA%B0%80%EB%B9%84%EC%A7%80-%EC%BB%AC%EB%A0%89%EC%85%98GC-%EB%8F%99%EC%9E%91-%EC%9B%90%EB%A6%AC-%EC%95%8C%EA%B3%A0%EB%A6%AC%EC%A6%98-%F0%9F%92%AF-%EC%B4%9D%EC%A0%95%EB%A6%AC


NGCMN    NGCMX     NGC     S0C   S1C       EC      OGCMN      OGCMX       OGC         OC       MCMN     MCMX      MC     CCSMN    CCSMX     CCSC    YGC    FGC   CGC 

     0.0 2097152.0 491520.0    0.0 43008.0 448512.0        0.0  2097152.0   559104.0   559104.0      0.0 1146880.0 112128.0      0.0 1048576.0  15360.0     13     0     8

yg min : 0
yg max : 2097152 kb
og min : 0
og max : 2097152 kb 


```shell 
docker exec -it cludy-manager jstat -gccapacity 1

docker exec -it cludy-manager jstat -gcutil -t 1 1000
```

|옵션|기능|
|---|---|
|gc|각 힙(heap) 영역의 현재 크기와 현재 사용량(Eden 영역, Survivor 영역, Old 영역등), 총 GC 수행 횟수, 누적 GC 소요 시간을 보여 준다.|
|gccapactiy|각 힙 영역의 최소 크기(ms), 최대 크기(mx), 현재 크기, 각 영역별 GC 수행 횟수를 알 수 있는 정보를 보여 준다. 단, 현재 사용량과 누적 GC 소요 시간은 알 수 없다.|
|gccause|-gcutil 옵션이 제공하는 정보와 함께 마지막 GC 원인과 현재 발생하고 있는 GC의 원인을 알 수 있는 정보를 보여 준다.|
|gcnew|New 영역에 대한 GC 수행 정보를 보여 준다.|
|gcnewcapacity|New 영역의 크기에 대한 통계 정보를 보여 준다.|
|gcold|Old 영역에 대한 GC 수행 정보를 보여 준다.|
|gcoldcapacity|Old 영역의 크기에 대한 통계 정보를 보여 준다.|
|gcpermcapacity|Permanent 영역에 대한 통계 정보를 보여 준다.|
|gcutil|각 힙 영역에 대한 사용 정도를 백분율로 보여 준다. 아울러 총 GC 수행 횟수와 누적 GC 시간을 알 수 있다.|


|칼럼|설명|jstat 옵션|
|---|---|---|
|S0C|Survivor 0 영역의 현재 크기를 KB 단위로 표시|-gc -gccapacity -gcnew -gcnewcapacity|
|S1C|Survivor 1 영역의 현재 크기를 KB 단위로 표시|-gc -gccapacity -gcnew -gcnewcapacity|
|S0U|Survivor 0 영역의 현재 사용량을 KB 단위로 표시|-gc -gcnew|
|S1U|Survivor 1 영역의 현재 사용량을 KB 단위로 표시|-gc -gcnew|
|EC|Eden 영역의 현재 크기를 KB 단위로 표시|-gc -gccapacity -gcnew -gcnewcapacity|
|EU|Eden 영역의 현재 사용량을KB 단위로 표시|-gc -gcnew|
|OC|Old 영역의 현재 크기를 KB 단위로 표시|-gc -gccapacity -gcold -gcoldcapacity|
|OU|Old 영역의 현재 사용량을KB 단위로 표시|-gc -gcold|
|PC|Permanent영역의 현재 크기를 KB 단위로 표시|-gc -gccapacity -gcold -gcoldcapacity -gcpermcapacity|
|PU|Permanent영역의 현재 사용량을KB 단위로 표시|-gc -gcold|
|YGC|Young Generation의 GC 이벤트 발생 횟수|-gc -gccapacity -gcnew -gcnewcapacity -gcold -gcoldcapacity -gcpermcapacity -gcutil -gccause|
|YGCT|Yong Generation의 GC 수행 누적 시간|-gc -gcnew -gcutil -gccause|
|FGC|Full GC 이벤트가 발생한 횟수|-gc -gccapacity -gcnew -gcnewcapacity -gcold -gcoldcapacity -gcpermcapacity -gcutil -gccause|
|FGCT|Full GC 수행 누적 시간|-gc -gcold -gcoldcapacity -gcpermcapacity -gcutil -gccause|
|GCT|전체 GC 수행 누적 시간|-gc -gcold -gcoldcapacity -gcpermcapacity -gcutil -gccause|
|NGCMN|New Generation의 최소 크기를 KB단위로 표시|-gccapacity -gcnewcapacity|
|NGCMX|New Generation의 최대 크기를 KB단위로 표시|-gccapacity -gcnewcapacity|
|NGC|New Generation의 현재 크기를 KB단위로 표시|-gccapacity -gcnewcapacity|
|OGCMN|Old Generation의 최소 크기를 KB단위로 표시|-gccapacity -gcoldcapacity|
|OGCMX|Old Generation의 최대 크기를 KB단위로 표시|-gccapacity -gcoldcapacity|
|OGC|Old Generation의 현재 크기를 KB단위로 표시|-gccapacity -gcoldcapacity|
|PGCMN|Permanent Generation의 최소 크기를 KB단위로 표시|-gccapacity -gcpermcapacity|
|PGCMX|Permanent Generation의 최대 크기를 KB단위로 표시|-gccapacity -gcpermcapacity|
|PGC|현재 Permanent Generation의 크기를 KB단위로 표시|-gccapacity -gcpermcapacity|
|PC|Permanent 영역의 현재 크기를 KB단위로 표시|-gccapacity -gcpermcapacity|
|PU|Permanent 영역의 현재 사용량을 KB단위로 표시|-gc -gcold|
|LGCC|지난 GC의 발생 이유|-gccause|
|GCC|현재 GC의 발생 이유|-gccause|
|TT|Tenuring threshold. Young 영역 내에서 이 횟수만큼 복사되었을 경우(S0 ->S1, S1->S0) Old 영역으로 이동|-gcnew|
|MTT|최대 Tenuring threshold. Yong 영역 내에서 이 횟수만큼 복사되었을 경우 Old 영역으로 이동|-gcnew|
|DSS|적절한 Survivor 영역의 크기를 KB단위로 표시|-gcnew|