package servidor;

import objetos_comuns.Cargo;

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

    public Player(Socket socket) throws IOException{
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    public Socket getSocket() {return socket;}
    public Cargo getCargo() {return cargo;}
    public void setCargo(Cargo cargo) {this.cargo = cargo;}
    public void enviar(String msg) {out.println(msg);}
    public String recebe() throws IOException {return in.readLine(); /*Bloqueia até chegar uma linha do cliente (ou null se ele desconectou).*/}
    public void Fechar() {
        try {socket.close();} catch (IOException ignored) {/* conexao já encerrada, nada a fazer*/}}

}
