# kube

## install

if use aws:

```shell
$ apt-get install kubeadm
```

if in Ubuntu 

Unable to locate package kubelet:

try with:

```shell
sudo vim /etc/apt/sources.list.d/kubernetes.list
deb http://apt.kubernetes.io/ kubernetes-xenial main
```

if centos:

you need do this:

```bash
cat <<EOF | sudo tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-\$basearch
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
exclude=kubelet kubeadm kubectl
EOF

# Set SELinux in permissive mode (effectively disabling it)
sudo setenforce 0
sudo sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

sudo yum install -y kubelet kubeadm kubectl --disableexcludes=kubernetes

sudo systemctl enable --now kubelet
```

success:

```shell
$ kubeadm
```

## init

```shell
$ kubeadm init
```

if success:

you will got this:

```shell
Your Kubernetes control-plane has initialized successfully!

To start using your cluster, you need to run the following as a regular user:

  mkdir -p $HOME/.kube
  sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
  sudo chown $(id -u):$(id -g) $HOME/.kube/config

Alternatively, if you are the root user, you can run:

  export KUBECONFIG=/etc/kubernetes/admin.conf

You should now deploy a pod network to the cluster.
Run "kubectl apply -f [podnetwork].yaml" with one of the options listed at:
  https://kubernetes.io/docs/concepts/cluster-administration/addons/

Then you can join any number of worker nodes by running the following on each as root:

kubeadm join IP:PORT --token tokentokentoken \
    --discovery-token-ca-cert-hash sha256:xx
```





## docker

if you got this:

[WARNING Service-Docker]: docker service is not enabled, please run 'systemctl enable docker.service'

run

```shell
$ systemctl enable docker.service
```

if your os  is centos lack of  docker:

Install the yum-utils package (which provides the yum-config-manager utility) and set up the stable repository.

```shell
$ sudo yum install -y yum-utils
$ sudo yum-config-manager \
    --add-repo \
    https://download.docker.com/linux/centos/docker-ce.repo
$ sudo yum update
$ sudo yum install docker-ce docker-ce-cli containerd.io
```

test docker:

```shell
sudo docker run hello-world
```

## ouch

```	[ERROR NumCPU]: the number of available CPUs 1 is less than the required 2```

## config

```shell
ls /etc/kubernetes/
```

you will found these file:

admin.conf  controller-manager.conf  kubelet.conf  scheduler.conf

