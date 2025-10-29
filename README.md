# 🧭 Buscardados – Gerador de Markdown para Assistente GPT

**Buscardados** é um aplicativo desktop em **Java 17 (Swing)** desenvolvido pela **Questor Sistemas**, projetado para **coletar conteúdo de páginas da web** e gerar **arquivos Markdown (.md)** prontos para ingestão em **Assistentes GPT** (como o OpenAI GPT Platform).

O foco do projeto é permitir que usuários não técnicos capturem **documentações públicas**, **tutoriais** ou **páginas de ajuda** e consolidem tudo em um único `.md`, simplificando a criação de *fontes de conhecimento* (vector stores).

---

## ✨ Principais Recursos

* 🧾 Gera um único arquivo `.md` consolidado a partir de várias URLs.
* 🧠 Extrai automaticamente o conteúdo principal (ignora menus, rodapés e anúncios).
* 🔗 Converte links HTML em formato Markdown (`[texto](url)`).
* 🧩 Suporte a layouts de documentação (Docusaurus, MkDocs, GitBook, etc.).
* 🧱 Interface gráfica (Swing) intuitiva.
* ⚙️ Opções de User‑Agent, timeout, sumário (TOC) e metadados.
* 💾 Permite upload manual do `.md` para o GPT Assistants (como arquivo de *vector store*).

---

## 🏗️ Estrutura do Projeto

```
BUSCARDADOS/
 ├─ pom.xml                     # Configuração Maven
 ├─ target/                     # Saída do build (jar)
 └─ src/
     ├─ main/java/com/questor/urlgpt/
     │   ├─ App.java            # Ponto de entrada (main)
     │   └─ service/
     │       └─ MarkdownFetcherApp.java  # Lógica e GUI Swing
     └─ test/java/com/questor/urlgpt/
         └─ AppTest.java        # Teste básico
```

---

## ⚙️ Dependências

```xml
<dependencies>
  <dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
  </dependency>
</dependencies>
```

---

## 🔧 Compilação e Execução

### Build via Maven

```bash
mvn clean package
```

### Executar o aplicativo

```bash
java -jar target/buscardados.jar
```

> O JAR é configurado com a classe principal `com.questor.urlgpt.App` no manifest.

---

## 💡 Como Usar

1. **Abra o aplicativo** (`buscardados.jar`).
2. Informe um **título** para o `.md`.
3. Cole as **URLs** (uma por linha) que deseja consolidar.
4. Configure as opções:

   * **User‑Agent**: altera o identificador da requisição (útil para evitar bloqueios).
   * **Timeout (s)**: tempo máximo de espera por resposta.
   * **Incluir metadados**: adiciona data/hora e fonte de cada seção.
   * **Gerar Sumário (TOC)**: cria índice automático com links internos.
5. Clique em **Gerar .md** e escolha onde salvar.
6. **Suba o arquivo** manualmente como *file* no seu Assistente GPT (no vector store desejado).

---

## 🧪 Exemplo de Uso

### Entrada:

```
Título: Documentação Questor Cloud
URLs:
https://docs.questor.com.br/
https://docs.questor.com.br/instalacao
https://docs.questor.com.br/configuracao
```

### Saída (`documentacao-questor-cloud.md`):

```md
# Documentação Questor Cloud

> Documento gerado em 2025-10-29 15:00

## Sumário
- [Instalação](#instalacao)
- [Configuração](#configuracao)

<a id="instalacao"></a>
## Instalação
> Fonte: https://docs.questor.com.br/instalacao

Passos de instalação...

<a id="configuracao"></a>
## Configuração
> Fonte: https://docs.questor.com.br/configuracao

Parâmetros de configuração...
```

---

## ⚠️ Limitações e Dicas

| Situação                       | Solução                                                   |
| ------------------------------ | --------------------------------------------------------- |
| Páginas dependem de JavaScript | Gerar HTML renderizado com Playwright/Selenium antes.     |
| PDFs                           | Converter manualmente para texto/MD ou incluir só o link. |
| Erros 403/429                  | Alterar User‑Agent ou reduzir frequência de downloads.    |
| Markdown escapado              | Atualizar para versão atual do app (bug corrigido).       |

---

## 🧰 Futuras Melhorias

* [ ] Coleta automática de links internos (*depth=1*).
* [ ] Suporte a renderização JS headless (Playwright).
* [ ] Conversão de tabelas HTML → Markdown.
* [ ] Seletor customizado para limpeza de elementos.

---

## 🧱 Tecnologias Utilizadas

* ☕ **Java 17**
* 🧩 **Swing (GUI)**
* 🌐 **Jsoup (HTML Parser)**
* 🧰 **Maven** (build & empacotamento)

---

## 👥 Autor

**Marcos Eduardo Luiz**
Gerente de Desenvolvimento de Produtos — *Questor Sistemas S/A*
📧 [marcos.luiz@questores.com.br](mailto:marcos.luiz@questores.com.br)

---

## 📄 Licença

Distribuído sob a licença **MIT**.
Sinta-se livre para usar, modificar e distribuir com os devidos créditos.
