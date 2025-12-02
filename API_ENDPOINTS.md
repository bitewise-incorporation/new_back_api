# API REST - Exemplos de Uso

## 📚 Base URL
```
http://localhost:8080
```

## 🔐 Autenticação

Todos os endpoints de receitas e usuários requerem o header:
```
Authorization: Bearer <seu-jwt-token>
```

---

## 👤 Endpoints de Autenticação

### 1. Registrar Novo Usuário

**Endpoint**: `POST /api/auth/register`

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao@example.com",
    "password": "senha123456"
  }'
```

**Response (200 OK)**:
```json
{
  "message": "Usuário registrado com sucesso."
}
```

**Erros**:
- `400 BAD_REQUEST` - Email já existe ou dados inválidos
- `400 BAD_REQUEST` - Senha com menos de 6 caracteres

---

### 2. Login

**Endpoint**: `POST /api/auth/login`

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "password": "senha123456"
  }'
```

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "message": "Autenticação bem-sucedida!"
}
```

**Erros**:
- `401 UNAUTHORIZED` - Email ou senha inválido
- `400 BAD_REQUEST` - Dados inválidos

---

## 🍳 Endpoints de Receitas

### 3. Gerar Receita com GPT (com imagem DALL-E 3)

**Endpoint**: `POST /api/recipes/generate?aiModel=gpt`

**Headers Obrigatórios**:
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request**:
```bash
curl -X POST http://localhost:8080/api/recipes/generate?aiModel=gpt \
  -H "Authorization: Bearer <seu-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "ingredients": [
      "peito de frango",
      "limão",
      "sal"
    ]
  }'
```

**Response (200 OK)**:
```json
{
  "title": "Frango ao Limão",
  "prepTime": "25 minutos",
  "servings": 4,
  "difficulty": "Fácil",
  "ingredients": [
    "500g de peito de frango",
    "3 limões",
    "sal a gosto",
    "2 dentes de alho",
    "azeite de oliva"
  ],
  "steps": [
    "Tempere o frango com sal e alho",
    "Aqueça o azeite em uma panela",
    "Frite o frango até dourar",
    "Adicione o suco de limão",
    "Cozinhe por 10 minutos"
  ],
  "tips": [
    "Use limão fresco para melhor sabor",
    "Não deixe o frango cozinhar demais"
  ],
  "nutrition": {
    "calories": 280,
    "proteinGrams": 35,
    "fatGrams": 12,
    "carbsGrams": 5
  },
  "image": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Parâmetros**:
- `aiModel` (opcional): `gpt` | `gemini` | `auto` (padrão: auto)
  - `gpt`: Usa GPT-4 + DALL-E 3 (com imagem)
  - `gemini`: Usa Gemini (sem imagem)
  - `auto`: Tenta GPT, fallback para Gemini

**Erros**:
- `401 UNAUTHORIZED` - Token inválido/expirado
- `400 BAD_REQUEST` - Menos de 3 ingredientes
- `500 INTERNAL_SERVER_ERROR` - Erro na API externa

---

### 4. Gerar Receita com Gemini (sem imagem)

**Endpoint**: `POST /api/recipes/generate?aiModel=gemini`

**Request**:
```bash
curl -X POST http://localhost:8080/api/recipes/generate?aiModel=gemini \
  -H "Authorization: Bearer <seu-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "ingredients": [
      "arroz",
      "feijão",
      "cebola"
    ]
  }'
```

**Response (200 OK)**:
```json
{
  "title": "Arroz com Feijão Brasileiro",
  "prepTime": "30 minutos",
  "servings": 4,
  "difficulty": "Fácil",
  "ingredients": [...],
  "steps": [...],
  "tips": [...],
  "nutrition": {...},
  "image": null
}
```

---

### 5. Modificar Receita Existente

**Endpoint**: `POST /api/recipes/modify?aiModel=gpt`

**Request**:
```bash
curl -X POST http://localhost:8080/api/recipes/modify?aiModel=gpt \
  -H "Authorization: Bearer <seu-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "originalRecipeJson": "{\"title\":\"Frango ao Limão\",\"ingredients\":[...]}",
    "modificationInstruction": "Tornar a receita vegetariana substitua o frango por tofu"
  }'
```

**Response (200 OK)**:
```json
{
  "title": "Tofu ao Limão com Temperos",
  "prepTime": "20 minutos",
  "servings": 4,
  "difficulty": "Fácil",
  "ingredients": [
    "500g de tofu firme",
    "3 limões",
    "sal a gosto",
    "2 dentes de alho",
    "azeite de oliva"
  ],
  "steps": [...],
  "tips": [...],
  "nutrition": {...},
  "image": "data:image/png;base64,..."
}
```

---

### 6. Salvar Receita

**Endpoint**: `POST /api/recipes/save`

**Request**:
```bash
curl -X POST http://localhost:8080/api/recipes/save \
  -H "Authorization: Bearer <seu-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Frango ao Limão",
    "prepTime": "25 minutos",
    "servings": 4,
    "difficulty": "Fácil",
    "ingredients": [
      "500g de peito de frango",
      "3 limões",
      "sal a gosto"
    ],
    "steps": [
      "Tempere o frango",
      "Frite até dourar"
    ],
    "tips": [
      "Use limão fresco"
    ]
  }'
```

**Response (200 OK)**:
```json
{
  "message": "Receita salva com sucesso!"
}
```

---

## 👥 Endpoints de Usuário

### 7. Obter Perfil do Usuário

**Endpoint**: `GET /api/users/me`

**Request**:
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <seu-token>"
```

**Response (200 OK)**:
```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@example.com"
}
```

---

### 8. Atualizar Perfil do Usuário

**Endpoint**: `PUT /api/users/me`

**Request**:
```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <seu-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Pedro Silva",
    "email": "joao.silva@example.com"
  }'
```

**Response (200 OK)**:
```json
{
  "id": 1,
  "name": "João Pedro Silva",
  "email": "joao.silva@example.com"
}
```

---

### 9. Listar Receitas Salvas

**Endpoint**: `GET /api/users/me/saved-recipes`

**Request**:
```bash
curl -X GET http://localhost:8080/api/users/me/saved-recipes \
  -H "Authorization: Bearer <seu-token>"
```

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "recipeId": 5,
    "title": "Frango ao Limão",
    "difficulty": "Fácil",
    "savedAt": "2025-12-01T15:30:00Z"
  },
  {
    "id": 2,
    "recipeId": 6,
    "title": "Arroz com Feijão",
    "difficulty": "Fácil",
    "savedAt": "2025-12-01T14:20:00Z"
  }
]
```

---

### 10. Remover Receita Salva

**Endpoint**: `DELETE /api/users/me/saved-recipes/{id}`

**Request**:
```bash
curl -X DELETE http://localhost:8080/api/users/me/saved-recipes/1 \
  -H "Authorization: Bearer <seu-token>"
```

**Response (200 OK)**:
```json
{
  "message": "Receita salva removida com sucesso."
}
```

---

## 🏥 Health Check

### 11. Verificar Status da API

**Endpoint**: `GET /api/health`

**Request**:
```bash
curl -X GET http://localhost:8080/api/health
```

**Response (200 OK)**:
```json
{
  "status": "UP"
}
```

---

## 🔍 Códigos HTTP

| Código | Significado | Causa |
|--------|------------|-------|
| 200 | OK | Requisição bem-sucedida |
| 400 | Bad Request | Dados inválidos |
| 401 | Unauthorized | Token ausente/inválido |
| 404 | Not Found | Recurso não encontrado |
| 500 | Internal Server Error | Erro no servidor/API externa |

---

## 💾 Fluxo Completo de Uso

### 1. Registrar e fazer login
```bash
# Registrar
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maria",
    "email": "maria@example.com",
    "password": "senha123456"
  }'

# Login para obter token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "maria@example.com",
    "password": "senha123456"
  }' | jq -r '.token')

echo "Token: $TOKEN"
```

### 2. Gerar receita
```bash
curl -X POST "http://localhost:8080/api/recipes/generate?aiModel=gpt" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ingredients": ["frango", "tomate", "cebola"]
  }' | jq '.'
```

### 3. Salvar receita
```bash
curl -X POST http://localhost:8080/api/recipes/save \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Frango com Tomate",
    "prepTime": "30 minutos",
    "servings": 4,
    "difficulty": "Fácil",
    "ingredients": ["500g frango", "3 tomates", "1 cebola"],
    "steps": ["Cozinhar o frango", "Adicionar tomate"],
    "tips": ["Use tomate fresco"]
  }'
```

### 4. Listar receitas salvas
```bash
curl -X GET http://localhost:8080/api/users/me/saved-recipes \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## 🚀 Dicas de Desenvolvimento

### Testando com Postman
1. Importar collection
2. Definir variável `{{token}}` no login
3. Usar `{{token}}` nos headers de requisições protegidas

### Debugando com Logs
```bash
# Terminal
tail -f application.log | grep -i "error\|warn\|recipe"
```

### Performance
- Requests GPT/Gemini: ~2-5 segundos
- Geração de imagem DALL-E 3: ~10-15 segundos
- Cache recomendado para receitas repetidas

---

## 📞 Suporte

Para reportar problemas:
1. Verifique os logs: `target/logs/app.log`
2. Valide o formato JSON
3. Confirme autenticação com `/api/auth/login`
4. Teste `/api/health` para verificar status

---

*Última atualização: 01/12/2025*
