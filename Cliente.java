import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Cliente {
    private static volatile boolean encerrado = false;

    public static void main(String[] args) throws IOException {
        String servidor = args.length > 0 ? args[0] : "localhost";
        try (Socket socket = new Socket(servidor, 5000);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter saida = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true);
             Scanner teclado = new Scanner(System.in, StandardCharsets.UTF_8.name())) {

            Thread receptor = new Thread(() -> {
                try {
                    // TODO 8: ler continuamente as mensagens do servidor.
                    // Exibir cada linha recebida imediatamente.
                    // Se começar com VENCEDOR, marcar encerrado = true.
                    String linha;
                    while ((linha = entrada.readLine()) != null) {
                        System.out.println(linha);
                        if (linha.startsWith("VENCEDOR")) {
                            encerrado = true;
                            System.out.println("Partida finalizada! Pressione ENTER para sair.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (!encerrado) {
                        System.out.println("Recepção encerrada.");
                    }
                } finally {
                    encerrado = true;
                }
            });
            receptor.setDaemon(true);
            receptor.start();

            System.out.println("Digite um número de 0 a 1000 ou SAIR.");
            // TODO 9: enquanto o jogo estiver ativo, ler linhas do teclado
            // e enviá-las com saida.println(...). Se SAIR, sair do laço.
            // Depois de ler o teclado, verificar encerrado novamente.
            // Scanner pode continuar bloqueado após o anúncio do vencedor:
            // nesse caso, ENTER permite concluir o programa. O anúncio deve
            // aparecer imediatamente, sem depender desse ENTER.
            while (!encerrado && teclado.hasNextLine()) {
                String linha = teclado.nextLine().trim();
                if (encerrado) {
                    break;
                }
                if (linha.equalsIgnoreCase("SAIR")) {
                    saida.println("SAIR");
                    encerrado = true;
                    break;
                }
                saida.println(linha);
            }
        }
    }
}
