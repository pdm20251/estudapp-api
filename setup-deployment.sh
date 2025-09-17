#!/bin/bash

# Script para configurar o deployment no Google Cloud
# Versão completa com Firebase e todas as permissões necessárias

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
  firebase.googleapis.com \
  firebasedatabase.googleapis.com \
  --project=$PROJECT_ID

echo "⏳ Aguardando APIs inicializarem (30s)..."
sleep 30

# 4. Obter informações do projeto
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
CLOUD_BUILD_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
FIREBASE_SA="firebase-adminsdk-fbsvc@${PROJECT_ID}.iam.gserviceaccount.com"

echo "📊 Informações do projeto:"
echo "   Project ID: $PROJECT_ID"
echo "   Project Number: $PROJECT_NUMBER"
echo "   Cloud Build SA: $CLOUD_BUILD_SA"
echo "   Firebase SA: $FIREBASE_SA"
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

# Permissão para criar tokens de service account
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${CLOUD_BUILD_SA}" \
  --role="roles/iam.serviceAccountTokenCreator" \
  --quiet

echo "✅ Permissões do Cloud Build configuradas"

# 6. Configurar permissões do Firebase Service Account
echo "🔥 Configurando Firebase Service Account..."

# Verificar se o service account do Firebase existe
if gcloud iam service-accounts describe $FIREBASE_SA --project=$PROJECT_ID >/dev/null 2>&1; then
    echo "✅ Firebase service account encontrado: $FIREBASE_SA"
else
    echo "⚠️  Firebase service account não encontrado, tentando criar..."
    
    # Tentar criar o service account
    gcloud iam service-accounts create firebase-adminsdk-fbsvc \
      --display-name="Firebase Admin SDK Service Account" \
      --description="Service Account para Firebase Admin SDK" \
      --project=$PROJECT_ID 2>/dev/null || echo "   ℹ️  Service account pode já existir"
      
    # Aguardar criação
    sleep 10
fi

# Configurar permissões do Firebase SA
echo "🔐 Aplicando permissões do Firebase..."

# Editor role (permissão ampla para Firebase)
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${FIREBASE_SA}" \
  --role="roles/editor" \
  --quiet

# Permissões específicas adicionais
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${FIREBASE_SA}" \
  --role="roles/datastore.user" \
  --quiet

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${FIREBASE_SA}" \
  --role="roles/storage.objectAdmin" \
  --quiet

# Permitir acesso ao Realtime Database (se a role existir)
if gcloud iam roles describe roles/firebasedatabase.admin --project=$PROJECT_ID >/dev/null 2>&1; then
    gcloud projects add-iam-policy-binding $PROJECT_ID \
      --member="serviceAccount:${FIREBASE_SA}" \
      --role="roles/firebasedatabase.admin" \
      --quiet
    echo "   ✅ Firebase Database admin configurado"
fi

echo "✅ Permissões do Firebase configuradas"

# 7. Verificar arquivos necessários
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

# 8. Configurar cloudbuild.yaml automaticamente
echo "⚙️  Verificando cloudbuild.yaml..."
if grep -q "gradle:7.6.0-jdk17" cloudbuild.yaml; then
    echo "⚠️  Detectada versão antiga do Gradle no cloudbuild.yaml"
    echo "   Atualizando para gradle:8.14-jdk17..."
    
    # Backup do arquivo original
    cp cloudbuild.yaml cloudbuild.yaml.backup
    
    # Atualizar versão do Gradle
    sed -i.tmp 's/gradle:7.6.0-jdk17/gradle:8.14-jdk17/g' cloudbuild.yaml
    rm cloudbuild.yaml.tmp 2>/dev/null || true
    
    echo "✅ cloudbuild.yaml atualizado (backup salvo como cloudbuild.yaml.backup)"
fi

# Verificar se o service account está configurado no cloudbuild.yaml
if ! grep -q "service-account.*firebase-adminsdk" cloudbuild.yaml; then
    echo "⚠️  Service account não encontrado no cloudbuild.yaml"
    echo "   Recomenda-se adicionar a linha:"
    echo "   - '--service-account=${FIREBASE_SA}'"
fi

# 9. Testar build local (opcional)
echo "🏗️  TESTE DE BUILD LOCAL:"
echo "   Para testar o build localmente antes do deploy:"
echo "   ./gradlew clean build"
echo "   ./gradlew jib --console=plain"
echo ""

# 10. Configurar trigger do GitHub
echo "🔗 CONFIGURAÇÃO DO GITHUB TRIGGER:"
echo "=========================================="
echo "1. Acesse: https://console.cloud.google.com/cloud-build/triggers?project=${PROJECT_ID}"
echo "2. Clique em 'Criar Trigger' ou 'Create Trigger'"
echo "3. Conecte seu repositório GitHub"
echo "4. Configure o trigger:"
echo "   - Nome: ${SERVICE_NAME}-deploy"
echo "   - Evento: Push para branch"
echo "   - Branch: ^${BRANCH_NAME}$"
echo "   - Configuração: Cloud Build (cloudbuild.yaml)"
echo "   - Localização: Repository"
echo ""

# 11. Primeiro build manual
echo "🚀 PRIMEIRO BUILD MANUAL:"
echo "=========================================="
echo "Execute após configurar o trigger:"
echo "gcloud builds submit --config cloudbuild.yaml --project=${PROJECT_ID}"
echo ""
echo "OU para build direto com tag específica:"
echo "BUILD_ID=\$(date +%Y%m%d-%H%M%S)"
echo "gcloud builds submit --config cloudbuild.yaml --substitutions=_BUILD_ID=\$BUILD_ID --project=${PROJECT_ID}"
echo ""

# 12. Deploy manual alternativo
echo "🚀 DEPLOY MANUAL (Alternativa ao cloudbuild):"
echo "=========================================="
echo "# Build da imagem primeiro:"
echo "./gradlew jib --image=gcr.io/${PROJECT_ID}/${SERVICE_NAME}:manual"
echo ""
echo "# Deploy com Firebase SA:"
echo "gcloud run deploy ${SERVICE_NAME} \\"
echo "  --image=gcr.io/${PROJECT_ID}/${SERVICE_NAME}:manual \\"
echo "  --region=${REGION} \\"
echo "  --platform=managed \\"
echo "  --allow-unauthenticated \\"
echo "  --port=8080 \\"
echo "  --memory=1Gi \\"
echo "  --cpu=2 \\"
echo "  --timeout=900s \\"
echo "  --service-account=${FIREBASE_SA} \\"
echo "  --max-instances=10 \\"
echo "  --min-instances=0"
echo ""

# 13. Configuração pós-deploy
echo "🔓 CONFIGURAÇÃO PÓS-DEPLOY:"
echo "=========================================="
echo "Após o primeiro deploy bem-sucedido, torne o serviço público:"
echo "gcloud run services add-iam-policy-binding ${SERVICE_NAME} \\"
echo "  --member=\"allUsers\" \\"
echo "  --role=\"roles/run.invoker\" \\"
echo "  --region=${REGION} \\"
echo "  --project=${PROJECT_ID}"
echo ""

# 14. Comandos úteis para diagnóstico
echo "🛠️  COMANDOS ÚTEIS PARA DIAGNÓSTICO:"
echo "=========================================="
echo "# Ver status do serviço:"
echo "gcloud run services describe ${SERVICE_NAME} --region=${REGION} --project=${PROJECT_ID}"
echo ""
echo "# Ver logs em tempo real:"
echo "gcloud run services logs tail ${SERVICE_NAME} --region=${REGION} --project=${PROJECT_ID}"
echo ""
echo "# Ver logs das últimas execuções:"
echo "gcloud run services logs read ${SERVICE_NAME} --region=${REGION} --project=${PROJECT_ID} --limit=100"
echo ""
echo "# Obter URL do serviço:"
echo "SERVICE_URL=\$(gcloud run services describe ${SERVICE_NAME} --region=${REGION} --project=${PROJECT_ID} --format=\"value(status.url)\")"
echo "echo \"URL do serviço: \$SERVICE_URL\""
echo ""
echo "# Testar endpoints básicos:"
echo "curl \"\$SERVICE_URL/\""
echo "curl \"\$SERVICE_URL/health\""
echo "curl \"\$SERVICE_URL/debug\""
echo ""
echo "# Verificar permissões do Firebase SA:"
echo "gcloud projects get-iam-policy ${PROJECT_ID} \\"
echo "  --flatten=\"bindings[].members\" \\"
echo "  --filter=\"bindings.members:${FIREBASE_SA}\""
echo ""
echo "# Ver imagens no Container Registry:"
echo "gcloud container images list --repository=gcr.io/${PROJECT_ID}"
echo ""
echo "# Ver builds recentes:"
echo "gcloud builds list --limit=10 --project=${PROJECT_ID}"
echo ""

# 15. Troubleshooting comum
echo "🔧 TROUBLESHOOTING COMUM:"
echo "=========================================="
echo "# Se a aplicação não iniciar:"
echo "1. Verifique os logs: gcloud run services logs read ${SERVICE_NAME} --region=${REGION} --limit=50"
echo "2. Verifique se a porta 8080 está configurada corretamente"
echo "3. Verifique se o Firebase SA tem as permissões corretas"
echo ""
echo "# Se houver erro 'service account not found':"
echo "gcloud iam service-accounts describe ${FIREBASE_SA} --project=${PROJECT_ID}"
echo ""
echo "# Para redeploy forçado:"
echo "gcloud run services delete ${SERVICE_NAME} --region=${REGION} --quiet"
echo "# Depois execute o deploy manual acima"
echo ""
echo "# Se houver problemas com Firebase:"
echo "export GOOGLE_APPLICATION_CREDENTIALS=''"
echo "gcloud auth application-default login"
echo ""

# 16. Informações finais
echo "🎯 RESUMO DA CONFIGURAÇÃO:"
echo "=========================================="
echo "✅ 1. APIs habilitadas:"
echo "   - Cloud Build, Container Registry, Cloud Run"
echo "   - Firebase, Firebase Database, IAM Credentials"
echo ""
echo "✅ 2. Service Accounts configurados:"
echo "   - Cloud Build: ${CLOUD_BUILD_SA}"
echo "   - Firebase: ${FIREBASE_SA}"
echo ""
echo "✅ 3. Permissões aplicadas:"
echo "   - Cloud Build pode fazer deploy e usar service accounts"
echo "   - Firebase SA tem acesso completo ao Firebase"
echo ""
echo "🔄 4. Próximos passos:"
echo "   - Configurar GitHub trigger (veja instruções acima)"
echo "   - Fazer primeiro build/deploy"
echo "   - Configurar acesso público ao serviço"
echo ""
echo "🌐 URL final esperada:"
echo "   https://${SERVICE_NAME}-${PROJECT_NUMBER}-${REGION//-}.a.run.app"
echo ""
echo "🔥 VARIÁVEIS DE AMBIENTE IMPORTANTES:"
echo "export PROJECT_ID=${PROJECT_ID}"
echo "export SERVICE_NAME=${SERVICE_NAME}"  
echo "export REGION=${REGION}"
echo "export FIREBASE_SA=${FIREBASE_SA}"
echo ""
echo "✨ Configuração concluída com sucesso!"
echo "   Execute os comandos de build/deploy quando estiver pronto."
