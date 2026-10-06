package servidor;

import objetos_comuns.Cargo; import objetos_comuns.Protocolo;

import java.io.IOException;
import java.net.ServerSocket; import java.net.Socket;
import java.util.ArrayList; import java.util.Arrays;
import java.util.EnumMap; import java.util.Map;
import java.util.List;
import java.net.SocketTimeoutException;

public class Lobby {
    private final ServerSocket server;
    private volatile boolean jogoComecou = false;
    private final Map<Cargo, Player> players = new EnumMap<>(Cargo.class);
    private final List<Cargo> disponivel = new ArrayList<>(Arrays.asList(Cargo.values()));
    private final List<Player> todosConectados = new ArrayList<>();

    public Lobby(ServerSocket server) {this.server = server;}

    public Map<Cargo, Player> aguardarJogadores() {
        // Thread assíncrona só para aceitar clientes, permitindo que vários negociem ao mesmo tempo
        Thread threadAceitadora = new Thread(() -> {
            while (true) {
                try {
                    Socket socket = server.accept();
                    Player player = new Player(socket);

                    synchronized (Lobby.this) {
                        // Rejeita a 5ª conexão em diante, fechando-a imediatamente
                        if (todosConectados.size() >= 4) {
                            player.enviar(Protocolo.Servidor.ERRO + " partida_cheia");
                            player.Fechar();
                            continue;
                        }
                        todosConectados.add(player);
                        enviarCargosLivres(player);
                    }
                    
                    // Inicia uma thread exclusiva para o novo cliente negociar o seu cargo
                    new Thread(() -> lidarComPlayer(player)).start();
                    
                } catch (IOException e) {
                    break; // O server principal foi fechado, encerra a thread
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

        return players; // Devolve o Map pronto para o ServidorCodenames
    }

    private void lidarComPlayer(Player player) {
        try {
            boolean escolheu = false;
            while (!escolheu) {
                String linha = player.recebe();
                if (linha == null) {
                    desconectar(player);
                    return;
                }

                String[] partes = linha.trim().split("\\s+", 2);
                if (partes[0].equalsIgnoreCase(Protocolo.Cliente.CARGO)) {
                    Cargo requisitado = null;
                    if (partes.length == 2) {
                        try {
                            requisitado = Cargo.valueOf(partes[1].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            // Deixa null intencionalmente para acionar erro de cargo_invalido
                        }
                    }

                    // Bloco para evitar condição de corrida entre os clientes
                    synchronized (this) {
                        if (requisitado == null) {
                            player.enviar(Protocolo.Servidor.ERRO + " cargo_invalido");
                        } else if (player.getCargo() != null) {
                            player.enviar(Protocolo.Servidor.ERRO + " cargo_ja_escolhido");
                        } else if (!disponivel.contains(requisitado)) {
                            player.enviar(Protocolo.Servidor.ERRO + " cargo_ocupado " + requisitado.name());
                        } else {
                            // SUCESSO
                            player.setCargo(requisitado);
                            disponivel.remove(requisitado);
                            players.put(requisitado, player);

                            player.enviar(Protocolo.Servidor.JOGO + " " + Protocolo.Jogo.BEM_VINDO + " " + requisitado.name());
                            System.out.println(requisitado + " conectado (" + players.size() + "/4).");

                            broadcast(Protocolo.Servidor.INFO + " " + players.size() + "/4 jogadores");
                            broadcastCargosLivres();

                            // Acorda a thread principal caso seja o último jogador
                            if (players.size() == 4) {
                                notifyAll();
                            }
                            escolheu = true;
                             // Quebra o loop para este jogador (espera acabar na main)
                        }
                    }
                } else {
                    player.enviar(Protocolo.Servidor.ERRO + " " + Protocolo.Erro.COMANDO_DESCONHECIDO);
                }
            }
        } catch (IOException e) {
            desconectar(player);
        }

        vigiarAposEscolha(player);
    }

    private void vigiarAposEscolha(Player player) {
    try {
        player.getSocket().setSoTimeout(200); // so um "tick" de verificacao
        while (!jogoComecou) {
            try {
                String linha = player.recebe();
                if (linha == null) {
                    desconectar(player);
                    return;
                }
                // qualquer coisa enviada nesse meio tempo e ignorada
            } catch (SocketTimeoutException e) {
                // normal: ninguem mandou nada nesses 200ms, continua conectado
            }
        }
    } catch (IOException e) {
        desconectar(player);
        return;
    } finally {
        try {
            player.getSocket().setSoTimeout(0); // devolve o socket "limpo" pro jogo usar
        } catch (IOException ignored) {
        }
    }
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