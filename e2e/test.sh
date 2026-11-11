#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

print_failure_logs() {
  status=$?
  trap - ERR
  echo "E2E failed (exit $status). Container status and logs:"
  docker compose ps -a || true
  docker compose logs --no-color --tail=200 broker schema-registry || true
  kubectl get pods -A || true
  kubectl logs test-producer -n test --tail=100 || true
  kubectl logs test-consumer -n test --tail=100 || true
  exit "$status"
}
trap print_failure_logs ERR

echo ""
echo "Create Kafka cluster"
echo ""
sed -i "s|{E2E_DIR_PATH}|$(pwd)|g" ./docker-compose.yaml
docker compose -f docker-compose.yaml up -d

echo ""
echo "Wait for Kafka and Schema Registry (up to 120 seconds)"
echo ""
ready=false
for ((attempt = 1; attempt <= 60; attempt++)); do
  for container in broker schema-registry; do
    state=$(docker inspect --format '{{.State.Status}}' "$container")
    if [[ "$state" == exited || "$state" == dead ]]; then
      echo "$container stopped before becoming ready"
      false
    fi
  done
  if curl --fail --silent --max-time 2 http://localhost:8081/subjects > /dev/null; then
    if docker compose exec -T broker kafka-topics --bootstrap-server broker:29092 --list > /dev/null 2>&1; then
      ready=true
      break
    fi
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  echo "Kafka or Schema Registry did not become ready"
  false
fi



echo ""
echo "Install Admission Webhook Controller in KinD cluster"
echo ""
export AZURE_TENANT_ID="f3292839-9228-4d56-a08c-6023c5d71e65"

helm repo add azure-workload-identity https://azure.github.io/azure-workload-identity/charts
helm repo update
helm install workload-identity-webhook azure-workload-identity/workload-identity-webhook \
  --namespace azure-workload-identity-system \
  --create-namespace \
  --version "${WORKLOAD_IDENTITY_CHART_VERSION:-1.6.1}" \
  --set azureTenantID="$AZURE_TENANT_ID" \
  --wait --timeout 2m30s\
  --debug \
  -v=5

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
echo "Create Schema in schema registry"
echo ""
curl --fail-with-body --show-error -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{"schema": "{\"type\": \"record\", \"name\": \"ExampleRecord\", \"fields\": [{\"name\": \"content\", \"type\": \"string\"}]}"}' \
    http://localhost:8081/subjects/nniikkoollaaii.topic-value/versions


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


kubectl apply -f manifests/consumer.pod.yaml

echo ""
echo "Sleep 20s"
echo ""
sleep 20s

sed -i "s/{BUILD_NUMBER}/$BUILD_NUMBER/g" manifests/producer.pod.yaml
kubectl apply -f manifests/producer.pod.yaml

echo ""
echo "Sleep 20s"
echo ""
sleep 20s

echo ""
echo "Producer logs: "
echo ""
kubectl logs test-producer -n test

echo ""
echo "Consumer logs: "
echo ""
kubectl logs test-consumer -n test
kubectl logs test-consumer -n test > result-logs.txt

# echo ""
# echo "Broker logs: "
# echo ""
# docker logs broker

echo ""
echo "Checking results ..."
echo ""
if [[ "$(cat result-logs.txt | grep "Received")" != "Received event: {\"content\": \"$BUILD_NUMBER\"}" ]]; then
  echo "Did not find expected result in log file"
  false
fi
echo "Success!"
