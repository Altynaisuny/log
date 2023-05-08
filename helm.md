# helm
## docs

helm2.x to  helm3.x

https://v3.helm.sh/docs/faq/changes_since_helm2/

## 1. before install

you need kubectl env

use kubectx to switch stg and prd.

like this:

```shell
➜  ~ kubectx
eks_prd-app
eks_stg-app
```

由于stg 和 prd 是同一个

## 2. install

https://docs.aws.amazon.com/eks/latest/userguide/helm.html

macos:

```shell
brew install helm
```

## 3. Common actions

Common actions for Helm:

- helm search:    search for charts
- helm pull:      download a chart to your local directory to view
- helm install:   upload the chart to Kubernetes
- helm list:      list releases of charts

## 4. check  

> The helm list (or helm ls) function will show you a list of all deployed releases.

helm repo list 

```shell
➜  ~ helm repo list
NAME   	URL
t3n    	https://storage.googleapis.com/t3n-helm-charts
stable 	https://charts.helm.sh/stable
linoapp	s3://devops-helm-registry
```

helm list

```shell
➜  ~ helm list
NAME   	NAMESPACE	REVISION	UPDATED                                	STATUS  	CHART        	APP VERSION
linoapp	linoapp  	1382    	2021-12-17 08:04:25.822224662 +0000 UTC	deployed	linoapp-0.1.0	1.0
```

可以看到， 我们的服务是部署在一个名为linoapp 的repo中

## 5. search
查看repo下都有哪些chart

```shell
helm search repo linoapp
```

如果看到以下许多相似提示：

```
index.go:339: skipping loading invalid entry for chart "payment" "v0.3.6-08650" from /Users/altynai/Library/Caches/helm/repository/linoapp-index.yaml: validation: chart.metadata.version "v0.3.6-08650" is invalid
```
这是因为某一些chart在之前的版本有一些版本号的问题。

## 6.update
需要更新一下repo

```shell
helm repo update
```

我们本地会生成一些cache，目录在

```
/Users/altynai/Library/Caches/helm/repository/linoapp-index.yaml
```

取其中的一部分：

```
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-c4962.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-b886c.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-b4b52.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-9bdb6.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-8eb0b.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-837b4.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-6214b.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-56e03.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-4e524.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-4e3a6.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-45f36.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-3b313.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-283a3.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-27a46.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-17dfb.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-15b5d.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-0b8e5.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-0a691.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-06df6.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-74220.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.20-46284.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.19-c745c.tgz
    name: payment
    - s3://devops-helm-registry/payment-v1.4.19-6214b.tgz
```

从上面的内容可以看出：
payment-v1.4.20 有多个不同后缀的版本

推测：

在新tag已经发布到prd上以后，只要没有新的prd tag 出现，
stg上的迭代是通过不同的后缀版本 来helm install的。

多个chart拥有相同的大版本号和不同的小版本号，stg环境上通过不同的小版本号来区分迭代。

prd上每次发布都是一个新的大版本号

## 7. helm update from 2.x to 3.x

问题：

如何区分stg 和 prd环境？

官方提供了两种方式更新helm.

1. helm2 和 helm3 共存，同时管理集群

2. 将helm2 更新到新版本。

做一版新的image：

```shell
docker build -t linonetwork/gitlab-production:v0.1.2 gitlab-production/
```

## plugin

### install
1. helm plugin install https://github.com/hypnoglow/helm-s3.git

2. helm plugin install https://github.com/futuresimple/helm-secrets

3. helm plugin install https://github.com/databus23/helm-diff

安装时遇到了一些网络问题：

尝试在这两者其中切换：

- git config --global http.version HTTP/2

- git config --global http.version HTTP/1.1

还是没解决

最后解决了，由于helm-secrets的名称更改，加上原仓库不再维护，而最新的2.0.3可能有些安全性的校验没通过（猜测）

故改成：

```shell
helm plugin install  https://github.com/zendesk/helm-secrets --version 2.0.2
```
### kube-app中的helmfile

kube-app中一共有两个用于helmfile的文件

- helmfile.yaml
- helmfilefast.yaml

此处简单介绍一下

新的commit提交到kube-app,替换dependency版本号

kube-app中的gitlab-ci.yaml 触发CICD

通过 helmfile plugin 进行安装

执行 helmfile apply 之后，helmfile 会进行如下操作：

1. 添加 repositories 中声明的 repo

2. 运行 helm diff 进行对比

3. 根据 release中声明的配置，安装或更新 chart

一键apply：

```shell
helm repo add ...
helm install ...
helm upgrade ...
```

可以直接被简化为：

```shell
helmfile apply
```


## 8. cicd

1. linobot 账号

    注意，cicd使用的github账号为：

    - user.name "linobot"
    - user.email "linobot@lino.network"

    注意保证这个账号的可用性

2. CI/CD Variables

    gitlab DLive Settings中有许多Variables是CICD过程必须要用到的

    - AWS_ACCESS_KEY_ID
    - AWS_DEFAULT_REGION
    - AWS_SECRET_ACCESS_KEY
    - CHART_REPO
    - GITHUB_TOKEN
    - KUBE_CLUSTER_CERTIFICATE_PRD
    - KUBE_CLUSTER_STREAM_CERTIFICATE
    - NPM_TOKEN
    - REPOSITORY
    - SSH_PRIVATE_KEY

3. aws s3

    doc:[https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/set-up-a-helm-v3-chart-repository-in-amazon-s3.html]

    repo  **s3://devops-helm-registry**

    from doc:

    use the following command: 
    ```shell
    helm repo add stable-myapp s3://my-helm-charts/stable/myapp/

    helm package ./my-app  

    helm s3 push ./my-app-0.1.0.tgz stable-myapp

    helm search repo stable-myapp

    helm repo update
    ```

    If your repository somehow became inconsistent or broken, you can use reindex to recreate the index in accordance with the charts in the repository.

    helm s3 reindex mynewrepo

### 8.1 stg

#### 8.1.1 repo master change

首先，当有一个repo有代码提交到master，触发该repo的cicd：
- install:build
- install:test
- docker:stg
    * Successfully tagged sniffer:ca3b6967f13c9bb9a6ce2bf0b5d8a579bf74bf33
    * The push refers to repository [xxx.dkr.ecr.eu-west-1.amazonaws.com/sniffer]
- helm
    * version.sh

        执行docker中的脚本，使用github中的CI_COMMIT_SHA和CI_COMMIT_TAG组合为新chart的版本号

        注意变量名称变化：HELM_REPO->CHART_REPO

        入参：
        ```
        CHART_NAME:"sniffer"
        CHART_REPO:"s3://devops-helm-registry"
        CI_COMMIT_SHA:"ca3b6967f13c9bb9a6ce2bf0b5d8a579bf74bf33"(举例)
        CI_COMMIT_TAG:""(注意这里其实是空的)
        ```

        执行version.sh

        ```shell
        CHART_VERSION=$(version.sh $CHART_NAME $CI_COMMIT_SHA $CHART_REPO $CI_COMMIT_TAG)
        ```

        shell 脚本内容：

        ```shell
        #!/bin/bash

        set -ex

        CHART_NAME=$1
        CI_COMMIT_SHA=$2
        HELM_REPO=$3
        VERSION=$4
        SUFFIX=${CI_COMMIT_SHA:0:5}

        function error_exit
        {
            echo "${PROGNAME}: ${1:-"Unknown Error"}" 1>&2
            exit 1;
        }

        strindex() {
        x="${1%%$2*}"
        [[ "$x" = "$1" ]] && echo -1 || echo "${#x}"
        }

        helm repo add ${CHART_NAME} ${HELM_REPO} || error_exit "helm repo add failed"

        helm repo update || error_exit "helm repo update failed"

        if [ -z "$VERSION" ]; then
            VERSION=$(helm search "$CHART_NAME/$CHART_NAME" | awk -v d="$CHART_NAME/$CHART_NAME" '{ if ($1==d) print $2;}')
            if [ -z "$VERSION" ]; then
                VERSION="v0.1.0"
            else
                if [[ $VERSION =~ "+" ]]; # backward compatible
                then
                    LENGTH=$(strindex $VERSION "+")
                    VERSION=${VERSION:0:$LENGTH}
                elif [[ $VERSION =~ "-" ]];
                then
                    LENGTH=$(strindex $VERSION "-")
                    VERSION=${VERSION:0:$LENGTH}
                fi
            fi
        fi

        echo "$VERSION-$SUFFIX"

        ```

        这里需要注意：
        
        helm2中执行tiller init 后会默认配置一个名为local的本地repo

        helm3移除了tiller，所以没有local repo。

        此处执行：
        
        ```shell
        helm repo add linoapp s3://devops-helm-registry
        helm repo update
        ```

        注意helm search 在helm 2.x 和 helm 3.x 发生大的变化，原语法不再适用。

        在helm2中 通过helm search linoapp/sniffer可以找到最新的版本号，然后字符串截取前面的大版本号。例如：sniffer-v0.2.2-ca3b6 取sniffer-v0.2.2

        但是helm3中该语法弃用，需要找的新的替代方式（TODO）

        helm search 会随机返回一个版本，而${CI_COMMIT_TAG}只会在有新的tag出现的时候，才会有值，所以需要选择一个最新的版本号

    * chart.sh 命令

        ```shell
        chart.sh $CHART_NAME $CI_COMMIT_SHA $CHART_REPO $CI_COMMIT_TAG
        ```
        这里的chart.sh中前半部分与version.sh相同，都是生成新的chart版本

        后面多了一步，执行patch.py,更新.helm/sniffer-v0.2.2-ca3b6/Chart.yaml中的version

        例如：将version从0.1.0替换成v0.2.2-ca3b6

        ```
        apiVersion: v1
        appVersion: "1.0"
        description: A Helm chart for Kubernetes
        name: sniffer
        version: 0.1.0
        ```

        helm package .helm/sniffer

        helm s3 push --force sniffer-v0.2.2-ca3b6 linoapp

        ```shell
        chart.sh $CHART_NAME $CI_COMMIT_SHA $CHART_REPO $CI_COMMIT_TAG
        ```
- release:stg
    push [master 4624aff89] [stg][sniffer] Update Lino App to kube-app
    
    like this:

    - stg/linoapp/values.yaml

    ```yaml
    sniffer:
        image:
            tag: "b5356e582b083b0c1757000886acf5971c5d116f"
            tag: "ca3b6967f13c9bb9a6ce2bf0b5d8a579bf74bf33"
    ```

    - stg/linoapp/requirements.yaml

    ```yaml
    - name: sniffer
        version: v0.2.0-e0d65
        version: v0.2.2-ca3b6
    ```

#### 8.1.2 kube-app auto merge pull request

我们分析kube-app中的任务：[stg][sniffer] Update Lino App

首先检查kube-app里的commit变更：

- stg/linoapp/values.yaml

```yaml
sniffer:
    image:
        tag: "b5356e582b083b0c1757000886acf5971c5d116f"
        tag: "ca3b6967f13c9bb9a6ce2bf0b5d8a579bf74bf33"
```

- stg/linoapp/requirements.yaml

```yaml
  - name: sniffer
    version: v0.2.0-e0d65
    version: v0.2.2-ca3b6
```

发现他只跑了一个任务release:stg:linoapp

gitlab-ci.yaml:
```yaml
release:stg:linoapp:
  only:
    refs:
      - master
    changes:
      - stg/linoapp/*
      - stg/helm_vars/linoapp/secrets.yaml
    variables:
      - $FAST != "off"
  stage: release
  tags:
    - kubernetes
  image: linonetwork/gitlab-staging:v0.1.3
  script:
    - helmfile --environment stg --file helmfilefast.yaml --selector name=linoapp apply
```

docker hub image:linonetwork/gitlab-staging:v0.1.3

1. git clone 
    - git@github.com:lino-network/staging-kubeconfig.git
    - git@github.com:lino-network/prod-kubeconfig.git

2. set kube config
    - mkdir .kube
    - copy config to .kube
    - kubectl config set clusters.stg.certificate-authority-data "$KUBE_CLUSTER_CERTIFICATE"
    - kubectl config set clusters.prd.certificate-authority-data "$KUBE_CLUSTER_CERTIFICATE_PRD"
3. helmfile --environment stg --file helmfilefast.yaml --selector name=linoapp apply

    - Adding repo linoapp s3://devops-helm-registry
    - Building dependency release=linoapp, chart=stg/linoapp
    - download all chart from repo s3://devops-helm-registry
    - Decrypting secret
    - Comparing & Upgrading:
        * linoapp, linoapp-sniffer, Deployment (apps)has changed:

        > Source: linoapp/charts/sniffer/templates/deployment.yaml

        ```
        -     image: "xxx.dkr.ecr.eu-west-1.amazonaws.com/sniffer:cd116439ed839905ab6a8fe46fe303aeb6c4845c"
        +     image: "xx.dkr.ecr.eu-west-1.amazonaws.com/sniffer:d87a32a5ea9b4553fde08198ee1011242e55860d"
        ```

        * linoapp, linoapp-web-neo, ConfigMap (v1) has changed:

        > Source: linoapp/charts/web-neo/templates/configmap.yaml

        ```
        -     chart: web-neo-v0.7.21-aa0d4
        +     chart: web-neo-v0.7.21-a47e8
        ```
        * (another situation)linoapp, linoapp-web-neo, Service (v1) has changed:
        * (another situation)linoapp, linoapp-web-neo, HorizontalPodAutoscaler (autoscaling) has changed:
    - log:
        ```
        Release "linoapp" has been upgraded. Happy Helming!
        NAME: linoapp
        LAST DEPLOYED: Mon Dec 20 11:12:23 2021
        NAMESPACE: linoapp
        STATUS: deployed
        REVISION: 1384
        TEST SUITE: None
        Listing releases matching ^linoapp$
        linoapp	linoapp  	1384    	2021-12-20 11:12:23.743276332 +0000 UTC	deployed	linoapp-0.1.0	1.0        
        UPDATED RELEASES:
        NAME      CHART         VERSION
        linoapp   stg/linoapp     0.1.0
        ```

helm-diff是被helmfile使用的，可以在执行操作前输出清晰的diff

### 8.2 prd

如果需要发布新版本到生产环境

#### 8.2.1 new tag 

首先在repo中打一个tag。例如：v0.2.3

- install:build
- install:test
- docker:prd
- helm
    
    大部分流程与stg都是相同的，这里不过多描述

- release:prd

    生成新的pr到kube-app中

    ```
    [promote-sniffer-v0.2.3 62622829c] Update Lino App
    2 files changed, 2 insertions(+), 2 deletions(-)
    remote: 
    remote: Create a pull request for 'promote-sniffer-v0.2.3' on GitHub by visiting:        
    remote:      https://github.com/lino-network/kube-app/pull/new/promote-sniffer-v0.2.3        
    remote: 
    To github.com:lino-network/kube-app.git
    * [new branch]          promote-sniffer-v0.2.3 -> promote-sniffer-v0.2.3
    ```

当我们merge kube-app中的 pr

pr中内容：

- prd/linoapp/values.yaml

```yaml
sniffer:
    image:
        tag: "v0.2.2"
        tag: "v0.2.3"
```

- prd/linoapp/requirements.yaml

```yaml
  - name: sniffer
    version: v0.2.2-61a65
    version: v0.2.3-ca3b6
```

触发了**release:prd:linoapp**

image: linonetwork/gitlab-staging:v0.1.2

include install 

- aws-iam-authenticator
- kubectl
- helm 
- helmfile 
- helm-s3 
- helm-secrets 
- helm-diff

script:

```shell
helmfile -e=prd -f helmfilefast.yaml -l name=linoapp -l name=linoapp-ws -l name=jobs apply
```
environment = prd
