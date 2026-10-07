package servidor;

import objetos_comuns.Cargo; import objetos_comuns.Protocolo; import objetos_comuns.Mensagem;

import java.io.IOException;
import java.net.ServerSocket; import java.net.Socket;
import java.util.ArrayList; import java.util.Arrays;
import java.util.EnumMap; import java.util.Map;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.net.SocketTimeoutException;

public class Lobby {
    private final ServerSocket server;
    private volatile boolean jogoComecou = false;
    private final Map<Cargo, Player> players = new EnumMap<>(Cargo.class);
    private final List<Cargo> disponivel = new ArrayList<>(Arrays.asList(Cargo.values()));
    private final List<Player> todosConectados = new ArrayList<>();
    
    // threads que cuidam de cada cliente no lobby (negociar cargo + vigiar até o jogo começar)
    private final List<Thread> threadsJogadores = new CopyOnWriteArrayList<>();

    public Lobby(ServerSocket server) {this.server = server;}

    public Map<Cargo, Player> aguardarJogadores() {
        // Thread assíncrona só para aceitar clientes, permitindo que vários negociem ao mesmo tempo
        Thread threadAceitadora = new Thread(() -> {
            while (true) {
                Socket socket = null;                       // declare ANTES do try
                try {
                    socket = server.accept();
                    Player player = new Player(socket);

                    synchronized (Lobby.this) {

                        if (todosConectados.size() >= 4) {
                            player.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.PARTIDA_CHEIA);
                            player.Fechar();
                            continue;
                        }
                        
                        todosConectados.add(player);
                        enviarCargosLivres(player);
                    }

                    // Inicia uma thread exclusiva para o novo cliente negociar o seu cargo
                    Thread threadDoPlayer = new Thread(() -> lidarComPlayer(player));
                    threadsJogadores.add(threadDoPlayer);
                    threadDoPlayer.start();

                } catch (IOException e) {
                    if (server.isClosed()) break;           // servidor fechou: encerra a thread
                    // falha só nessa conexão (cliente caiu na hora etc.): fecha ela e segue aceitando
                    if (socket != null) {
                        try { socket.close(); } catch (IOException ignored) {}
                    }
                }
            }
        });
        threadAceitadora.setDaemon(true);
        threadAceitadora.start();

        // A thread principal trava aqui até os 4 cargos estarem ocupados
        synchronized (this) {
            while (players.size() < 4) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            jogoComecou = true;
        }

        // Espera as threads de vigia terminarem (elas devolvem o socket "limpo", sem timeout).
        // Sem isso, as threads leitoras do jogo poderiam disputar o mesmo BufferedReader com a vigia,
        // ou pegar um SocketTimeoutException de 200ms e achar que o jogador caiu.
        // Fica FORA do synchronized, porque desconectar() também é synchronized (evita deadlock).
        for (Thread t : threadsJogadores) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return players; // Devolve o Map pronto para o ServidorCodenames
    }

    private void lidarComPlayer(Player player) {
        try {
            player.getSocket().setSoTimeout(200); // "tick" só para conferir se o jogo já começou
            while (!jogoComecou) {
                String linha;
                try {
                    linha = player.recebe();
                } catch (SocketTimeoutException e) {
                    continue; // ninguém mandou nada nesses 200ms
                }
                if (linha == null) {
                    desconectar(player);
                    return;
                }
                tratarLinha(player, linha);
            }
        } catch (IOException e) {
            desconectar(player); // Ctrl+C / conexão resetada
        } finally {
            try {
                player.getSocket().setSoTimeout(0); // devolve o socket "limpo" pro jogo usar
            } catch (IOException ignored) {
            }
        }
    }

    /** Mesma ordem de erros do PROTOCOLO.md: vazia/longa, desconhecido, fora_de_hora, argumentos, específicos. */
    private void tratarLinha(Player player, String linha) {
        if (linha.length() > Protocolo.TAMANHO_MAXIMO_LINHA) { erro(player, Protocolo.Erro.LINHA_LONGA); return; }
        linha = linha.trim();
        if (linha.isEmpty()) { erro(player, Protocolo.Erro.LINHA_VAZIA); return; }

        Mensagem msg = Mensagem.parse(linha);
        String comando = msg.getComando();

        // comandos do jogo no lobby: existem, mas ainda não é a hora
        if (comando.equals(Protocolo.Cliente.DICA) || comando.equals(Protocolo.Cliente.CHUTE)
                || comando.equals(Protocolo.Cliente.PASSA)) {
            erro(player, Protocolo.Erro.FORA_DE_HORA);
            return;
        }
        if (!comando.equals(Protocolo.Cliente.CARGO)) {
            erro(player, Protocolo.Erro.COMANDO_DESCONHECIDO);
            return;
        }
        if (msg.getArgs().size() != 1) {
            erro(player, Protocolo.Erro.ARGUMENTOS_INVALIDOS);
            return;
        }

        Cargo requisitado = null;
        try {
            requisitado = Cargo.valueOf(msg.getArgs().get(0)); // sem toUpperCase: só maiúsculas valem
        } catch (IllegalArgumentException e) {
            // fica null e cai em cargo_invalido
        }

        // Bloco para evitar condição de corrida entre os clientes
        synchronized (this) {
            if (requisitado == null) {
                erro(player, Protocolo.Erro.CARGO_INVALIDO);
            } else if (player.getCargo() != null) {
                erro(player, Protocolo.Erro.CARGO_JA_ESCOLHIDO);
            } else if (!disponivel.contains(requisitado)) {
                erro(player, Protocolo.Erro.CARGO_OCUPADO + " " + requisitado.name());
            } else {
                // SUCESSO (igual ao que já tinha, só sem a variável "escolheu")
                player.setCargo(requisitado);
                disponivel.remove(requisitado);
                players.put(requisitado, player);

                player.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.BEM_VINDO + " " + requisitado.name());
                System.out.println(requisitado + " conectado (" + players.size() + "/4).");

                broadcast(Protocolo.Servidor.INFO + " " + players.size() + "/4 jogadores");
                broadcastCargosLivres();

                if (players.size() == 4) {
                    notifyAll(); // acorda a thread principal
                }
            }
        }
    }

    private void erro(Player player, String motivo) {
        player.enviar(Protocolo.Servidor.ERRO + " " + motivo);
    }

    private synchronized void enviarCargosLivres(Player p) {
        StringBuilder sb = new StringBuilder(Protocolo.Servidor.CARGOS_LIVRES);
        for (Cargo c : disponivel) {
            sb.append(" ").append(c.name());
        }
        p.enviar(sb.toString());
    }

    private synchronized void broadcastCargosLivres() {
        StringBuilder sb = new StringBuilder(Protocolo.Servidor.CARGOS_LIVRES);
        for (Cargo c : disponivel) {
            sb.append(" ").append(c.name());
        }
        String msg = sb.toString();
        for (Player p : todosConectados) {
            p.enviar(msg);
        }
    }

    private synchronized void broadcast(String msg) {
        for (Player p : todosConectados) {
            p.enviar(msg);
        }
    }

    private synchronized void desconectar(Player player) {
        todosConectados.remove(player);
        Cargo c = player.getCargo();
        if (c != null) {
            disponivel.add(c);
            players.remove(c);
            player.setCargo(null);
            System.out.println("Vaga liberada: " + c);
        }
        // Se cair, seja com ou sem cargo, liberou vaga geral e reenvia cargos lives
        broadcastCargosLivres();
        player.Fechar();
    }
}
