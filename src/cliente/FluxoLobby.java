package cliente;

import objetos_comuns.Cargo;
import objetos_comuns.Mensagem;
import objetos_comuns.Protocolo;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 *  - CARGOS_LIVRES      -> (re)monta e mostra o menu numerado;
 *  - o jogador digita o número -> tratarEntrada() envia "CARGO <NOME>" (ver ClienteCodenames.setTratadorEntrada);
 *  - JOGO bem_vindo     -> guarda o cargo no EstadoCliente;
 *  - ERRO cargo_ocupado / cargo_invalido / cargo_ja_escolhido -> mostra o motivo e pede de novo;
 *  - ERRO partida_cheia -> mostra o motivo e encerra o cliente.
 */
public class FluxoLobby {
    private final PrintWriter out;
    private final EstadoCliente estado;
    private volatile List<Cargo> livres = List.of();

    public FluxoLobby(PrintWriter out, EstadoCliente estado) {
        this.out = out;
        this.estado = estado;
    }

    public static boolean ehDoLobby(Mensagem m) {
        String comando = m.getComando();
        if (comando.equals(Protocolo.Servidor.CARGOS_LIVRES)) return true;

        if (comando.equals(Protocolo.Servidor.JOGO)) {
            String argumento = primeiroArg(m);
            if (argumento.equals(Protocolo.Jogo.BEM_VINDO)) {
                return true;
            }
        }

        if (comando.equals(Protocolo.Servidor.ERRO)) {
            String motivo = primeiroArg(m);
            
            if (motivo.equals(Protocolo.Erro.CARGO_OCUPADO))        return true;
            if (motivo.equals(Protocolo.Erro.CARGO_INVALIDO))       return true;
            if (motivo.equals(Protocolo.Erro.CARGO_JA_ESCOLHIDO))   return true;
            if (motivo.equals(Protocolo.Erro.PARTIDA_CHEIA))        return true;
        }

        return false;
    }

    public boolean processarMensagemServidor(String linha) {
        Mensagem m = Mensagem.parse(linha);
        
        if (!ehDoLobby(m)) {return false;}

        String comando = m.getComando();
        if (comando.equals          (Protocolo.Servidor.CARGOS_LIVRES)) {                                               aoCargosLivres(m);
        } else if (comando.equals   (Protocolo.Servidor.JOGO) && primeiroArg(m).equals(Protocolo.Jogo.BEM_VINDO)) {     aoBemVindo(m);
        } else if (comando.equals(Protocolo.Servidor.ERRO)) {                                                           aoErro(m);}
        
        return true; // realmente é do lobby, intercepta e não imprimime na tela principal
    }

    private void aoCargosLivres(Mensagem m) {
        List<Cargo> novos = new ArrayList<>();
        for (String nome : m.getArgs()) {
            try {
                novos.add(Cargo.valueOf(nome));
            } catch (IllegalArgumentException e) {
                // cargo que este cliente não conhece: ignora
            }
        }
        livres = List.copyOf(novos); // relê a lista a cada nova mensagem

        if (estado.temCargo()) { // já escolheu: não oferece o menu de novo, só informa quem falta
            if (!novos.isEmpty()) {
                System.out.println("Aguardando os outros jogadores. Cargos ainda livres: " + nomes(novos));
            }
            return;
        }
        mostrarMenu();
    }

    private void aoBemVindo(Mensagem m) {
        if (m.getArgs().size() < 2) return;
        try {
            estado.setCargo(Cargo.valueOf(m.getArgs().get(1)));
        } catch (IllegalArgumentException e) {
            System.err.println("[ignorada] cargo desconhecido em bem_vindo: " + m.paraLinha());
            return;
        }
        System.out.println("Você entrou como " + estado.getCargo().name()
                + (estado.ehMestre()
                        ? " (mestre-espião: vai ver as cores de todas as cartas e dar as dicas)."
                        : " (agente: vai chutar as cartas a partir das dicas)."));
    }

    private void aoErro(Mensagem m) {
        String motivo = primeiroArg(m);

        if (motivo.equals(Protocolo.Erro.CARGO_OCUPADO)) {
            String quem = m.getArgs().size() > 1 ? m.getArgs().get(1) : "esse cargo";
            System.out.println("O cargo " + quem + " já está ocupado. Escolha outro.");
            mostrarMenuSePrecisar();
        } else if (motivo.equals(Protocolo.Erro.CARGO_INVALIDO)) {
            System.out.println("Esse cargo não existe. Escolha uma das opções.");
            mostrarMenuSePrecisar();
        } else if (motivo.equals(Protocolo.Erro.CARGO_JA_ESCOLHIDO)) {
            System.out.println("Você já escolheu um cargo" + " (" + estado.getCargo().name() + ")" + ". Aguarde os outros jogadores.");
        } else if (motivo.equals(Protocolo.Erro.PARTIDA_CHEIA)) {
            System.out.println("A partida está cheia.");
            System.exit(0);
        }
    }

    public boolean processarEntradaTeclado(String linha) {
        if (estado.temCargo()) return false;
        if (!linha.matches("\\d+")) return false;

        List<Cargo> opcoes = livres;
        if (opcoes.isEmpty()) {
            System.out.println("Ainda não recebi a lista de cargos livres. Aguarde.");
            return true;
        }

        int n;
        try { n = Integer.parseInt(linha);
        } catch (NumberFormatException e) { n = -1; }
            
        if (n < 1 || n > opcoes.size()) {
            System.out.println("Opção inválida. Digite um número de 1 a " + opcoes.size() + ".");
            return true;
        }

        // Aqui substituímos o antigo cliente.enviar() pelo out.println()
        out.println(Mensagem.cargo(opcoes.get(n - 1)).paraLinha());
        return true;
    }
    // ------------------------------------------------------------------ apoio

    private void mostrarMenuSePrecisar() {
        if (!estado.temCargo()) mostrarMenu();
    }

    private void mostrarMenu() {
        List<Cargo> opcoes = livres;
        if (opcoes.isEmpty()) {
            System.out.println("Nenhum cargo livre no momento. Aguarde.");
            return;
        }
        StringBuilder sb = new StringBuilder("Escolha seu cargo:\n");
        for (int i = 0; i < opcoes.size(); i++) {
            sb.append("  ").append(i + 1).append(") ").append(opcoes.get(i).name()).append("\n");
        }
        sb.append("Digite o número da opção.");
        System.out.println(sb);
    }

    private static String nomes(List<Cargo> cargos) {
        List<String> nomes = new ArrayList<>();
        for (Cargo c : cargos) nomes.add(c.name());
        return String.join(", ", nomes);
    }

    private static String primeiroArg(Mensagem m) {
        return m.getArgs().isEmpty() ? "" : m.getArgs().get(0);
    }
}
