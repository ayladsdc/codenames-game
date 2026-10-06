package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import objetos_comuns.*;

/*  aqui vai toda a lógica do servidor INICIAl (sem mais de uma sala)
    site de ref inicial do código: https://tcp-udp-ports.website.yandexcloud.net/pt/guides/a-comprehensive-guide-on-opening-a-tcp-connection-in-java-step-by-step-instructions/
    por agora, tem pouco uso de redes mesmo, só na abertura 
    
    recebe só 4 cliente por agr, um socket pra cada */

public class ServidorCodenames {
    private static final int PORTA = Protocolo.PORTA_PADRAO;

    private final int port;

    public ServidorCodenames(int port){
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        int port = PORTA;
        if(args.length>0){
            port = Integer.parseInt(args[0]); // se recebe outra porta, roda nela
        }
        new ServidorCodenames(port).run();
    }

    /** Começa o processo de aceitar players e chama o inicio do jogo logo depois */
    public void run() throws IOException{

        System.out.println("Servidor codenames iniciado");

        try (ServerSocket servidor = new ServerSocket(port)){ // abriu o servidor
            System.out.println("Aguardando 4 jogadores na porta " + port);
            Lobby lobby = new Lobby(servidor);
            Map<Cargo, Player> jogadores = lobby.aguardarJogadores();
    
            System.out.println("Todos os jogadores conectados! Iniciando Partida");
            JogoServidor jogoServidor = new JogoServidor(jogadores);

            jogoServidor.iniciarJogo();
        }
    }
}
