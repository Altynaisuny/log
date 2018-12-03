# vps

> centos 7

## ready

```shell
# shadowsocks
pip install https://github.com/shadowsocks/shadowsocks/archive/master.zip -U
# chose path of libsodium
cd /home/admin
# libsodium config
wget https://github.com/jedisct1/libsodium/releases/download/1.0.15/libsodium-1.0.15.tar.gz
tar xf libsodium-1.0.15.tar.gz && cd libsodium-1.0.15
./configure && make -j2 && make install
ldconfig
```

## config

```shell
cd /etc
mkdir shadowsocks && cd shadowsocks
touch ss.json
vim ss.json
```

* ss.json

```json
{
    "server":"0.0.0.0",
    "server_port":443,
    "local_address": "127.0.0.1",
    "local_port":1080,
    "password":"",
    "timeout":300,
    "method":"aes-256-gcm",
    "fast_open": true
}

```

## start

* shadowsocks  start
``` shell
ssserver -c /etc/shadowsocks/ss.json -d start
```

* another 
``` shell
ssserver -c /etc/shadowsocks/ss.json -d stop
ssserver -c /etc/shadowsocks/ss.json -d restart
```

* bbr
```shell
wget --no-check-certificate https://github.com/teddysun/across/raw/master/bbr.sh && chmod +x bbr.sh && ./bbr.sh
uname -r
# linux 内核 版本大于 4.19
```