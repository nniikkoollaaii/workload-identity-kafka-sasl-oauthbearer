
cd e2e

echo ""
echo "Create Kafka cluster"
echo ""
docker-compose -f docker-compose.yaml up -d 

echo ""
echo "Sleep 10s"
echo ""
sleep 10s



echo ""
echo "Install Admission Webhook Controller in KinD cluster"
echo ""
docker pull mcr.microsoft.com/oss/azure/workload-identity/webhook:v1.1.0
kind load docker-image mcr.microsoft.com/oss/azure/workload-identity/webhook:v1.1.0
export AZURE_TENANT_ID="f3292839-9228-4d56-a08c-6023c5d71e65"

#export AZURE_ENVIRONMENT="AZUREPUBLICCLOUD"
# not working as AZURE_ENVIRONMENT get not replaced
#curl -sL https://github.com/Azure/azure-workload-identity/releases/download/v1.1.0/azure-wi-webhook.yaml | envsubst | kubectl apply -f -



helm repo add azure-workload-identity https://azure.github.io/azure-workload-identity/charts
helm repo update
helm install workload-identity-webhook azure-workload-identity/workload-identity-webhook \
  --namespace azure-workload-identity-system \
  --create-namespace \
  --set azureTenantID="f3292839-9228-4d56-a08c-6023c5d71e65" \
  --wait \
  --debug \
  -v=5 \
  --devel

# In early pipeline runs the test failed because the Webhook Service was not ready. 
# Local tests showed it took from "2023-06-13T20:04:14.624230Z to "2023-06-13T20:06:06.372301Z"
# until message "Serving webhook server" was displayed in logs of azure-wi-webhook-controller-manager-* pods
#echo "sleep 120s to give azure-wi-webhook-controller-manager time to setup"
#sleep 120s
# with Helm use --wait

#debug
kubectl get pods -A
kubectl logs --selector=azure-workload-identity.io/system=true -n azure-workload-identity-system

echo ""
echo "Build producer"
echo ""
cd test-producer
mvn -B package --no-transfer-progress -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
cp ../broker/truststore/kafka.truststore.jks ./kafka.truststore.jks
docker build -t io.github.nniikkoollaaii.kafka-producer-app:1.0.0 .
kind load docker-image io.github.nniikkoollaaii.kafka-producer-app:1.0.0
cd ..

echo ""
echo "Build consumer"
echo ""
cd test-consumer
mvn -B package --no-transfer-progress -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn
cp ../broker/truststore/kafka.truststore.jks ./kafka.truststore.jks
docker build -t io.github.nniikkoollaaii.kafka-consumer-app:1.0.0 .
kind load docker-image io.github.nniikkoollaaii.kafka-consumer-app:1.0.0
cd ..


echo ""
echo "Create test pods in KinD cluter"
echo ""
kubectl apply -f manifests/ns.yaml
kubectl apply -f manifests/sa.yaml
kubectl apply -f manifests/host.service.yaml
sed -i "s/{BUILD_NUMBER}/$BUILD_NUMBER/g" manifests/producer.pod.yaml
kubectl apply -f manifests/producer.pod.yaml
kubectl apply -f manifests/consumer.pod.yaml

echo ""
echo "Sleep 10s"
echo ""
sleep 10s

echo ""
echo "Producer logs: "
echo ""
kubectl logs test-producer -n test

echo ""
echo "Consumer logs: "
echo ""
kubectl logs test-consumer -n test
kubectl logs test-consumer -n test > result-logs.txt



echo ""
echo "Checking results ..."
echo ""
if [[ "$(cat result-logs.txt | grep "Received")" != "Received event: $BUILD_NUMBER" ]]; then
  echo "Did not find expected result in log file"; exit 1;
fi
echo "Success!"