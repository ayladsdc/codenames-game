package servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.EnumMap;
import java.util.Map;

import logica_jogo.Evento;
import logica_jogo.Fase;
import logica_jogo.Partida;
import logica_jogo.Resultado;
import objetos_comuns.*;

/*  aqui vai toda a lógica do servidor INICIAl (sem mais de uma sala)
    site de ref inicial do código: https://tcp-udp-ports.website.yandexcloud.net/pt/guides/a-comprehensive-guide-on-opening-a-tcp-connection-in-java-step-by-step-instructions/
    por agora, tem pouco uso de redes mesmo, só na abertura 
    
    recebe só 4 cliente por agr, um socket pra cada */

public class ServidorCodenames {
    private static final int PORTA = Protocolo.PORTA_PADRAO;

    private final int port;
    private volatile boolean partidaEncerrada = false;
    private Map<Cargo, Player> players = new EnumMap<>(Cargo.class); //pra mapear tds os jogadpres e seus cargos
    private Tabuleiro tabuleiro;
    private Partida partida;

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
            this.players = lobby.aguardarJogadores();
    
            System.out.println("Todos os jogadores conectados! Iniciando Partida");
            broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.INICIADO);
    
            tabuleiro = new Tabuleiro();
            enviaTabuleiros();
            iniciarJogo();
        }

        
    }

    /** Função que exibe o tabuleiro para os diferentes cargos. Usa as funções visaoAgente() e visaoMestre() da classe tabuleiro*/
    private void enviaTabuleiros(){
        String visaoAgente = codificarTabuleiro(false);
        String visaoMestre = codificarTabuleiro(true);

        players.get(Cargo.AZUL_AGENTE).enviar(Protocolo.Servidor.TABULEIRO_AGENTE + " " + visaoAgente);
        players.get(Cargo.VERMELHA_AGENTE).enviar(Protocolo.Servidor.TABULEIRO_AGENTE + " " + visaoAgente);

        players.get(Cargo.AZUL_MESTREESPIAO).enviar(Protocolo.Servidor.TABULEIRO_MESTRE + " " + visaoMestre);
        players.get(Cargo.VERMELHA_MESTREESPIAO).enviar(Protocolo.Servidor.TABULEIRO_MESTRE + " " + visaoMestre);
    }
    
    private void iniciarJogo() throws IOException {
        partida = new Partida(tabuleiro);

        while(!partidaEncerrada && partida.getFase()!= Fase.FIM_DE_JOGO ){  // enquanto jogo não acabar, ou ta na fase de dica ou na de palpite
            while(partida.getFase()!= Fase.FIM_DE_JOGO){  // enquanto jogo não acabar, ou ta na fase de dica ou na de palpite
                if(partida.getFase()== Fase.AGUARDANDO_DICA){
                    processarTurnoDica(partida.cargoMestreDaVez());
                } else {
                    processarTurnoPalpite(partida.cargoAgenteDaVez());
                }
            }
        }

        if (partidaEncerrada) {
            System.out.println("Partida encerrada por desconexao.");
        } else {
            System.out.println("Partida encerrada! Vencedor: " + partida.getVencedor()
                    + " (" + partida.getMotivoFim() + ")");
            broadcast(Protocolo.Servidor.TABULEIRO_FINAL + " " + codificarTabuleiro(true));
            broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO);
            desconectarTodos();
        }
    }

    private String codificarTabuleiro(boolean mostrarCor) {
        StringBuilder sb = new StringBuilder();
        for (Carta c : tabuleiro.getCartasJogo()) {
            if (sb.length() > 0) {
                sb.append(Protocolo.SEPARADOR);
            }
            sb.append(codificarCarta(c, mostrarCor));
        }
        return sb.toString();
    }

    private String codificarCarta(Carta c, boolean mostrarCor) {
        String cor = (mostrarCor || c.estaRevelada()) ? corParaProtocolo(c.getCor()) : Protocolo.CARTA_OCULTA;
        String revelada = c.estaRevelada() ? "1" : "0";
        return c.getPosicao() + Protocolo.SEPARADOR_CAMPOS_CARTA
                + c.getPalavra().replace(" ", "_") + Protocolo.SEPARADOR_CAMPOS_CARTA
                + cor + Protocolo.SEPARADOR_CAMPOS_CARTA
                + revelada;
    }

    private void processarTurnoDica(Cargo cargoDaVez) throws IOException {
        Player jogador = players.get(cargoDaVez);
        broadcast(Protocolo.Servidor.VEZ_DICA + " " + cargoDaVez.time());
        //coloca tempo maximo ?????

        //recebe a entrada do jogador, fica em loop ate a dica estar correta
        while(true){
            String linha;
            try {
                linha = jogador.recebe();
            } catch (IOException e) {
                tratarDesconexao(jogador);
                return;
            }
            
            if (linha == null) {
                tratarDesconexao(jogador);
                return;
            }


            linha = linha.trim();
            if (linha.isEmpty()) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.LINHA_VAZIA);
                continue;
            }
            if (linha.length() > Protocolo.TAMANHO_MAXIMO_LINHA) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.LINHA_LONGA);
                continue;
            }

            String[] partes = linha.split("\\s+", 3);
            if (!partes[0].equalsIgnoreCase(Protocolo.Cliente.DICA)) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.COMANDO_DESCONHECIDO);
                continue;
            }
            if (partes.length != 3) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.ARGUMENTOS_INVALIDOS);
                continue;
            }

            int numero;
            try {
                numero = Integer.parseInt(partes[2]);
            } catch (NumberFormatException e) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.NUMERO_INVALIDO);
                continue;
            }

            Resultado r = partida.darDica(cargoDaVez, partes[1], numero);
            if(!r.deuSucesso()){
                jogador.enviar(Protocolo.Servidor.ERRO + " " + r.getErro());
                continue; // reinicia o loop até dar sucesso
            }

            Evento.DicaDada e = (Evento.DicaDada) r.getEventos().get(0);
            jogador.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.DICA_VALIDA);
            broadcast(Protocolo.Servidor.DICA_DADA + " " + e.palavra + " " + e.numero);
            return;
        }

    }

        private void processarTurnoPalpite(Cargo cargoDaVez) throws IOException {
        Player jogador = players.get(cargoDaVez);
        broadcast(Protocolo.Servidor.VEZ_PALPITE + " " + cargoDaVez.time() + " " + partida.getPalpitesRestantes());
        //coloca tempo maximo ?????

        //enquanto tiver aguardando palpite
        while (partida.getFase() == Fase.AGUARDANDO_PALPITE){
            String linha;
            try {
                linha = jogador.recebe();
            } catch (IOException e) {
                tratarDesconexao(jogador);
                return;
            }
            linha = linha.trim();
            if (linha.isEmpty()) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.LINHA_VAZIA);
                continue;
            }
            if (linha.length() > Protocolo.TAMANHO_MAXIMO_LINHA) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.LINHA_LONGA);
                continue;
            }

            String[] partes = linha.split("\\s+");

            if (partes[0].equalsIgnoreCase(Protocolo.Cliente.PASSA)) {
                Resultado r = partida.passar(cargoDaVez);
                if (!r.deuSucesso()) {
                    jogador.enviar(Protocolo.Servidor.ERRO + " " + r.getErro());
                    continue;
                }
                Evento.FimTurno fimTurno = (Evento.FimTurno) r.getEventos().get(0);
                jogador.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.PASSA_VALIDA);
                broadcast(Protocolo.Servidor.FIM_TURNO + " " + Protocolo.MotivoFimTurno.PASSOU
                        + " " + fimTurno.proximoTime);
                return;
            }

            if (!partes[0].equalsIgnoreCase(Protocolo.Cliente.CHUTE)) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.COMANDO_DESCONHECIDO);
                continue;
            }
            if (partes.length != 2) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.ARGUMENTOS_INVALIDOS);
                continue;
            }

            int posicao;
            try {
                posicao = Integer.parseInt(partes[1]);
            } catch (NumberFormatException e) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.POSICAO_INVALIDA);
                continue;
            }

            Resultado r = partida.chutar(cargoDaVez, posicao);
            if (!r.deuSucesso()) {
                jogador.enviar(Protocolo.Servidor.ERRO + " " + r.getErro());
                continue;
            }
            jogador.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.CHUTE_VALIDO);

            // chutar sempre devolve Revelar primeiro, e às vezes um segundo evento junto (fim de jogo ou fim de turno)
            Evento.Revelar revelou = (Evento.Revelar) r.getEventos().get(0);
            broadcast(Protocolo.Servidor.REVELAR + " " + revelou.carta.getPosicao()
                    + " " + corParaProtocolo(revelou.carta.getCor()));
            broadcastPlacar();

            if (r.getEventos().size() == 1) {
                // só revelou (acertou a propria cor, ainda sobra tentativa) -- turno continua
                broadcast(Protocolo.Servidor.VEZ_PALPITE + " " + cargoDaVez.time()
                        + " " + partida.getPalpitesRestantes());
                continue;
            }

            Evento segundo = r.getEventos().get(1);
            if (segundo instanceof Evento.FimDeJogo) {
                Evento.FimDeJogo fim = (Evento.FimDeJogo) segundo;
                broadcast(Protocolo.Servidor.VENCEDOR + " " + fim.vencedor + " " + fim.motivo);
            } else {
                Evento.FimTurno fimTurno = (Evento.FimTurno) segundo;
                String motivo = (revelou.carta.getCor() == cargoDaVez.time())
                        ? Protocolo.MotivoFimTurno.SEM_PALPITES
                        : Protocolo.MotivoFimTurno.ERROU;
                broadcast(Protocolo.Servidor.FIM_TURNO + " " + motivo + " " + fimTurno.proximoTime);
            }
            return;
        }
    }

    private void tratarDesconexao(Player jogador) {
        System.out.println(jogador.getCargo() + " desconectou durante a partida.");
        broadcast(Protocolo.Servidor.INFO + " Um jogador desconectou. Partida encerrada.");
        broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO);
        desconectarTodos();
        partidaEncerrada = true;
    }

    private void desconectarTodos() {
        System.out.println("Encerrando a conexão com todos os jogadores...");
        for (Player p : players.values()) {
            p.Fechar();
        }
        players.clear();
    }

    private String corParaProtocolo(CorCarta cor) {
        switch (cor) {
            case VERMELHA: return Protocolo.Cor.VERMELHA;
            case AZUL: return Protocolo.Cor.AZUL;
            case NEUTRA: return Protocolo.Cor.NEUTRA;
            case ASSASSINA: return Protocolo.Cor.ASSASSINA;
            default: throw new IllegalStateException("Cor desconhecida: " + cor);
        }
    }

    private void broadcastPlacar() {
        int vermelha = tabuleiro.cartasRestantes(CorCarta.VERMELHA);
        int azul = tabuleiro.cartasRestantes(CorCarta.AZUL);
        broadcast(Protocolo.Servidor.PLACAR + " " + vermelha + " " + azul);
    }

    private void broadcast(String msg) {
        for (Player p : players.values()) {
            p.enviar(msg);
        }
    }

}
