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

public class Cliente {
    
    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int porta = args.length > 1 ? Integer.parseInt(args[1]) : Protocolo.PORTA_PADRAO;

        try (Socket socket = new Socket(host, porta)) {
            System.out.println("Conectado ao servidor " + host + ":" + porta);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // uma thread separada só fica escutando o servidor e traduzindo o que chega
            Thread leitor = new Thread(() -> ouvirServidor(in));
            leitor.setDaemon(true);
            leitor.start();

            // a thread principal fica lendo o teclado e mandando pro servidor
            enviarDoTeclado(out);
        }

        System.out.println("Conexao encerrada.");
    }

    private static void ouvirServidor(BufferedReader in) {
        try {
            String linha;
            while ((linha = in.readLine()) != null) {
                System.out.println(Tradutor.paraCliente(linha));
            }
            System.out.println(">>> Servidor encerrou a conexao.");
        } catch (IOException e) {
            System.out.println(">>> Conexao com o servidor perdida: " + e.getMessage());
        }
    }

    private static void enviarDoTeclado(PrintWriter out) {
        Scanner teclado = new Scanner(System.in, "UTF-8");
        while (teclado.hasNextLine()) {
            String linha = teclado.nextLine();
            out.println(linha);
            if (linha.trim().equalsIgnoreCase("sair")) {
                teclado.close();
                return;
            }
        }
        teclado.close();
    }
}
