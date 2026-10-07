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

            boolean lobbyIntercepta = lobby.processarMensagemServidor(linha);

            if (!lobbyIntercepta) {
                String[] partes = linha.split(" ");
                String comando = partes[0];

                if (comando.equals(Protocolo.Servidor.TABULEIRO_AGENTE) || comando.equals(Protocolo.Servidor.TABULEIRO_MESTRE) || comando.equals(Protocolo.Servidor.TABULEIRO_FINAL)) {    
                    renderizador.guardarTabuleiro(partes);
                            
                    if (comando.equals(Protocolo.Servidor.TABULEIRO_FINAL)) renderizador.desenharTela();
                            
                    } else if (comando.equals(Protocolo.Servidor.REVELAR)) {
                        renderizador.atualizarCarta(partes[1], partes[2]);
                        renderizador.desenharTela();
                            
                    } else if (comando.equals(Protocolo.Servidor.PLACAR)) {
                        renderizador.atualizarPlacar(partes[1], partes[2]);
                        
                    } else if (comando.equals(Protocolo.Servidor.VEZ_DICA) || comando.equals(Protocolo.Servidor.VEZ_PALPITE)) {
                        renderizador.atualizarTurno(Tradutor.paraCliente(linha));
                        renderizador.desenharTela();
                        
                    } else { System.out.println(Tradutor.paraCliente(linha));}
                }
            }

            System.out.println(">>> Servidor encerrou a conexao. (Digite 'sair' para fechar)");
        } catch (IOException e) {
            System.out.println(">>> Conexao com o servidor perdida. (Digite 'sair' para fechar)");
        }
    }

    private static void enviarDoTeclado(PrintWriter out) {
        Scanner teclado = new Scanner(System.in, "UTF-8");
        while (teclado.hasNextLine()) {
            String linha = teclado.nextLine();
            if (linha.isEmpty()) continue;
            if (linha.equalsIgnoreCase("sair")) break;

            if (!estado.temCargo()) lobby.processarEntradaTeclado(linha);   
            else out.println(linha);
        }
        teclado.close();
    }
}
