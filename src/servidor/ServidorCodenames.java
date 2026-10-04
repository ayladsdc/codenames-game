package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import objetos_comuns.*;

/*  aqui vai toda a lógica do servidor INICIAl (sem mais de uma sala)
    site de ref inicial do código: https://tcp-udp-ports.website.yandexcloud.net/pt/guides/a-comprehensive-guide-on-opening-a-tcp-connection-in-java-step-by-step-instructions/
    por agora, tem pouco uso de redes mesmo, só na abertura 
    
    recebe só 4 cliente por agr, um socket pra cada */

public class ServidorCodenames {
    private static final int PORTA = Protocolo.PORTA_PADRAO;

    private final int port;
    private Map<Cargo, Player> players = new EnumMap<>(Cargo.class); //pra mapear tds os jogadpres e seus cargos
    private Tabuleiro tabuleiro;

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

        try ( ServerSocket servidor = new ServerSocket(port)){ // abriu o servidor
            System.out.println("Aguardando 4 jogadores na porta " + port);
            aceitaPlayers(servidor);
        }

        System.out.println("Todos os jogadores conectados! Iniciando Partida");
        broadcast(Protocolo.Servidor.INFO + " Todos jogadores prontos! Iniciando partida");

        tabuleiro = new Tabuleiro();
        enviaTabuleiros();

        //inicia o jogo: função iniciarJogo()
        //se tiver problema fecha (ent faz trys e catchs)
    }

    /** Função que exibe o tabuleiro para os diferentes cargos. Usa as funções visaoAgente() e visaoMestre() da classe tabuleiro*/
    private void enviaTabuleiros(){
        //Exibe tabuleiros  pros agentes
        players.get(Cargo.AZUL_AGENTE).enviar(tabuleiro.visaoAgente());
        players.get(Cargo.VERMELHA_AGENTE).enviar(tabuleiro.visaoAgente());

        //Exibi tabuleiros pros mestres
        players.get(Cargo.AZUL_MESTREESPIAO).enviar(tabuleiro.visaoMestre());
        players.get(Cargo.VERMELHA_MESTREESPIAO).enviar(tabuleiro.visaoMestre());
    }

/** Função que conecta os jpgadores e recebe o cargo que eles querem (da pra refinar, a gente faz se der tempo) */
    private void aceitaPlayers(ServerSocket server) throws IOException {
        List<Cargo> disponivel = new  ArrayList<>(Arrays.asList(Cargo.values()));

        while(!disponivel.isEmpty()){
            Socket socket = server.accept();
            Player player = new Player(socket);
            System.out.println("Cliente conectado: " + socket.getInetAddress());

            player.enviar(Protocolo.Servidor.INFO + " Papeis disponiveis: " + disponivel);
            // aceitou a conexão e agr p player seleciona o cargo q qr
            Cargo escolha = null;
            while(escolha==null){
            player.enviar(Protocolo.Servidor.INFO + " Escolha um cargo (" + Protocolo.Cliente.CARGO + "<NOME>):");
            String resposta = player.recebe();
            if(resposta==null) { //vazou e não escolheu o cargo
                System.out.println("Cliente desconectou antes de escolher papel.");
                player.Fechar();
                break;///////////////////////////////////////////////////////////////////////////////////////////////////////////////
            }

            String[] parts = resposta.trim().split("\\s+",2);
            if(parts.length ==2 && parts[0].equalsIgnoreCase(Protocolo.Cliente.CARGO)){///////compara 
                
                Cargo resquisitado = null; 

                try{
                    resquisitado = Cargo.valueOf(parts[1].toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.enviar(Protocolo.Servidor.ERRO + " Papel invalido");
                }

                if (resquisitado == null) {
                    player.enviar(Protocolo.Servidor.ERRO + " Papel invalido");
                } else if (!disponivel.contains(resquisitado)) {
                    player.enviar(Protocolo.Erro.CARGO_OCUPADO + " " + resquisitado);
                } else {
                    escolha = resquisitado;
                    // atauliza as vars e coloca o player no map
                    player.setCargo(escolha);
                    disponivel.remove(escolha);
                    players.put(escolha, player);
                    player.enviar(Protocolo.Jogo.BEM_VINDO + " " + escolha);
                    System.out.println(escolha + " conectado (" + (4 - disponivel.size()) + "/4).");
                }

            } else {
                player.enviar((Protocolo.Servidor.ERRO + " Comando invalido. Use: " + Protocolo.Cliente.CARGO + " <NOME>"));
            }

            }
        }
    }

    private void broadcast(String msg) {
        for (Player p : players.values()) {
            p.enviar(msg);
        }
    }

}
