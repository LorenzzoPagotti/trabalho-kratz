# Jogo de Adivinhação com Sockets TCP — Redes de Computadores II

Aplicação cliente-servidor desenvolvida em Java utilizando **Sockets TCP** e **Threads** para disputa simultânea de adivinhação de um número secreto entre `0` e `1000`.

---

##  Integrantes do Grupo

- Lorenzzo Freitas Pagotti
- Kauã Vieira Porto

---

##  Descrição e Funcionamento

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

##  Como Compilar e Executar

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

##  Execução com Docker (Servidor em Contêiner)

O **servidor** roda obrigatoriamente dentro de um contêiner Docker; o **cliente** roda fora do contêiner (no host Linux/WSL ou no Windows).

### Pré-requisitos
- Linux com Docker instalado **ou** Windows com WSL 2 (Ubuntu) + Docker (Docker Desktop com integração WSL ou Docker Engine dentro do Ubuntu).
- Java JDK no host, apenas para compilar/rodar o **cliente**.

### Estrutura do projeto

```text
trabalho-kratz/
├── Servidor.java      # servidor TCP (porta 5000)
├── Cliente.java       # cliente TCP
├── Dockerfile         # receita da imagem do servidor
├── .dockerignore      # arquivos ignorados no build
├── README.md
└── capturas/          # prints da execução
```

### O Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk                          # 1. imagem base Java
WORKDIR /app                                         #    pasta de trabalho no contêiner
COPY Servidor.java Cliente.java ./                   # 2. copia o código-fonte
RUN javac -encoding UTF-8 Servidor.java Cliente.java # 3. compila dentro da imagem
EXPOSE 5000                                          # 4. porta TCP do servidor
CMD ["java", "-Dfile.encoding=UTF-8", "Servidor"]    # 5. inicia o servidor automaticamente
```

### Passo 1 — Construir a imagem (`docker build`)
Na raiz do projeto:

```bash
docker build -t jogo-adivinhacao-lorenzzo-kaua:1.0 .
```

- `-t` define o nome (tag) da imagem, identificando o jogo e o grupo.
- `.` indica que o contexto do build (e o `Dockerfile`) está na pasta atual.

Conferir se a imagem foi criada:

```bash
docker images jogo-adivinhacao-lorenzzo-kaua
```

### Passo 2 — Executar o servidor em um contêiner (`docker run`)

```bash
docker run -d --name servidor-adivinhacao -p 5000:5000 jogo-adivinhacao-lorenzzo-kaua:1.0
```

- `-d` executa em segundo plano (detached).
- `--name` dá um nome ao contêiner.
- `-p 5000:5000` publica a porta: `porta_do_host:porta_do_contêiner`.

Verificar se está rodando e ver os logs do servidor:

```bash
docker ps
docker logs -f servidor-adivinhacao
```

Saída esperada nos logs:

```text
Servidor iniciado na porta 5000. Aguardando jogadores...
```

> Alternativa para ver o servidor em primeiro plano: `docker run -it --rm --name servidor-adivinhacao -p 5000:5000 jogo-adivinhacao-lorenzzo-kaua:1.0`

### Passo 3 — Executar o cliente (fora do contêiner)
Em outro terminal, na raiz do projeto:

```bash
javac -encoding UTF-8 Cliente.java
java Cliente localhost
```

Exemplo de partida (busca binária):

```text
Digite um número de 0 a 1000 ou SAIR.
BEM_VINDO Digite um número entre 0 e 1000 (ou SAIR para encerrar)
500
MENOR
250
MAIOR
375
MENOR
...
312
ACERTOU
VENCEDOR IP=172.17.0.1 HOST=172.17.0.1 NUMERO=312
Partida finalizada! Pressione ENTER para sair.
```

> **Observação:** o IP do vencedor aparece como o gateway da rede Docker (ex.: `172.17.0.1`), pois a conexão do cliente chega ao contêiner através do mapeamento de portas (`-p`). Isso comprova que a comunicação TCP está passando pela rede do Docker.

- **Windows + WSL com Docker Desktop:** o cliente pode rodar no PowerShell/CMD do Windows com `java Cliente localhost`.
- **Outra máquina da rede:** `java Cliente <IP_DA_MAQUINA_HOST>` (porta 5000 liberada no firewall).

### Passo 4 — Nova partida, parar e remover

O servidor sorteia o número uma única vez. Para começar uma nova partida, reinicie o contêiner:

```bash
docker restart servidor-adivinhacao   # nova partida (novo número)
docker stop servidor-adivinhacao      # para o contêiner
docker rm servidor-adivinhacao        # remove o contêiner
docker rmi jogo-adivinhacao-lorenzzo-kaua:1.0   # remove a imagem (opcional)
```

### Resumo dos comandos Docker

| Comando | Para que serve |
|---|---|
| `docker build -t <nome> .` | Constrói a imagem a partir do `Dockerfile` |
| `docker images` | Lista as imagens criadas |
| `docker run -d --name <c> -p 5000:5000 <imagem>` | Cria e inicia o contêiner publicando a porta TCP |
| `docker ps` | Lista os contêineres em execução |
| `docker logs -f <c>` | Mostra a saída (logs) do servidor |
| `docker exec -it <c> bash` | Abre um terminal dentro do contêiner |
| `docker restart <c>` | Reinicia o contêiner (nova partida) |
| `docker stop <c>` / `docker rm <c>` | Para / remove o contêiner |
| `docker rmi <imagem>` | Remove a imagem |

### Capturas de tela
As evidências ficam na pasta [`capturas/`](capturas/):

1. `01-docker-build.png` — saída do `docker build`.
2. `02-docker-images.png` — imagem `jogo-adivinhacao-lorenzzo-kaua` listada.
3. `03-docker-ps.png` — contêiner `servidor-adivinhacao` em execução com a porta `0.0.0.0:5000->5000/tcp`.
4. `04-docker-logs.png` — logs do servidor (início, conexões e vencedor).
5. `05-cliente-jogo.png` — cliente enviando palpites, recebendo `MAIOR`/`MENOR` e `ACERTOU`.

### Gerar o .zip de entrega

```bash
zip -r trabalho-docker-lorenzzo-kaua.zip Servidor.java Cliente.java Dockerfile .dockerignore README.md capturas/
```

---

##  Roteiro de Testes Realizados

1. **Três ou mais clientes simultâneos:** Três clientes conectados ao mesmo tempo enviando palpites independentes e recebendo `MAIOR` ou `MENOR` individualmente.
2. **Anúncio em cliente aguardando digitação:** Um cliente conectado que permaneceu parado no prompt de digitação recebeu imediatamente a mensagem `VENCEDOR IP=... HOST=... NUMERO=...` assim que outro jogador acertou.
3. **Entradas inválidas e fora do intervalo:** Envio de textos (`abc`), linhas vazias e números fora de `0..1000` (`-5`, `1500`) retornaram `ERRO Digite um inteiro entre 0 e 1000` sem derrubar o servidor.
4. **Saída individual (`SAIR`):** Um cliente enviou `SAIR` e teve apenas sua sessão encerrada enquanto os demais continuaram na disputa.
5. **Acerto simultâneo (Concorrência):** Dois clientes enviando o número correto concorrentemente resultaram em apenas um vencedor anunciado de forma idêntica para todos os participantes, graças à exclusão mútua em `synchronized (travaJogo)`.
6. **Conexão pós-partida:** Clientes que se conectam após o término da partida recebem imediatamente o anúncio do vencedor e têm a conexão finalizada.

---

##  Respostas para a Apresentação

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
