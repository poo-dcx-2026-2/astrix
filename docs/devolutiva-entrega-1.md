# Devolutiva da Entrega 1 — Dev-java

## Nota: 8,5 de 10 (85 de 100 pontos)

Código e testes muito bons, com apenas uma falha pequena. A nota foi reduzida principalmente pela ausência de colaboração e pela concentração em um integrante.

Avaliei o último commit da branch `main` até 12/10/2026 às 23h59 (o prazo original, 05/10, mais uma semana de extensão), pela leitura do código, dos testes, do histórico de commits, dos pull requests e da documentação. 

## Pontos por critério

| Critério | O que é avaliado | Pontos | Nível |
|---|---|---|---|
| 4.1 | Correção funcional e aderência ao contrato | 19 de 20 | Adequado |
| 4.2 | Modelagem OO, responsabilidades, coesão e acoplamento | 23 de 25 | Excelente |
| 4.3 | Testes e evidências de depuração | 14 de 15 | Excelente |
| 4.4 | Robustez, exceções e casos-limite | 8 de 10 | Adequado |
| 4.5 | Evolução, refatoração e qualidade do código | 9 de 10 | Excelente |
| 4.6 | Git, build, pipeline e documentação | 6 de 10 | Parcial |
| 4.7 | Processo, checkpoints, explicação e autoria | 6 de 10 | Parcial |
| | **Total** | **85 de 100** | |

## Justificativa (onde tiro pontos)

- Ausência de ordenação por tombo em listarExemplares.

- código/matrícula em branco nas consultas geram RecursoNaoEncontradoException em vez de DadosInvalidosException.

- Ordenação dos exemplares também ficou sem proteção por teste.

- Não houve branch/PR nem revisão por outro integrante, requisito explícito da entrega.

- Jean fez os sete commits, Fábio aparece apenas no commit inicial e não consta na declaração de IA. Não há revisão de colega.

## Checklist de correções

- [ ] **Ordenar a resposta de `listarExemplares` pelo tombo.** O tombo é o número de registro de cada exemplar. O enunciado pede os exemplares "ordenados por tombo", e hoje a lista sai na ordem de cadastro: foi o único teste oculto que falhou. Uma forma de fazer: antes do `return`, ordenar com `lista.sort(Comparator.comparing(ExemplarView::tombo))`. Para conferir, escrevam um teste que cadastra os tombos fora de ordem (por exemplo `T3`, `T1`, `T2`) e verifica que a lista volta `T1`, `T2`, `T3`.
- [ ] **Lançar `DadosInvalidosException` para código ou matrícula em branco nas consultas.** Hoje uma consulta com `""` ou `"  "` lança `RecursoNaoEncontradoException`, que significa "o dado faz sentido, mas não existe". Entrada em branco é outro erro: "o dado não faz sentido". Validem a entrada no começo do método e escrevam um teste para cada caso.
- [ ] **Trabalhar por branch e pull request.** Em vez de fazer commit direto na `main`, cada tarefa nasce numa *branch* própria (`git switch -c nome-da-tarefa`), é enviada com `git push -u origin nome-da-tarefa` e entra na `main` por um pull request revisado por outro integrante (ver o item de revisão).
- [ ] **Registrar a revisão de colega no pull request.** Pull request (PR) é o pedido para juntar uma *branch* (linha de trabalho separada) na `main`. A revisão só conta como evidência se aparecer no GitHub: na aba *Files changed* do PR, o colega que **não** escreveu o código clica em *Review changes*, escreve pelo menos um comentário sobre o que leu e escolhe *Approve* ou *Request changes*. Mensagem de commit dizendo "revisado" não substitui isso.
- [ ] **Dividir a implementação entre os integrantes.** Cada integrante precisa ter commits de código e de testes com o próprio usuário do GitHub. Uma divisão simples: um integrante cuida do acervo e o outro dos usuários, e cada um revisa o PR do outro. Combinem também que cada um consegue explicar a parte do colega, porque isso é perguntado no checkpoint.
- [ ] **Completar a declaração de uso de IA com todos os integrantes.** Hoje o Fábio não consta nela.
- [ ] **Rever os *setters* e as regras extras de ano.** Removam os `setAlgo` que não são usados por nenhuma operação do contrato, e expliquem no README por que o ano tem as restrições adicionais que vocês criaram.
