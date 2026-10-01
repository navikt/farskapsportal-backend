#!/bin/bash
kubectx dev-gcp
kubens farskapsportal

deployment="deployment/farskapsportal-api-feature"
[ "$1" == "main" ] && deployment="deployment/farskapsportal-api"

echo "Henter miljøparametere fra deployment: $deployment"
kubectl exec --tty $deployment -- printenv \
  | tr -d '\r' \
  | grep -E 'AZURE_|TOKEN_X_|DB_|_URL|SCOPE|VIRKSOMHETSSERTIFIKAT_|GCP_' \
  > apps/api/src/test/resources/application-lokal-nais-secrets.properties