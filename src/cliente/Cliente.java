package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import objetos_comuns.Protocolo;
import objetos_comuns.RenderizadorTabuleiro;

public class Cliente {
    private static EstadoCliente estado = new EstadoCliente();
    private static FluxoLobby lobby;
    private static FluxoTurno turno;
    private static RenderizadorTabuleiro renderizador = new RenderizadorTabuleiro(); // Adicione esta linha
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.PORTA_PADRAO;

        try (Socket socket = new Socket(host, porta)) {
            System.out.println("Conectado ao servidor " + host + ":" + porta);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // Inicializamos o lobby passando o 'out' (para ele poder enviar o comando CARGO <NOME>)
            lobby = new FluxoLobby(out, estado);
            turno = new FluxoTurno(out, estado, renderizador);

            // uma thread separada fica só  escutando o servidor e traduzindo o que chega
            Thread leitor = new Thread(() -> ouvirServidor(in));
            leitor.setDaemon(true);
            leitor.start();

            // a thread principal fica lendo o teclado e mandando pro servidor
            enviarDoTeclado(out);
        }

        System.out.println("Conexão encerrada.");
    }

    private static void ouvirServidor(BufferedReader in) {
        try {
            String linha;
            while ((linha = in.readLine()) != null) {

                if (linha.equals(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.PARTIDA_CHEIA)) {
                    System.out.println("Erro: A partida já está cheia! (Digite 'sair' para fechar)");
                    return;
                }

                try {
                    if (lobby.processarMensagemServidor(linha)) continue;   // menu de cargos
                    if (turno.processarMensagemServidor(linha)) continue;   // tabuleiro, turnos, placar, fim de jogo

                    System.out.println(Tradutor.paraCliente(linha));         // INFO, ERRO, JOGO iniciado/encerrado...
                } catch (RuntimeException e) {                               // linha estranha do servidor: ignora e segue
                    System.out.println("[linha ignorada] " + linha);
                }

                if (linha.equals(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO)) {
                    sairDoJogo(0);                                           // a partida acabou: fecha o cliente
                }
            }

            // o servidor fechou a conexão: se a partida já tinha acabado é um fim normal
            System.out.println(">>> Servidor encerrou a conexao.");
            sairDoJogo(estado.getFase() == EstadoCliente.Fase.FIM ? 0 : 1);
        } catch (IOException e) {
            System.out.println(">>> Conexao com o servidor perdida.");
            sairDoJogo(estado.getFase() == EstadoCliente.Fase.FIM ? 0 : 1);
        }
    }

    /** Fecha o programa. A thread principal está presa esperando o teclado, então não terminaria sozinha. */
    private static void sairDoJogo(int codigo) {
        System.out.println("Conexão encerrada.");
        System.exit(codigo);
    }
    
    private static void enviarDoTeclado(PrintWriter out) {
        Scanner teclado = new Scanner(System.in, "UTF-8");
        while (teclado.hasNextLine()) {
            String linha = teclado.nextLine().trim();
            if (linha.isEmpty()) continue;
            if (linha.equalsIgnoreCase("sair")) break;
            if (linha.equalsIgnoreCase("ajuda")) { turno.mostrarAjuda(); continue; }

            if (!estado.temCargo()) lobby.processarEntradaTeclado(linha);
            else turno.processarEntradaTeclado(linha);
        }
        teclado.close();
    }

}
