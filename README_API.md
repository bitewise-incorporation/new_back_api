# 🍽️ BiteWise API - Documentação Completa

API REST Spring Boot para geração e gerenciamento de receitas com inteligência artificial (Gemini + GPT-4).

## 🚀 Endpoints Disponíveis

### 1️⃣ Autenticação

#### Registrar Usuário
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "João Silva",
  "email": "joao@example.com",
  "password": "SenhaSegura123"
}
```

**Response (200 OK):**
```json
{
  "message": "Usuário registrado com sucesso."
}
```

---

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "joao@example.com",
  "password": "SenhaSegura123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2FvQGV4YW1wbGUuY29tIiwiaWF0IjoxNzYzOTEzMzQ3LCJleHAiOjE3NjM5OTk3NDd9.BciqPE0CdXBf_uaAyZ2d07fGSghKe_bCjjGDRTPIduk",
  "type": "Bearer",
  "message": "Login bem-sucedido"
}
```

**⚠️ Guardar o `token` para usar em requisições autenticadas!**

---

### 2️⃣ Receitas - Gerar

#### Gerar Receita com Ingredientes
```http
POST /api/recipes/generate?aiModel=gpt
Authorization: Bearer {token}
Content-Type: application/json

{
  "ingredients": ["frango", "arroz", "cebola"]
}
```

**Parâmetros Query:**
- `aiModel=gpt` → Usa GPT-4-Turbo (recomendado)
- `aiModel=gemini` → Usa Gemini (fallback automático)
- *(sem parâmetro)* → Auto-fallback (tenta GPT, depois Gemini)

**Response (200 OK):**
```json
{
  "title": "Arroz com Frango Simples",
  "prepTime": "40 minutos",
  "servings": 4,
  "difficulty": "Fácil",
  "ingredients": [
    "500g de peito de frango cortado em cubos",
    "2 xícaras de arroz branco",
    "1 cebola grande picada",
    "2 dentes de alho picados",
    "2 colheres de sopa de azeite",
    "4 xícaras de água quente ou caldo de galinha",
    "Sal a gosto",
    "Pimenta do reino a gosto",
    "Salsinha picada para decorar"
  ],
  "steps": [
    "Aqueça o azeite em uma panela grande...",
    "Adicione a cebola picada e refogue até ficar transparente...",
    "..."
  ],
  "tips": [
    "Dica 1: Use arroz integral para mais fibras",
    "Dica 2: Adicione brócolis ou cenoura picada",
    "..."
  ],
  "nutrition": {
    "calories": 520,
    "proteinGrams": 40,
    "fatGrams": 12,
    "carbsGrams": 60
  }
}
```

---

#### Modificar Receita
```http
POST /api/recipes/modify?aiModel=gpt
Authorization: Bearer {token}
Content-Type: application/json

{
  "originalRecipeJson": "{\"title\":\"Arroz com Frango\",\"servings\":4,...}",
  "modificationInstruction": "Faça vegetariana e baixa em calorias"
}
```

**Response (200 OK):** Receita modificada no mesmo formato

---

### 3️⃣ Receitas - Salvar & Listar

#### Salvar Receita
```http
POST /api/recipes/save
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Arroz com Frango",
  "prepTime": "40 minutos",
  "servings": 4,
  "difficulty": "Fácil",
  "ingredients": ["frango", "arroz", "cebola", "alho"],
  "steps": ["Passo 1", "Passo 2", "Passo 3"],
  "tips": ["Dica 1", "Dica 2"]
}
```

**Response (200 OK):**
```json
{
  "message": "Receita 'Arroz com Frango' salva com sucesso!"
}
```

---

#### Listar Receitas Salvas
```http
GET /api/users/me/saved-recipes
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "recipeId": 5,
    "title": "Arroz com Frango",
    "difficulty": "Fácil",
    "savedAt": "2025-11-23T12:30:45.123Z"
  },
  {
    "id": 2,
    "recipeId": 6,
    "title": "Salada Verde",
    "difficulty": "Muito Fácil",
    "savedAt": "2025-11-23T13:15:22.456Z"
  }
]
```

---

#### Deletar Receita Salva
```http
DELETE /api/users/me/saved-recipes/{id}
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "message": "Receita salva removida com sucesso."
}
```

---

### 4️⃣ Perfil de Usuário

#### Ver Perfil
```http
GET /api/users/me
Authorization: Bearer {token}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao@example.com"
}
```

---

#### Atualizar Perfil
```http
PUT /api/users/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "João Silva Junior",
  "email": "joao.junior@example.com"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "João Silva Junior",
  "email": "joao.junior@example.com"
}
```

---

### 5️⃣ Health Check

#### Verificar Status da API
```http
GET /health
```

**Response (200 OK):**
```
Backend BiteWise (Spring Boot) está no ar!
```

---

## 📊 Modelos de Inteligência Artificial

| Modelo | Status | Fallback | Qualidade | Speed |
|--------|--------|----------|-----------|-------|
| **GPT-4-Turbo** | ✅ Ativo | Primário | Excelente | Rápido |
| **Gemini-2.5-Pro** | ⚠️ Em Backup | Secundário | Muito Bom | Médio |

**Estratégia de Fallback:**
1. Tenta o modelo solicitado (`?aiModel=gpt` ou `?aiModel=gemini`)
2. Se falhar e sem parâmetro → tenta GPT first
3. Se GPT falhar → cai para Gemini automaticamente
4. Se Gemini falhar → erro 500

---

## 🔐 Segurança

### JWT Token
- **Expiration:** 24 horas (86.400.000 ms)
- **Tipo:** Bearer Token
- **Como usar:** `Authorization: Bearer {token}`

### Validação de Entrada
- Email deve ser válido
- Senha mínimo 6 caracteres
- Ingredientes: mínimo 3 itens
- Todos os campos obrigatórios

---

## 🛠️ Exemplo de Fluxo Completo (Frontend)

### JavaScript/TypeScript

```javascript
// 1. Registrar
const register = async () => {
  const res = await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: 'João',
      email: 'joao@test.com',
      password: 'Senha123'
    })
  });
  return res.json();
};

// 2. Login
const login = async () => {
  const res = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'joao@test.com',
      password: 'Senha123'
    })
  });
  const data = await res.json();
  localStorage.setItem('token', data.token); // Guardar token
  return data;
};

// 3. Gerar Receita
const generateRecipe = async (ingredients) => {
  const token = localStorage.getItem('token');
  const res = await fetch('http://localhost:8080/api/recipes/generate?aiModel=gpt', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ ingredients })
  });
  return res.json();
};

// 4. Salvar Receita
const saveRecipe = async (recipe) => {
  const token = localStorage.getItem('token');
  const res = await fetch('http://localhost:8080/api/recipes/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(recipe)
  });
  return res.json();
};

// 5. Listar Receitas
const getSavedRecipes = async () => {
  const token = localStorage.getItem('token');
  const res = await fetch('http://localhost:8080/api/users/me/saved-recipes', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  return res.json();
};
```

### React Hook Example

```javascript
import { useState } from 'react';

const useRecipeAPI = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const generateRecipe = async (ingredients) => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(
        'http://localhost:8080/api/recipes/generate?aiModel=gpt',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({ ingredients })
        }
      );
      if (!res.ok) throw new Error('Erro ao gerar receita');
      return await res.json();
    } catch (err) {
      setError(err.message);
      return null;
    } finally {
      setLoading(false);
    }
  };

  return { generateRecipe, loading, error };
};
```

---

## ⚙️ Configuração

### Variáveis de Ambiente (`.env`)
```powershell
# Database
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "bitewise_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sua_senha"

# JWT
$env:JWT_SECRET = "sua-chave-base64"
$env:JWT_EXPIRATION_TIME = "86400000"

# Google Gemini
$env:GOOGLE_API_KEY = "sua-chave-gemini"
$env:GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

# OpenAI GPT
$env:OPENAI_API_KEY = "sk-proj-sua-chave-openai"
$env:OPENAI_MODEL = "gpt-4-turbo"
```

---

## 🐛 Troubleshooting

| Erro | Causa | Solução |
|------|-------|---------|
| 401 Unauthorized | Token ausente/expirado | Fazer login novamente |
| 403 Forbidden | Token inválido | Verificar se token é válido |
| 500 IA Error | Chave IA inválida | Verificar variáveis de ambiente |
| 404 Not Found | Endpoint errado | Verificar URL e método HTTP |

---

## 📦 Stack Técnico

- **Framework:** Spring Boot 3.2.0
- **Java:** 17+
- **Database:** PostgreSQL 12+
- **Auth:** JWT (JJWT 0.12.5)
- **APIs IA:** OpenAI GPT-4, Google Gemini 2.5
- **Build:** Maven

---

## 📝 License

MIT License - veja LICENSE.md para detalhes

---

**Desenvolvido com ❤️ por BiteWise Team**
