# Estuda++ API 🚀

**API Backend para o aplicativo de estudo com flashcards inteligentes**

API RESTful desenvolvida em Kotlin/Ktor para suportar o aplicativo Estuda++, oferecendo funcionalidades de inteligência artificial, autenticação Firebase e sistema de repetição espaçada.

## 👥 Equipe de Desenvolvimento

- **Chloe Anne Scaramal** - 12311BSI232
- **Gabriel Augusto Paiva** - 12311BSI245
- **João Pedro Zanetti** - 12311BSI230
- **Marcelo Gabriel Milani Santos** - 12311BSI251
- **Marcos Antônio da Silva Junior** - 12311BSI256
- **Paulo Daniel Forti da Fonseca** - 12311BSI321
- **Pedro Henrique Lopes Duarte** - 12311BSI237
- **Rayane Reis Mota** - 12311BSI233
- **Vinícius Resende Garcia** - 12021BCC027

## 📦 Entregáveis

Os arquivos entregáveis estão na pasta delivery/ na raiz do repositório:
```
delivery
├── cronograma.pdf
├── estudapp.apk
├── relatório.pdf
├── slides.pdf
├── vídeo-api.mkv
└── vídeo-app.mp4
```

## 📱 Sobre o Projeto

A Estuda++ API é uma aplicação backend moderna desenvolvida em Kotlin usando o framework Ktor. Hospedada no Google Cloud Run, ela oferece serviços de inteligência artificial para geração e validação de flashcards, chat inteligente e cálculo de repetição espaçada, integrando-se perfeitamente com o aplicativo Android.

## ✨ Funcionalidades Principais

### 🤖 Inteligência Artificial
- **Geração automática de flashcards** via Google Gemini AI
- **Validação inteligente** de respostas abertas
- **Chat educacional** com MonitorIA usando Groq/Llama
- **Análise semântica** de respostas do usuário

### 📚 Sistema de Flashcards
- **Suporte a 4 tipos** de flashcards:
  - Frente/Verso
  - Cloze (preenchimento de lacunas)
  - Digite a Resposta
  - Múltipla Escolha
- **CRUD completo** para flashcards e decks
- **Compartilhamento de decks** entre usuários

### 🧠 Repetição Espaçada Inteligente
- **Cálculo automático** da próxima data de revisão
- **Algoritmo baseado em IA** que considera o desempenho do usuário
- **Otimização do aprendizado** através de dados históricos

### 💬 Chat Educacional
- **MonitorIA** - assistente virtual para dúvidas
- **Histórico de conversas** persistente
- **Processamento assíncrono** para resposta rápida

### 🔐 Autenticação e Segurança
- **Integração com Firebase Authentication**
- **Validação de tokens JWT**
- **Controle de acesso** baseado em usuário
- **Políticas de segurança** robustas

## 🛠️ Tecnologias Utilizadas

### Core Framework
- **Kotlin** - Linguagem principal
- **Ktor** - Framework web moderno e assíncrono
- **Kotlinx Serialization** - Serialização JSON

### Inteligência Artificial
- **Google Gemini AI** - Geração e validação de conteúdo
- **Groq/Llama 3.3** - Chat conversacional
- **Processamento de linguagem natural**

### Banco de Dados e Backend
- **Firebase Realtime Database** - Armazenamento de dados
- **Firebase Authentication** - Autenticação de usuários
- **Firebase Admin SDK** - Integração server-side

### Cloud e Deploy
- **Google Cloud Run** - Hospedagem serverless
- **Google Cloud Build** - CI/CD automatizado
- **Container Registry** - Armazenamento de imagens Docker
- **Jib** - Containerização otimizada

### Bibliotecas e Ferramentas
- **Ktor Client** - Requisições HTTP
- **Logback** - Logging estruturado
- **Gradle** - Build e gerenciamento de dependências

## 📋 Arquitetura da API

### Estrutura de Pastas
```
src/main/kotlin/
├── domain/
│   ├── model/              # Modelos de domínio
│   ├── repositories/       # Interfaces de repositório
│   └── usecases/           # Casos de uso (Clean Architecture)
├── infrastructure/
│   ├── gateways/dto/       # Integrações externas (Gemini, Groq)
│   ├── http/               # Rotas e DTOs HTTP
│   └── persistence/        # Implementações de repositório
└── plugins/                # Configurações do Ktor
    ├── Routing.kt
    ├── Security.kt
    ├── Serialization.kt
    └── Monitoring.kt
```

### Principais Endpoints

#### Autenticação
- Todas as rotas protegidas requerem token Firebase Bearer

#### Decks
- `GET /my-decks` - Lista decks do usuário
- `POST /my-decks` - Cria novo deck
- `GET /share-deck/{userId}/{deckId}` - Compartilha deck
- `POST /share-deck/{userId}/{deckId}` - Importa deck compartilhado
- `POST /decks/{deckId}/calculate-next-review` - Calcula próxima revisão

#### Flashcards
- `POST /decks/{deckId}/flashcards/generate` - Gera flashcard com IA
- `POST /flashcards/validate` - Valida resposta do usuário

#### Chat
- `POST /chat/respond` - Envia mensagem para MonitorIA
- `GET /chat/history` - Histórico de conversas

#### Utilitários
- `GET /health` - Health check do serviço

## 🚀 Deploy e Configuração

### Pré-requisitos
- **Java 17+**
- **Google Cloud Account**
- **Firebase Project**
- **API Keys** (Gemini, Groq)

### Configuração Local

1. **Clone o repositório**
```bash
git clone [URL_DO_REPOSITORIO]
cd estudapp-api
```

2. **Configure as variáveis de ambiente**
```bash
export GEMINI_API_KEY="sua_chave_gemini"
export GROQ_API_KEY="sua_chave_groq"
export GOOGLE_APPLICATION_CREDENTIALS="caminho/para/service-account.json"
```

3. **Execute localmente**
```bash
./gradlew run
```

### Deploy no Google Cloud

#### Configuração Automatizada
```bash
chmod +x setup-deployment.sh
./setup-deployment.sh
```

#### Deploy Manual
```bash
# Build e deploy
./gradlew jib --image=gcr.io/estudapp-71947/estudapp-api:latest

# Deploy no Cloud Run
gcloud run deploy estudapp-api \
  --image=gcr.io/estudapp-71947/estudapp-api:latest \
  --region=southamerica-east1 \
  --platform=managed \
  --allow-unauthenticated \
  --memory=1Gi \
  --cpu=2
```

### CI/CD com Cloud Build

O arquivo `cloudbuild.yaml` configura deploy automático:

```yaml
steps:
  - name: 'gradle:8.14-jdk17'
    entrypoint: 'gradle'
    args: ['jib', '--image=gcr.io/$PROJECT_ID/estudapp-api:$BUILD_ID']
  
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: 'gcloud'
    args: ['run', 'deploy', 'estudapp-api', ...]
```

## 🔧 Configuração de Desenvolvimento

### Build Local
```bash
# Compile o projeto
./gradlew build

# Execute testes
./gradlew test

# Build da imagem Docker
./gradlew jib
```

### Configuração do Firebase
1. Baixe o arquivo `service-account-key.json` do Firebase Console
2. Configure no `GOOGLE_APPLICATION_CREDENTIALS`
3. Configure regras do Realtime Database

### Configuração das APIs de IA

#### Google Gemini
1. Acesse [Google AI Studio](https://aistudio.google.com/)
2. Gere uma API key
3. Configure `GEMINI_API_KEY`

#### Groq
1. Acesse [Groq Console](https://console.groq.com/)
2. Gere uma API key
3. Configure `GROQ_API_KEY`

## 📊 Monitoramento e Logs

### Logging
- **Structured logging** com Logback
- **Call logging** de todas as requisições
- **Error tracking** detalhado

### Monitoramento
```bash
# Ver logs em tempo real
gcloud run services logs tail estudapp-api --region=southamerica-east1

# Ver métricas no Cloud Console
gcloud run services describe estudapp-api --region=southamerica-east1
```

## 🧪 Testes e Validação

### Testes de Endpoint
```bash
# Health check
curl https://estudapp-api-xxx.a.run.app/health

# Teste com autenticação (substitua o token)
curl -H "Authorization: Bearer YOUR_FIREBASE_TOKEN" \
     https://estudapp-api-xxx.a.run.app/my-decks
```

### Testes de IA
```bash
# Teste de geração de flashcard
POST /decks/{deckId}/flashcards/generate
{
  "type": "FRENTE_VERSO",
  "userComment": "Pergunta sobre física quântica"
}
```

## 🔐 Segurança

### Autenticação
- **Firebase JWT tokens** obrigatórios
- **Validação server-side** de todos os tokens
- **Princípio do menor privilégio**

### Autorização
- **Controle de acesso** baseado no UID do usuário
- **Validação de propriedade** de recursos
- **Sanitização** de inputs

### Configurações de Segurança
```kotlin
// Exemplo de middleware de segurança
authenticate("firebase-auth") {
    // Rotas protegidas aqui
}
```

## 🌐 Integração com o App Android

### Autenticação
```kotlin
// No app Android
val token = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.token
```

### Chamadas de API
```kotlin
// Exemplo de requisição
val response = httpClient.post("$BASE_URL/decks/$deckId/flashcards/generate") {
    bearerAuth(token)
    contentType(ContentType.Application.Json)
    setBody(GenerateFlashcardRequest(type = "FRENTE_VERSO", userComment = prompt))
}
```

## 📈 Performance e Escalabilidade

### Otimizações
- **Processamento assíncrono** para IA
- **Connection pooling** para Firebase
- **Caching** de respostas frequentes
- **Timeouts** configuráveis

### Escalabilidade
- **Stateless design** para auto-scaling
- **Cloud Run** com scaling automático
- **Load balancing** nativo
- **Cold start** otimizado com Jib

## 🐛 Troubleshooting

### Problemas Comuns

#### Erro de Autenticação Firebase
```bash
# Verificar service account
gcloud iam service-accounts describe firebase-adminsdk-fbsvc@estudapp-71947.iam.gserviceaccount.com
```

#### Erro de API de IA
```bash
# Verificar variáveis de ambiente
gcloud run services describe estudapp-api --region=southamerica-east1 --format="export"
```

#### Problemas de Build
```bash
# Limpar cache do Gradle
./gradlew clean build --refresh-dependencies
```

## 📄 Licença

Este projeto está licenciado sob a GNU General Public License v3.0 - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🎓 Contexto Acadêmico

API desenvolvida para a disciplina de **Programação para Dispositivos Móveis** da **Universidade Federal de Uberlândia**, orientado pelo professor **Alexsandro Santos Soares**.

**Valor**: 35 pontos  
**Data de Apresentação**: 18/09/2025
