package servidor;

import objetos_comuns.Cargo; import objetos_comuns.Protocolo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

//USANDO REF https://docs.oracle.com/javase/tutorial/networking/sockets/readingWriting.html 

// visão do servidro sobre o player, guarda o socket
// e prepara para receber e a entrada e saida vindo/indo;
public class Player {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private Cargo cargo;
    private final StringBuilder linhaAtual = new StringBuilder(); // sobrevive a SocketTimeoutException
    private boolean excedeu = false;

    public Player(Socket socket) throws IOException{
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** recebe a linha e vê se o tamanho é válido */
    public String recebe() throws IOException {
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') return finalizarLinha();
            if (linhaAtual.length() <= Protocolo.TAMANHO_MAXIMO_LINHA) {
                linhaAtual.append((char) c);
            } else {
                excedeu = true; // passou do limite: não guarda mais nada até o '\n'
            }
        }
        // conexão fechada: devolve o que sobrou (se houver), depois null
        linhaAtual.setLength(0);
        excedeu = false;
        return null;
    }

    private String finalizarLinha() {
        String linha = linhaAtual.toString();
        boolean longa = excedeu;
        linhaAtual.setLength(0);
        excedeu = false;
        if (!longa && linha.endsWith("\r")) { // aceita \r\n
            linha = linha.substring(0, linha.length() - 1);
        }
        return linha;
    }

    public Socket getSocket() {return socket;}
    public Cargo getCargo() {return cargo;}
    public void setCargo(Cargo cargo) {this.cargo = cargo;}
    public void enviar(String msg) {out.println(msg);}
    public void Fechar() {
        try {socket.close();} catch (IOException ignored) {/* conexao já encerrada, nada a fazer*/}}

}
