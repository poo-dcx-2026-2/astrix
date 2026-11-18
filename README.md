# Sistema de Gestão de Acervo e Biblioteca

Projeto desenvolvido para a disciplina de **Programação Orientada a Objetos** — DCX/CCAE/UFPB.  
**Entrega 2: Design e Persistência**.

---

## 1. Visão Geral do Domínio

O sistema gerencia o acervo bibliográfico, os usuários, os empréstimos, devoluções e a fila de reservas de uma biblioteca universitária, com persistência completa em arquivo.

As entidades centrais do domínio são:
* **Item**: Representa a obra intelectual cadastrada no acervo (código único, título, autoria, categoria e ano de publicação).
* **Exemplar**: Representa a unidade física correspondente a um Item, identificada por tombo único global e estado (`DISPONIVEL`, `EMPRESTADO`, `RESERVADO`, `INDISPONIVEL`).
* **Usuario**: Representa o membro da comunidade acadêmica (matrícula, nome, situação ativa e contagem de empréstimos ativos).
* **Emprestimo**: Registro de empréstimo com identificador único, tombo, matrícula, data de empréstimo, data prevista (prazo de 14 dias), data de devolução e status (`ATIVO`, `DEVOLVIDO`, `ATRASADO`).
* **Reserva**: Registro da fila de espera por ordem de chegada para itens que não possuem exemplares disponíveis, com posição na fila e controle de separação de exemplar após devolução.

---

## 2. Mapa do Projeto

A estrutura de diretórios do projeto está organizada da seguinte forma:

```text
astrix-main/
├── .github/workflows/
│   └── build.yml               # Pipeline de integração contínua (GitHub Actions com Maven e JDK 21)
├── dados/
│   ├── itens-exemplo.csv       # Arquivo de dados de exemplo para o acervo (PRESERVADO, nunca sobrescrito)
│   └── biblioteca.dat          # Arquivo binário de persistência gerado em tempo de execução (ignorado no git)
├── docs/
│   ├── decisoes.md             # Registro completo das decisões de design, GRASP e arquitetura (Entrega 2)
│   ├── modelo.puml             # Diagrama de classes UML do domínio e arquitetura em formato PlantUML
│   ├── modelo.png              # Imagem gerada do diagrama UML do sistema
│   └── devolutiva-entrega-1.md # Feedback e correções da Entrega 1
├── src/
│   ├── main/java/br/ufpb/dcx/poo/biblioteca/
│   │   ├── Fabrica.java        # Ponto de entrada oficial para instanciação independente da Biblioteca
│   │   ├── contrato/           # Pacote CONGELADO com interfaces, records de visualização e exceções
│   │   │   ├── Biblioteca.java
│   │   │   ├── AcervoService.java
│   │   │   ├── UsuarioService.java
│   │   │   ├── EmprestimoService.java
│   │   │   ├── RelatorioService.java
│   │   │   ├── *View.java
│   │   │   ├── Status*.java
│   │   │   └── excecoes/
│   │   ├── persistencia/       # Módulo isolado de persistência (domínio livre de E/S)
│   │   │   ├── GerenciadorPersistencia.java  # Leitura e escrita atômica com try-with-resources
│   │   │   └── DadosPersistencia.java        # DTO Snapshot serializável do estado completo
│   │   └── inicial/            # Implementação do modelo de domínio e serviços
│   │       ├── Item.java                 # Entidade Item com proteção de invariantes
│   │       ├── Exemplar.java             # Entidade Exemplar com transição de status
│   │       ├── Usuario.java              # Entidade Usuario
│   │       ├── Emprestimo.java           # Entidade Emprestimo (Information Expert de datas e prazos)
│   │       ├── Reserva.java              # Entidade Reserva com gestão de posições e separação
│   │       ├── AcervoEmMemoria.java      # Implementação do AcervoService
│   │       ├── UsuariosEmMemoria.java    # Implementação do UsuarioService
│   │       ├── EmprestimosEmMemoria.java # Implementação do EmprestimoService (empréstimos e reservas)
│   │       ├── RelatoriosNaoImplementados.java # Stub para Entrega 3
│   │       └── BibliotecaInicial.java    # Ponto de entrada centralizador (Padrão Facade)
│   └── test/java/br/ufpb/dcx/poo/biblioteca/
│       ├── AcervoTest.java        # Suíte de testes do acervo (E1 + E2: categorias, baixa e correções)
│       ├── UsuarioTest.java       # Suíte de testes de usuários (E1 + E2: desativação e reativação)
│       ├── EmprestimoTest.java    # Suíte de testes de empréstimos, devoluções e ciclo de reservas
│       ├── PersistenciaTest.java  # Suíte de persistência (ida e volta, @TempDir, ausente e corrompido)
│       └── FabricaTest.java       # Suíte de testes de isolamento e integridade da Fábrica
├── pom.xml                     # Configuração do Maven (Java 21, JUnit 5 Surefire)
├── DECLARACAO-DE-USO-DE-IA.md  # Registro transparente do uso de ferramentas de IA
└── README.md                   # Documentação completa do projeto
```

---

## 3. Onde Ficam os Dados (Persistência)

* **Diretório padrão:** `dados/` na raiz de execução do projeto.
* **Propriedade de sistema:** Caso a propriedade JVM `biblioteca.dados` esteja definida (`System.getProperty("biblioteca.dados")`), o sistema utilizará o caminho especificado por ela. É através dessa propriedade que os testes automatizados apontam a persistência para pastas temporárias seguras via `@TempDir`.
* **Arquivo gravado:** `dados/biblioteca.dat` (serialização binária nativa).
* **Preservação de arquivos do repositório:** O arquivo `dados/itens-exemplo.csv` **não é alterado ou sobrescrito**. O arquivo `biblioteca.dat` e arquivos de dados temporários estão devidamente ignorados no `.gitignore`.
* **Robustez e Atomicidade:**
  * Diretório ou arquivo ausente: inicializa o acervo como vazio sem lançar exceção.
  * Arquivo corrompido (bytes inválidos ou integridade referencial rompida): lança `PersistenciaException` com a causa preservada e **não altera** o estado em memória.
  * Todo recurso de entrada e saída é manipulado com `try-with-resources`.

---

## 4. Decisões de Design e Arquitetura

O detalhamento das escolhas de modelagem e padrões GRASP/GoF encontra-se documentado em:
👉 **[docs/decisoes.md](docs/decisoes.md)**

Principais decisões abordadas:
1. **Composição vs. Herança**: Composição estrita entre `Item` e `Exemplar`.
2. **Interfaces e Polimorfismo**: Utilizadas apenas nas fronteiras de serviço onde há substituição real.
3. **Padrões GRASP (Aula 12)**:
   * *Information Expert* na entidade `Emprestimo` (cálculo de prazos e validação temporal).
   * *Controller* e *Information Expert* em `EmprestimosEmMemoria` e `Reserva` (fila FIFO, transições de status e reordenação).
4. **Padrão Facade (Aula 13)**: `BibliotecaInicial` como porta única de entrada que protege o domínio de acessos externos diretos.
5. **Reservas de Usuário Desativado**: Ao desativar um usuário, suas reservas ativas são canceladas e exemplares reservados são devolvidos ao acervo ou transferidos para o próximo da fila.
6. **Persistência Desacoplada**: Pacote `persistencia` isolado, garantindo que o domínio não execute operações de E/S.

---

## 5. Como Executar

O projeto utiliza **Java 21** e **Maven**.

### Executar a suíte completa de testes (89 testes):
```bash
mvn test
```

### Compilar, verificar e empacotar:
```bash
mvn verify
```

---

## 6. Extensão Autoral da Equipe

### Regra de Negócio: Validação Temporal de Publicação e Unicidade Estrita de Tombamento
* **Motivação e Justificativa das Restrições de Ano:** Em uma biblioteca acadêmica informatizada, o catálogo exige consistência cronológica para garantir a plausibilidade das obras catalogadas. O marco inicial adotado é o ano de **1450**, que assinala a invenção da imprensa de tipos móveis por Johannes Gutenberg e o nascimento da publicação bibliográfica em escala moderna. Manuscritos anteriores a essa data pertencem à categoria de acervos históricos/museológicos especiais, com tombamento diferenciado. No limite superior, admite-se até o ano corrente acrescido de um ano ($anoAtual + 1$), permitindo catalogação de pré-lançamentos editoriais com ISBN antecipado, ao mesmo tempo em que rejeita datas futuras fictícias decorrentes de erros de digitação.
* **Onde está no código:** Entidade `Item.java` (constante `ANO_MINIMO_PUBLICACAO = 1450` e validação no construtor), com testes em `AcervoTest.java` (`anoDePublicacaoInvalidoPassado()` e `anoDePublicacaoInvalidoFuturo()`).
* **Proteção de Invariantes:** As entidades de domínio protegem suas invariantes já em seus construtores, rejeitando valores nulos ou em branco com `DadosInvalidosException`. Setters desnecessários foram removidos para garantir imutabilidade dos dados cadastrais.

---

## 7. Diagrama UML

O diagrama atualizado correspondente à arquitetura e ao código da Entrega 2 encontra-se modelado em [`docs/modelo.puml`](docs/modelo.puml).

---

## 8. Equipe

| Nome | Matrícula | GitHub |
|---|---|---|
| Jean Gustavo | 20210152676 | @jeangsv |
| Igor Leandro | 20240009705 | igorleandrok22 |
| Fábio Adriano Queirolo Taves | 20250104440 | @fabiotaves-alt |
