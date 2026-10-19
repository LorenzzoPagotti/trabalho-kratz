import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class Servidor {
    private static final int PORTA = 5000;
    private static final CopyOnWriteArrayList<Jogador> jogadores =
            new CopyOnWriteArrayList<>();
    private static final Object travaJogo = new Object();
    private static int numeroSecreto;
    private static boolean encerrado = false;
    private static String mensagemFinal;

    public static void main(String[] args) throws IOException {
        // TODO 1: sortear UMA VEZ um inteiro de 0 a 1000, inclusive.
        numeroSecreto = new Random().nextInt(1001);

        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("Servidor iniciado na porta " + PORTA + ". Aguardando jogadores...");
            while (true) {
                Socket socket = servidor.accept();
                // TODO 2: criar um Jogador e iniciar uma nova Thread.
                try {
                    Jogador jogador = new Jogador(socket);
                    Thread thread = new Thread(jogador);
                    thread.start();
                } catch (IOException e) {
                    System.out.println("Erro ao iniciar conexão com cliente: " + e.getMessage());
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private static void enviarParaTodos(String mensagem) {
        // TODO 3: percorrer jogadores e chamar enviar(mensagem).
        for (Jogador jogador : jogadores) {
            jogador.enviar(mensagem);
        }
    }

    private static class Jogador implements Runnable {
        private final Socket socket;
        private final PrintWriter saida;
        private final String ip;
        private final String hostname;
        private final int portaRemota;

        Jogador(Socket socket) throws IOException {
            this.socket = socket;
            this.saida = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);
            this.ip = socket.getInetAddress().getHostAddress();
            this.hostname = socket.getInetAddress().getHostName();
            this.portaRemota = socket.getPort();
        }

        // Evita que duas threads escrevam simultaneamente neste cliente.
        synchronized void enviar(String mensagem) {
            saida.println(mensagem);
        }

        void fecharConexao() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void run() {
            try (Socket conexao = socket;
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(
                         conexao.getInputStream(), StandardCharsets.UTF_8))) {

                // TODO 4: dentro de synchronized(travaJogo):
                // - Se encerrado, enviar mensagemFinal e retornar.
                // - Caso contrário, adicionar este jogador à lista e dar boas-vindas.
                synchronized (travaJogo) {
                    if (encerrado) {
                        enviar(mensagemFinal);
                        return;
                    }
                    jogadores.add(this);
                    System.out.println("Novo jogador conectado: IP=" + ip + " HOST=" + hostname + " PORTA=" + portaRemota);
                    enviar("BEM_VINDO Digite um número entre 0 e 1000 (ou SAIR para encerrar)");
                }

                String linha;
                while ((linha = entrada.readLine()) != null) {
                    String texto = linha.trim();

                    // TODO 5: aceitar SAIR; converter o palpite para inteiro.
                    // Tratar texto inválido e valores fora de 0 a 1000.
                    if (texto.equalsIgnoreCase("SAIR")) {
                        System.out.println("Jogador solicitou saída: IP=" + ip + " PORTA=" + portaRemota);
                        break;
                    }

                    int palpite;
                    try {
                        palpite = Integer.parseInt(texto);
                    } catch (NumberFormatException e) {
                        enviar("ERRO Digite um inteiro entre 0 e 1000");
                        continue;
                    }

                    if (palpite < 0 || palpite > 1000) {
                        enviar("ERRO Digite um inteiro entre 0 e 1000");
                        continue;
                    }

                    // TODO 6: dentro de synchronized(travaJogo):
                    // - Verificar encerrado ANTES de comparar o palpite.
                    // - Se palpite < numeroSecreto: enviar MAIOR.
                    // - Se palpite > numeroSecreto: enviar MENOR.
                    // - Se igual: marcar encerrado = true, construir mensagemFinal
                    //   com VENCEDOR, ip, hostname e número; enviar para todos.
                    // - Ao encerrar, fechar sockets de todos os jogadores para
                    //   liberar as threads bloqueadas em readLine().
                    // Somente o PRIMEIRO acerto pode definir o vencedor.
                    synchronized (travaJogo) {
                        if (encerrado) {
                            break;
                        }

                        if (palpite < numeroSecreto) {
                            enviar("MAIOR");
                        } else if (palpite > numeroSecreto) {
                            enviar("MENOR");
                        } else {
                            encerrado = true;
                            enviar("ACERTOU");
                            mensagemFinal = "VENCEDOR IP=" + ip + " HOST=" + hostname + " NUMERO=" + numeroSecreto;
                            System.out.println("Partida encerrada! " + mensagemFinal + " (PORTA=" + portaRemota + ")");
                            enviarParaTodos(mensagemFinal);

                            for (Jogador j : jogadores) {
                                j.fecharConexao();
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Conexão encerrada: " + ip + ":" + portaRemota);
            } finally {
                // TODO 7: remover este jogador da lista.
                synchronized (travaJogo) {
                    jogadores.remove(this);
                }
            }
        }
    }
}
