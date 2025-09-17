#!/bin/bash

# Script para configurar o deployment no Google Cloud
# Baseado nas configurações testadas e funcionais

set -e

# Variáveis (substitua pelos seus valores)
PROJECT_ID="estudapp-71947"
REGION="southamerica-east1"
SERVICE_NAME="estudapp-api"
GITHUB_OWNER="pdm20251"
GITHUB_REPO="estudapp-api"
BRANCH_NAME="master"

echo "🚀 Configurando deployment para o projeto: $PROJECT_ID"
echo "📦 Serviço: $SERVICE_NAME"
echo "🌍 Região: $REGION"
echo ""

# 1. Verificar autenticação
echo "🔐 Verificando autenticação..."
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q "@"; then
    echo "❌ Erro: Você precisa estar autenticado no Google Cloud"
    echo "Execute: gcloud auth login"
    exit 1
fi

# 2. Configurar projeto
echo "⚙️  Configurando projeto..."
gcloud config set project $PROJECT_ID

# 3. Habilitar APIs necessárias
echo "📡 Habilitando APIs do Google Cloud..."
gcloud services enable cloudbuild.googleapis.com \
  containerregistry.googleapis.com \
  run.googleapis.com \
  iamcredentials.googleapis.com \
  --project=$PROJECT_ID

# 4. Obter informações do projeto
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
CLOUD_BUILD_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"

echo "📊 Informações do projeto:"
echo "   Project ID: $PROJECT_ID"
echo "   Project Number: $PROJECT_NUMBER"
echo "   Cloud Build SA: $CLOUD_BUILD_SA"
echo ""

# 5. Configurar permissões do Cloud Build
echo "🔑 Configurando permissões do Cloud Build..."

# Permissões básicas para Cloud Run
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CLOUD_BUILD_SA}" \
  --role="roles/run.admin" \
  --quiet

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CLOUD_BUILD_SA}" \
  --role="roles/iam.serviceAccountUser" \
  --quiet

# Permissões para Container Registry
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CLOUD_BUILD_SA}" \
  --role="roles/storage.admin" \
  --quiet

echo "✅ Permissões configuradas"

# 6. Verificar arquivos necessários
echo "📁 Verificando arquivos do projeto..."
FILES=("build.gradle.kts" "cloudbuild.yaml" "src/main/kotlin/Application.kt")
for file in "${FILES[@]}"; do
    if [[ ! -f "$file" ]]; then
        echo "❌ Arquivo não encontrado: $file"
        echo "   Certifique-se de que todos os arquivos do projeto estão presentes"
        exit 1
    fi
done
echo "✅ Todos os arquivos necessários encontrados"

# 7. Instruções para conectar GitHub
echo ""
echo "🔗 CONFIGURAÇÃO DO GITHUB:"
echo "=========================================="
echo "1. Acesse: https://console.cloud.google.com/cloud-build/triggers?project=${PROJECT_ID}"
echo "2. Clique em 'Criar Trigger'"
echo "3. Conecte seu repositório GitHub"
echo "4. Configure o trigger:"
echo "   - Nome: ${SERVICE_NAME}-deploy"
echo "   - Evento: Push para branch"
echo "   - Branch: ^${BRANCH_NAME}$"
echo "   - Configuração: Cloud Build (cloudbuild.yaml)"
echo ""

# 9. Teste local do build (opcional)
echo "🏗️  PRIMEIRO BUILD (Execute após configurar o trigger):"
echo "gcloud builds submit --config cloudbuild.yaml --project=${PROJECT_ID}"
echo ""

# 10. Configuração de permissões pós-deploy
echo "🔓 CONFIGURAÇÃO PÓS-DEPLOY (Execute após o primeiro deploy):"
echo "gcloud run services add-iam-policy-binding ${SERVICE_NAME} \\"
echo "  --member=\"allUsers\" \\"
echo "  --role=\"roles/run.invoker\" \\"
echo "  --region=${REGION} \\"
echo "  --project=${PROJECT_ID}"
echo ""

# 11. Comandos úteis
echo "🛠️  COMANDOS ÚTEIS:"
echo "=========================================="
echo "# Ver status do serviço:"
echo "gcloud run services describe ${SERVICE_NAME} --region=${REGION}"
echo ""
echo "# Ver logs:"
echo "gcloud run services logs read ${SERVICE_NAME} --region=${REGION}"
echo ""
echo "# Obter URL do serviço:"
echo "gcloud run services describe ${SERVICE_NAME} --region=${REGION} --format=\"value(status.url)\""
echo ""
echo "# Testar endpoints:"
echo "SERVICE_URL=\$(gcloud run services describe ${SERVICE_NAME} --region=${REGION} --format=\"value(status.url)\")"
echo "curl \"\$SERVICE_URL/\""
echo "curl \"\$SERVICE_URL/health\""
echo ""

# 12. Verificação final
echo "🎯 PRÓXIMOS PASSOS:"
echo "=========================================="
echo "1. ✅ APIs habilitadas"
echo "2. ✅ Permissões configuradas" 
echo "3. 🔄 Configure o trigger do GitHub (veja instruções acima)"
echo "4. 🔄 Faça o primeiro build"
echo "5. 🔄 Configure permissões públicas do serviço"
echo ""
echo "🌐 Após o deploy, sua API estará disponível em:"
echo "https://${SERVICE_NAME}-${PROJECT_NUMBER}.${REGION}.run.app"
echo ""
echo "✨ Configuração concluída com sucesso!"