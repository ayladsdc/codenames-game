package servidor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import logica_jogo.*;
import objetos_comuns.*;

public class JogoServidor {

    // guarda quem mandou e o que mandou. A desconexão é um campo próprio (e não uma
    // string especial) para que nenhum jogador consiga encerrar a partida digitando "DESCONECTOU".
    private static class Comando {
        public final Player autor;
        public final String linha;
        public final boolean desconexao;

        private Comando(Player autor, String linha, boolean desconexao) {
            this.autor = autor;
            this.linha = linha;
            this.desconexao = desconexao;
        }

        static Comando linha(Player autor, String linha) {
            return new Comando(autor, linha, false);
        }

        static Comando desconexao(Player autor) {
            return new Comando(autor, null, true);
        }
    }

    private Map<Cargo, Player> players;
    private Tabuleiro tabuleiro;
    private Partida partida;
    private final BlockingQueue<Comando> filaComandos = new LinkedBlockingQueue<>();
    private boolean encerradaPorDesconexao = false;

    public JogoServidor(Map<Cargo, Player> players) {
        this.players = players;
    }

    public void iniciarJogo() throws IOException {
        broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.INICIADO);
        this.tabuleiro = new Tabuleiro();
        enviaTabuleiros();
        this.partida = new Partida(tabuleiro);

        // Inicia as 4 threads leitoras (uma por jogador) para a fila central
        for (Player p : players.values()) {
            Thread leitora = new Thread(() -> {
                try {
                    while (true) {
                        String linha = p.recebe();

                        if (linha == null) {
                            filaComandos.put(Comando.desconexao(p));
                            break;
                        }
                        filaComandos.put(Comando.linha(p, linha));
                    }
                } catch (IOException e) {
                    try { filaComandos.put(Comando.desconexao(p)); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            leitora.start();
        }

        // avisa quem começa antes de entrar no laço
        broadcast(Protocolo.Servidor.VEZ_DICA + " " + partida.cargoMestreDaVez().time());

        // Só esta thread mexe na partida e escreve nos jogadores durante o jogo
        while (partida.getFase() != Fase.FIM_DE_JOGO) {
            Comando cmd;
            try {
                cmd = filaComandos.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO);
                desconectarTodos();
                return;
            }

            if (cmd.desconexao) {
                tratarDesconexao(cmd.autor);
                break;
            }

            String linha = cmd.linha.trim();
            if (linha.isEmpty()) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.LINHA_VAZIA);
                continue;
            }
            if (linha.length() > Protocolo.TAMANHO_MAXIMO_LINHA) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.LINHA_LONGA);
                continue;
            }

            Mensagem msg = Mensagem.parse(linha);
            if (msg.vazia()) continue;

            if (partida.getFase() == Fase.AGUARDANDO_DICA) {
                processarTurnoDica(cmd, msg);
            } else {
                processarTurnoPalpite(cmd, msg);
            }
        }

        // Acabou o jogo
        if (encerradaPorDesconexao) {
            System.out.println("Partida encerrada por desconexao.");
            return; // tratarDesconexao já avisou todo mundo e fechou as conexões
        }

        System.out.println("Partida encerrada! Vencedor: " + partida.getVencedor()
                + " (" + partida.getMotivoFim() + ")");
        broadcast(Mensagem.tabuleiroFinal(tabuleiro).paraLinha()); // já inclui TABULEIRO_FINAL
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
        boolean passou = comandoRecebido.equalsIgnoreCase(Protocolo.Cliente.PASSA);
        Cargo cargoDoAutor = cmd.autor.getCargo();

        if (passou) {
            r = partida.passar(cargoDoAutor);
        }
        else if (comandoRecebido.equalsIgnoreCase(Protocolo.Cliente.CHUTE)) {
            if (msg.getArgs().size() != 1) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.ARGUMENTOS_INVALIDOS);
                return;
            }

            try {
                int posicao = Integer.parseInt(msg.getArgs().get(0));
                r = partida.chutar(cargoDoAutor, posicao);
            } catch (NumberFormatException e) {
                cmd.autor.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.POSICAO_INVALIDA);
                return;
            }
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

        if (passou) {
            cmd.autor.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.PASSA_VALIDA);
        } else {
            cmd.autor.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.CHUTE_VALIDO);
        }

        // Envia os eventos para todo mundo
        Evento.Revelar revelou = null; // chutar sempre devolve Revelar primeiro; guardamos para saber o motivo do fim de turno
        for (Evento evento : r.getEventos()) {
            if (evento instanceof Evento.Revelar) {
                revelou = (Evento.Revelar) evento;
                broadcast(Protocolo.Servidor.REVELAR + " " + revelou.carta.getPosicao() + " " + ConversorProtocolo.corParaProtocolo(revelou.carta.getCor()));
                broadcastPlacar();

                // Se o turno continua (acertou a própria cor e ainda sobra tentativa), avisa de novo
                if (partida.getFase() == Fase.AGUARDANDO_PALPITE) {
                    broadcast(Protocolo.Servidor.VEZ_PALPITE + " " + partida.cargoAgenteDaVez().time() + " " + partida.getPalpitesRestantes());
                }
            }
            else if (evento instanceof Evento.FimTurno) {
                Evento.FimTurno e = (Evento.FimTurno) evento;

                String motivo;
                if (passou) {
                    motivo = Protocolo.MotivoFimTurno.PASSOU;
                } else if (revelou != null && revelou.carta.getCor() == cargoDoAutor.time()) {
                    // acertou a própria cor, mas acabaram os palpites
                    motivo = Protocolo.MotivoFimTurno.SEM_PALPITES;
                } else {
                    motivo = Protocolo.MotivoFimTurno.ERROU;
                }

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
        // codificarTabuleiro já devolve a linha completa, com o comando (TABULEIRO_AGENTE / TABULEIRO_MESTRE)
        String visaoAgente = Mensagem.codificarTabuleiro(tabuleiro, false);
        String visaoMestre = Mensagem.codificarTabuleiro(tabuleiro, true);

        players.get(Cargo.AZUL_AGENTE).enviar(visaoAgente);
        players.get(Cargo.VERMELHA_AGENTE).enviar(visaoAgente);

        players.get(Cargo.AZUL_MESTREESPIAO).enviar(visaoMestre);
        players.get(Cargo.VERMELHA_MESTREESPIAO).enviar(visaoMestre);
    }

    private void tratarDesconexao(Player jogador) {
        System.out.println(jogador.getCargo() + " desconectou durante a partida.");
        encerradaPorDesconexao = true;
        broadcast(Protocolo.Servidor.INFO + " Um jogador desconectou. Partida encerrada.");
        broadcast(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.ENCERRADO);
        desconectarTodos();
    }

    private void desconectarTodos() {
        System.out.println("Encerrando a conexão com todos os jogadores...");
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