# Jogo de Adivinhação com Sockets TCP — Redes de Computadores II

Aplicação cliente-servidor desenvolvida em Java utilizando **Sockets TCP** e **Threads** para disputa simultânea de adivinhação de um número secreto entre `0` e `1000`.

---

## 👥 Integrantes do Grupo

- Lorenzzo Freitas Pagotti
- Kauã Vieira Porto

---

## 📋 Descrição e Funcionamento

1. **Sorteio Único:** Ao iniciar, o `Servidor` sorteia um único número inteiro entre `0` e `1000` (inclusive) usando `Random.nextInt(1001)`.
2. **Múltiplos Clientes (Threads):** Cada conexão aceita pelo `ServerSocket` na porta `5000` é atendida por uma `Thread` independente (`Jogador`), garantindo que clientes ociosos não bloqueiem os demais.
3. **Respostas aos Palpites:**
   - `MAIOR`: o número secreto é maior que o palpite enviado;
   - `MENOR`: o número secreto é menor que o palpite enviado;
   - `ACERTOU`: o cliente descobriu o número secreto;
   - `ERRO Digite um inteiro entre 0 e 1000`: entrada não numérica, vazia ou fora do intervalo `[0, 1000]`.
4. **Vencedor Único e Anúncio Imediato:**
   - A verificação do palpite e a definição do estado `encerrado = true` são protegidas por um bloco `synchronized (travaJogo)`, garantindo que apenas o **primeiro** cliente a acertar seja declarado vencedor, mesmo em caso de envios simultâneos.
   - Assim que há um vencedor, o servidor envia a todos os clientes conectados:
     ```text
     VENCEDOR IP=<ip_do_vencedor> HOST=<hostname_do_vencedor> NUMERO=<numero_secreto>
     ```
   - No `Cliente`, uma thread exclusiva (`receptor`) lê continuamente o `BufferedReader` do socket, permitindo exibir o anúncio do vencedor imediatamente na tela mesmo enquanto a thread principal aguarda digitação no `Scanner`.
5. **Desconexões e Comando `SAIR`:**
   - Se um cliente digitar `SAIR` ou fechar o terminal abruptamente, apenas a sua conexão é encerrada e removida da lista de jogadores ativos, sem interromper o servidor ou a partida para os demais.
   - Caso um novo cliente se conecte após o término da partida, o servidor informa imediatamente a mensagem final do vencedor e encerra a conexão.

---

## 🚀 Como Compilar e Executar

### Pré-requisitos
- **Java JDK** (8 ou superior) instalado e configurado no `PATH`.
- Porta TCP `5000` liberada (caso executado entre máquinas diferentes na rede).

### 1. Compilar os arquivos
No diretório raiz do projeto, execute:

```bash
javac -encoding UTF-8 Servidor.java Cliente.java
```

### 2. Iniciar o Servidor
Em um terminal, inicie o servidor:

```bash
java Servidor
```

### 3. Iniciar os Clientes (3 ou mais terminais)
Abra pelo menos **três novos terminais** e execute em cada um:

```bash
java Cliente localhost
```

Para conectar a partir de outra máquina na mesma rede local, substitua `localhost` pelo endereço IP da máquina do servidor:

```bash
java Cliente 192.168.1.10
```

---

## 🧪 Roteiro de Testes Realizados

1. **Três ou mais clientes simultâneos:** Três clientes conectados ao mesmo tempo enviando palpites independentes e recebendo `MAIOR` ou `MENOR` individualmente.
2. **Anúncio em cliente aguardando digitação:** Um cliente conectado que permaneceu parado no prompt de digitação recebeu imediatamente a mensagem `VENCEDOR IP=... HOST=... NUMERO=...` assim que outro jogador acertou.
3. **Entradas inválidas e fora do intervalo:** Envio de textos (`abc`), linhas vazias e números fora de `0..1000` (`-5`, `1500`) retornaram `ERRO Digite um inteiro entre 0 e 1000` sem derrubar o servidor.
4. **Saída individual (`SAIR`):** Um cliente enviou `SAIR` e teve apenas sua sessão encerrada enquanto os demais continuaram na disputa.
5. **Acerto simultâneo (Concorrência):** Dois clientes enviando o número correto concorrentemente resultaram em apenas um vencedor anunciado de forma idêntica para todos os participantes, graças à exclusão mútua em `synchronized (travaJogo)`.
6. **Conexão pós-partida:** Clientes que se conectam após o término da partida recebem imediatamente o anúncio do vencedor e têm a conexão finalizada.

---

## 💡 Respostas para a Apresentação

- **Por que o cliente precisa receber mensagens em outra thread?**
  Porque a leitura do teclado (`Scanner.nextLine()`) é uma operação bloqueante. Se o cliente lesse o teclado e o socket na mesma thread, ele ficaria travado esperando o usuário digitar um palpite e só exibiria o anúncio de fim de jogo depois que o usuário pressionasse `ENTER`. Com uma thread dedicada à recepção (`receptor`), qualquer mensagem do servidor é impressa na tela instantaneamente.
- **O que pode acontecer sem sincronização no servidor?**
  Duas condições de corrida (race conditions) podem ocorrer: (1) dois clientes podem acertar quase ao mesmo tempo, ambos lerem `encerrado == false` e o servidor anunciar dois vencedores diferentes; (2) um novo cliente pode entrar exatamente entre o momento do acerto e o envio da mensagem final, ficando em estado inconsistente.
- **Qual é a diferença entre IP, hostname e porta?**
  - **IP (Internet Protocol):** endereço numérico lógico que identifica a interface de rede do dispositivo (ex.: `127.0.0.1` ou `192.168.1.20`).
  - **Hostname:** nome legível associado ao dispositivo na rede/DNS (ex.: `pc-lab-03`). Caso o DNS reverso não esteja configurado na rede, `getHostName()` retorna o próprio endereço IP.
  - **Porta TCP:** número de 16 bits (ex.: `5000` no servidor, ou porta efêmera no cliente) que identifica o processo/aplicação específico dentro da máquina, permitindo distinguir múltiplos clientes rodando no mesmo computador.
- **O que acontece quando `readLine()` retorna `null`?**
  Significa que o outro lado da conexão TCP enviou o pacote de término (`FIN` / EOF), indicando que o socket foi fechado normalmente pelo cliente (ou pelo servidor). O laço `while ((linha = entrada.readLine()) != null)` termina de forma limpa e o bloco `finally` remove o jogador da lista.
