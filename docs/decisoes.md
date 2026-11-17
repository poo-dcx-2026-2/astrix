# Registro de Decisões de Design e Arquitetura — Entrega 2

Este documento registra as decisões estruturais de modelagem orientada a objetos, padrões GRASP, design e persistência adotadas na Entrega 2 do Sistema de Biblioteca, conforme exigido pelo enunciado.

---

### Decisão 1: Composição vs. Herança na Modelagem de Item e Exemplar (Aula 11)
* **Contexto:** Uma obra intelectual catalogada no acervo (livro, periódico, mídia) pode ter múltiplas cópias físicas tombadas para empréstimo e reserva na biblioteca.
* **Decisão:** Optou-se pela **composição** estrita entre `Item` e `Exemplar`, onde um `Item` contém e gerencia sua lista de `Exemplar`, e cada `Exemplar` possui uma referência ao `Item` ao qual pertence e seu respectivo `StatusExemplar`.
* **Alternativa rejeitada:** Herança entre classes (fazer `Exemplar extends Item` ou modelar subtipos rígidos de exemplares herdando de itens). Essa alternativa foi rejeitada porque um exemplar físico não é uma especialização do conceito de obra bibliográfica, mas sim uma instância materializada da mesma. Herança geraria duplicação desnecessária dos metadados da obra (título, autor, categoria, ano) em cada exemplar criado e impediria a alteração dinâmica de status.
* **Consequência:** Baixo acoplamento e reuso limpo: os metadados da obra ficam centralizados no `Item`, permitindo adicionar novos exemplares independentes e consultar a disponibilidade agregada (`getExemplaresDisponiveis()`) sem redundância de dados.

---

### Decisão 2: Interfaces e Polimorfismo Restritos a Substituição Real (Aula 11)
* **Contexto:** O contrato define interfaces para os serviços da biblioteca (`AcervoService`, `UsuarioService`, `EmprestimoService`, `RelatorioService`, `Biblioteca`).
* **Decisão:** Manter interfaces apenas nas fronteiras de contrato estipuladas onde há intenção explícita de polimorfismo ou inversão de controle (como a troca da implementação de memória por persistência ou mock em testes). As entidades centrais do modelo (`Item`, `Exemplar`, `Usuario`, `Emprestimo`, `Reserva`) foram mantidas como classes concretas de domínio ricas em comportamento.
* **Alternativa rejeitada:** Criar interfaces genéricas do tipo `IItem`, `IExemplar`, `IEmprestimo` ou hierarquias artificiais de "EntidadeBase" sem substituição comportamental real.
* **Consequência:** Código sem complexidade acidental, atendendo estritamente ao critério da disciplina de que interfaces só existem onde há variação real de comportamento.

---

### Decisão 3: Reorganização GRASP — Padrão Information Expert para Validações e Ciclo de Vida do Empréstimo (Aula 12)
* **Contexto:** Na versão inicial do sistema, campos primitivos ou estruturas esparsas nos serviços poderiam ser tentados para controlar status e prazos de devolução.
* **Decisão:** Pelo padrão **Information Expert (Especialista na Informação)**, a responsabilidade de calcular a data prevista de devolução (somando `PRAZO_EM_DIAS`), verificar se a devolução está em atraso (`dataPrevistaDeDevolucao.isBefore(dataReferencia)`) e validar coerência de datas foi atribuída à entidade de domínio `Emprestimo`.
* **Alternativa rejeitada:** Manter a entidade `Emprestimo` como modelo anêmico (apenas getters/setters) e realizar todos os cálculos de prazos, atrasos e transições de status soltos dentro de métodos de `EmprestimosEmMemoria`.
* **Consequência:** **Alta Coesão** no objeto de domínio e **Baixo Acoplamento** no serviço: as invariantes e regras temporais do empréstimo ficam encapsuladas na entidade especialista, facilitando testes unitários isolados e evitando espalhamento de regras de negócio.

---

### Decisão 4: Reorganização GRASP — Padrão Controller e Information Expert na Fila de Reservas (Aula 12)
* **Contexto:** A devolução de um exemplar precisa saber se existe usuário aguardando na fila de reserva daquele item, separando o exemplar físico para o reservante prioritário ou liberando-o ao acervo.
* **Decisão:** A classe `EmprestimosEmMemoria` atua como **Controller** das operações de empréstimo e reserva, enquanto a entidade `Reserva` atua como **Information Expert** do seu estado de atendimento e vínculo com exemplar separado (`tomboExemplarSeparado`). Ao devolver um exemplar (`devolver`), o serviço busca na fila do item a reserva mais antiga sem exemplar separado e vincula o exemplar a ela; ao cancelar uma reserva (`cancelarReserva`), o exemplar separado é automaticamente repassado para a próxima reserva da fila ou devolvido ao status `DISPONIVEL`.
* **Alternativa rejeitada:** Deixar que `AcervoEmMemoria` ou a classe `Exemplar` gerenciem filas de reservas de usuários e regras de prioridade.
* **Consequência:** As responsabilidades do acervo (itens e exemplares) ficam totalmente desacopladas da lógica de usuários e listas de espera. `AcervoEmMemoria` apenas mantém os exemplares físicos e seus status fundamentais, mantendo cada serviço focado em sua única responsabilidade.

---

### Decisão 5: Padrão Facade — Porta Única de Entrada do Sistema (Aula 13)
* **Contexto:** Na Entrega 3, uma interface gráfica de usuário (GUI) precisará interagir com todo o sistema bibliotecário, demandando um ponto único e seguro de comunicação com o domínio.
* **Decisão:** A classe `BibliotecaInicial` implementa a interface `Biblioteca` atuando estritamente como **Facade (Fachada)** do sistema. Ela agrega e orquestra os subsistemas `AcervoService`, `UsuarioService`, `EmprestimoService`, `RelatorioService` e a persistência em arquivo.
* **Alternativa rejeitada:** Permitir que consumidores externos ou futuras interfaces visuais acessem diretamente coleções em memória, mapas de entidades internas ou instâncias do gerenciador de persistência.
* **Consequência:** Ponto único de entrada desacoplado, garantindo encapsulamento absoluto do domínio: a interface gráfica ou testes falam apenas com a Facade, sem conhecer a estrutura interna dos dados.

---

### Decisão 6: Destino das Reservas de Usuário Desativado (Decisão de Equipe — §3 e §3.1)
* **Contexto:** O enunciado estipula que a consequência da desativação de um usuário sobre suas reservas pendentes é uma decisão a ser deliberada e registrada pela equipe.
* **Decisão:** Ao desativar um usuário através de `desativarUsuario(matricula)`, todas as suas reservas ativas são **canceladas automaticamente** através de `emprestimos.cancelarReservasDoUsuario(matricula)`. Caso qualquer uma dessas reservas já contasse com um exemplar reservado separado para retirada, o exemplar é repassado imediatamente para a próxima reserva sem exemplar da fila ou volta ao status `DISPONIVEL` se não houver fila.
* **Alternativa rejeitada:** Manter a reserva ativa na fila do item para um usuário desativado. Essa alternativa bloquearia exemplares físicos escassos para membros inativos que não teriam permissão de realizar empréstimos, prejudicando os demais usuários ativos da biblioteca.
* **Consequência:** Integridade do acervo garantida e máxima rotatividade das obras: exemplares não ficam retidos para usuários que não podem pegá-los emprestados, beneficiando diretamente os usuários ativos.

---

### Decisão 7: Separação de Persistência e Domínio com Atomicidade no Carregamento (Aula 14 — §2.3)
* **Contexto:** A biblioteca deve persistir seu estado completo em arquivo no diretório `dados/` ou no caminho configurado via propriedade de sistema `biblioteca.dados`, garantindo que nenhuma classe de domínio realize operações de E/S.
* **Decisão:** Criou-se a classe `GerenciadorPersistencia` no pacote `persistencia`, responsável por ler e escrever o snapshot de dados `DadosPersistencia`. O salvamento e carregamento utilizam exclusivamente `try-with-resources` e convertem qualquer `IOException` em `PersistenciaException` preservando a causa com `initCause()`. O carregamento é **atômico**: os dados do arquivo são primeiro lidos e validados estruturalmente; apenas se a integridade for 100% comprovada o estado em memória é substituído. Se o arquivo estiver corrompido, `carregar()` lança `PersistenciaException` sem alterar o estado em memória. Se o arquivo ou diretório estiver ausente, o sistema inicializa com estado vazio sem falhar.
* **Alternativa rejeitada:** Fazer as classes de domínio (`Item`, `Exemplar`, `Usuario`) gravarem a si mesmas diretamente em arquivo ou realizar salvamento parcial que deixasse resíduo de dados caso ocorresse falha no meio do processo.
* **Consequência:** Separação total de responsabilidades (domínio puro sem E/S), robustez e segurança transacional contra falhas e corrupção de arquivos em conformidade rigorosa com a rubrica.
