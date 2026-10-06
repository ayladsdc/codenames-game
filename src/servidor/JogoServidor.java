package servidor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import logica_jogo.*;
import objetos_comuns.*;

public class JogoServidor {

    // Apenas guardamos quem mandou e o que mandou
    private static class Comando {
        public Player autor;
        public String linha;
        
        public Comando(Player autor, String linha) {
            this.autor = autor;
            this.linha = linha;
        }
    }

    private Map<Cargo, Player> players;
    private Tabuleiro tabuleiro;
    private Partida partida;
    private BlockingQueue<Comando> filaComandos = new LinkedBlockingQueue<>();

    public JogoServidor(Map<Cargo, Player> players) {
        this.players = players;
    }

    public void iniciarJogo() throws IOException {
        broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.INICIADO);
        this.tabuleiro = new Tabuleiro();
        enviaTabuleiros();
        this.partida = new Partida(tabuleiro);

        // Inicia as 4 threads leitoras para a fila central
        for (Player p : players.values()) {
            Thread leitora = new Thread(() -> {
                try {
                    while (true) {
                        String linha = p.recebe(); 
                        
                        if (linha == null) {
                            filaComandos.put(new Comando(p, "DESCONECTOU"));
                            break;
                        }
                        filaComandos.put(new Comando(p, linha));
                    }
                } catch (IOException e) {
                    try { filaComandos.put(new Comando(p, "DESCONECTOU")); } catch (Exception ex) {}
                } catch (InterruptedException e) {
                    // se der erro
                }
            });
            leitora.start();
        }

        // avisa quem começa antes de entrar no laço
        broadcast(Protocolo.Servidor.VEZ_DICA + " " + partida.cargoMestreDaVez().time());

        while (partida.getFase() != Fase.FIM_DE_JOGO) {
            try {
                Comando cmd = filaComandos.take(); 

                if (cmd.linha.equals("DESCONECTOU")) {
                    tratarDesconexao(cmd.autor);
                    break;
                }

                Mensagem msg = Mensagem.parse(cmd.linha);
                if (msg.vazia()) continue;

                if (partida.getFase() == Fase.AGUARDANDO_DICA) {
                    processarTurnoDica(cmd, msg);
                } else {
                    processarTurnoPalpite(cmd, msg);
                }

            } catch (InterruptedException e) {
                break;
            }
        }

        // Acabou o jogo
        broadcast(Protocolo.Servidor.TABULEIRO_FINAL + " " + Mensagem.codificarTabuleiro(tabuleiro, true));
        broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO);
        desconectarTodos();
    }

    private void processarTurnoDica(Comando cmd, Mensagem msg) {
        if (!msg.getComando().equalsIgnoreCase(Protocolo.Cliente.DICA)) {
            cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.COMANDO_DESCONHECIDO);
            return;
        }

        if (msg.getArgs().size() != 2) {
            cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.ARGUMENTOS_INVALIDOS);
            return;
        }

        try {
            String palavra = msg.getArgs().get(0);
            int numero = Integer.parseInt(msg.getArgs().get(1));

            Resultado r = partida.darDica(cmd.autor.getCargo(), palavra, numero);

            if (!r.deuSucesso()) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + r.getErro());
                return;
            }

            // Deu certo, avisa quem mandou:
            cmd.autor.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.DICA_VALIDA);

            // Transforma os eventos em mensagens de broadcast
            for (Evento evento : r.getEventos()) {
                if (evento instanceof Evento.DicaDada) {
                    Evento.DicaDada e = (Evento.DicaDada) evento;
                    broadcast(Protocolo.Servidor.DICA_DADA + " " + e.palavra + " " + e.numero);
                    
                    // Avisa que é a vez do palpite
                    broadcast(Protocolo.Servidor.VEZ_PALPITE + " " + partida.cargoAgenteDaVez().time() + " " + partida.getPalpitesRestantes());
                }
            }

        } catch (NumberFormatException e) {
            cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.NUMERO_INVALIDO);
        }
    }

    private void processarTurnoPalpite(Comando cmd, Mensagem msg) {
        Resultado r = null;
        String comandoRecebido = msg.getComando();

        if (comandoRecebido.equalsIgnoreCase(Protocolo.Cliente.PASSA)) {
            r = partida.passar(cmd.autor.getCargo());
        } 
        else if (comandoRecebido.equalsIgnoreCase(Protocolo.Cliente.CHUTE)) {
            if (msg.getArgs().size() != 1) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.ARGUMENTOS_INVALIDOS);
                return;
            }

            try {
                int posicao = Integer.parseInt(msg.getArgs().get(0));
                r = partida.chutar(cmd.autor.getCargo(), posicao);
            } catch (NumberFormatException e) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.POSICAO_INVALIDA);
                return;}
        } 
        else {
            cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.COMANDO_DESCONHECIDO);
            return;
        }

        // Verifica se houve erro de regra
        if (!r.deuSucesso()) {
            cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + r.getErro());
            return;
        }

        if (comandoRecebido.equalsIgnoreCase(Protocolo.Cliente.PASSA)) {
            cmd.autor.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.PASSA_VALIDA);
        } else {
            cmd.autor.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.CHUTE_VALIDO);
        }

        // Envia os eventos para todo mundo
        for (Evento evento : r.getEventos()) {
            if (evento instanceof Evento.Revelar) {
                Evento.Revelar e = (Evento.Revelar) evento;
                broadcast(Protocolo.Servidor.REVELAR + " " + e.carta.getPosicao() + " " + ConversorProtocolo.corParaProtocolo(e.carta.getCor()));
                broadcastPlacar();
                
                // Se o turno continua (acertou e tem tentativas), avisa de novo
                if (partida.getFase() == Fase.AGUARDANDO_PALPITE) {
                    broadcast(Protocolo.Servidor.VEZ_PALPITE + " " + partida.cargoAgenteDaVez().time() + " " + partida.getPalpitesRestantes());
                }
            } 
            else if (evento instanceof Evento.FimTurno) {
                Evento.FimTurno e = (Evento.FimTurno) evento;
                String motivo = comandoRecebido.equalsIgnoreCase(Protocolo.Cliente.PASSA) ? Protocolo.MotivoFimTurno.PASSOU : Protocolo.MotivoFimTurno.ERROU;
                
                broadcast(Protocolo.Servidor.FIM_TURNO + " " + motivo + " " + e.proximoTime);
                broadcast(Protocolo.Servidor.VEZ_DICA + " " + partida.cargoMestreDaVez().time());
            } 
            else if (evento instanceof Evento.FimDeJogo) {
                Evento.FimDeJogo e = (Evento.FimDeJogo) evento;
                broadcast(Protocolo.Servidor.VENCEDOR + " " + e.vencedor + " " + e.motivo);
            }
        }
    }

    private void enviaTabuleiros() {
        String visaoAgente = Mensagem.codificarTabuleiro(tabuleiro, false);
        String visaoMestre = Mensagem.codificarTabuleiro(tabuleiro, true);

        players.get(Cargo.AZUL_AGENTE).enviar(visaoAgente);
        players.get(Cargo.VERMELHA_AGENTE).enviar(visaoAgente);

        players.get(Cargo.AZUL_MESTREESPIAO).enviar(visaoMestre);
        players.get(Cargo.VERMELHA_MESTREESPIAO).enviar(visaoMestre);
    }

    private void tratarDesconexao(Player jogador) {
        System.out.println(jogador.getCargo() + " desconectou.");
        broadcast(Protocolo.Servidor.INFO + " Um jogador desconectou. Partida encerrada.");
        broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO);
        desconectarTodos();
    }

    private void desconectarTodos() {
        for (Player p : players.values()) p.Fechar();
        players.clear();
    }

    private void broadcastPlacar() {
        int vermelha = tabuleiro.cartasRestantes(CorCarta.VERMELHA);
        int azul = tabuleiro.cartasRestantes(CorCarta.AZUL);
        broadcast(Protocolo.Servidor.PLACAR + " " + vermelha + " " + azul);
    }

    private void broadcast(String msg) {
        for (Player p : players.values()) p.enviar(msg);
    }
}