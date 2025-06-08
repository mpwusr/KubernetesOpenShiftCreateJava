## KubernetesOpenShiftCreateJava

## Make sure you have access to the Rancher K3S cluster
```
michaelwilliams@Michaels-MBP ~ % rdctl shell sudo cat /etc/rancher/k3s/k3s.yaml > ~/k3s.yaml

michaelwilliams@Michaels-MBP ~ % export KUBECONFIG=~/k3s.yaml                               
kubectl get nodes

NAME                   STATUS   ROLES                  AGE     VERSION
lima-rancher-desktop   Ready    control-plane,master   4m44s   v1.32.5+k3s1
michaelwilliams@Michaels-MBP ~ % echo 'export KUBECONFIG=~/k3s.yaml' >> ~/.zshrc
michaelwilliams@Michaels-MBP ~ % source ~/.zshrc
michaelwilliams@Michaels-MBP ~ % kubectl get nodes
NAME                   STATUS   ROLES                  AGE     VERSION
lima-rancher-desktop   Ready    control-plane,master   6m18s   v1.32.5+k3s1
```
## Extract certificate from Kubeconfig
```
kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d > openshift-ca.crt
```
## Create a test namespace
```
kubectl create namespace test
```
## give default service account admin privileges in test namespace
```
kubectl create rolebinding default-admin-binding \
  --clusterrole=admin \
  --serviceaccount=test:default \
  --namespace=test
```
## Export Bearer token
```
export BEARER_TOKEN=$(kubectl create token default)
```
## Build and Compile code
```
./gradlew clean build
./gradlew shadowJar
```
## Run code
```
java -jar build/libs/KubernetesOpenShiftCreateJava-all.jar
```
