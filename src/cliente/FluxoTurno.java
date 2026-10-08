package cliente;

import objetos_comuns.Mensagem;
import objetos_comuns.Protocolo;
import objetos_comuns.RenderizadorTabuleiro;

import java.io.PrintWriter;
import java.util.List;

/**
 * Fluxo da partida (Issue #14), no mesmo jeito do FluxoLobby:
 *  - processarMensagemServidor(linha): trata o que o servidor manda durante o jogo e atualiza a tela;
 *    devolve true se a mensagem foi tratada aqui (então quem chamou não imprime de novo);
 *  - processarEntradaTeclado(linha): valida o que o jogador digitou e só envia DICA / CHUTE / PASSA
 *    se for a vez dele.
 */
public class FluxoTurno {

    private static final int TOTAL_CARTAS = 25;

    private final PrintWriter out;
    private final EstadoCliente estado;
    private final RenderizadorTabuleiro tela;

    // últimas notícias (carta revelada, fim de turno...): vão junto do tabuleiro, porque o próximo
    // desenho da tela poderia apagar uma linha impressa antes dele. Só a thread do socket mexe aqui.
    private String ultimoEvento = "";

    public FluxoTurno(PrintWriter out, EstadoCliente estado, RenderizadorTabuleiro tela) {
        this.out = out;
        this.estado = estado;
        this.tela = tela;
    }

    // ================================================================== servidor -> jogador

    public boolean processarMensagemServidor(String linha) {
        Mensagem m = Mensagem.parse(linha);
        String comando = m.getComando();
        List<String> args = m.getArgs();
        String[] partes = linha.trim().split(Protocolo.SEPARADOR);

        if (comando.equals(Protocolo.Servidor.TABULEIRO_AGENTE) || comando.equals(Protocolo.Servidor.TABULEIRO_MESTRE)) {
            tela.guardarTabuleiro(partes); // a tela só é desenhada no primeiro VEZ_DICA
            return true;
        }

        if (comando.equals(Protocolo.Servidor.TABULEIRO_FINAL)) {
            tela.guardarTabuleiro(partes);
            estado.setFase(EstadoCliente.Fase.FIM);
            tela.atualizarTurno(textoFinal());
            tela.desenharTela();
            return true;
        }

        if (comando.equals(Protocolo.Servidor.PLACAR) && args.size() >= 2) {
            tela.atualizarPlacar(args.get(0), args.get(1));
            return true;
        }

        if (comando.equals(Protocolo.Servidor.REVELAR) && args.size() >= 2) {
            tela.atualizarCarta(args.get(0), args.get(1));
            ultimoEvento = Tradutor.paraCliente(linha);
            return true; // a tela é redesenhada logo depois, no VEZ_PALPITE / VEZ_DICA / TABULEIRO_FINAL
        }

        if (comando.equals(Protocolo.Servidor.DICA_DADA) && args.size() >= 2) {
            estado.setDicaAtual(Tradutor.paraCliente(linha));
            return true; // o VEZ_PALPITE que vem em seguida redesenha
        }

        if (comando.equals(Protocolo.Servidor.FIM_TURNO)) {
            ultimoEvento = (ultimoEvento + " " + Tradutor.paraCliente(linha)).trim();
            return true; // o VEZ_DICA que vem em seguida redesenha
        }

        if (comando.equals(Protocolo.Servidor.VENCEDOR)) {
            estado.setFase(EstadoCliente.Fase.FIM);
            ultimoEvento = (ultimoEvento + "\n" + Tradutor.paraCliente(linha)).trim();
            return true; // o TABULEIRO_FINAL que vem em seguida desenha a tela final
        }

        if (comando.equals(Protocolo.Servidor.VEZ_DICA) && args.size() >= 1) {
            estado.setFase(EstadoCliente.Fase.DICA);
            estado.setTimeDaVez(args.get(0));
            estado.setDicaAtual(null);
            redesenhar();
            return true;
        }

        if (comando.equals(Protocolo.Servidor.VEZ_PALPITE) && args.size() >= 2) {
            try {
                estado.setPalpitesRestantes(Integer.parseInt(args.get(1)));
            } catch (NumberFormatException e) {
                return false; // mensagem estranha: deixa quem chamou mostrar a linha
            }
            estado.setFase(EstadoCliente.Fase.PALPITE);
            estado.setTimeDaVez(args.get(0));
            redesenhar();
            return true;
        }

        if (comando.equals(Protocolo.Servidor.JOGO) && args.size() >= 1) {
            String estadoJogo = args.get(0);
            // confirmações só para quem enviou: a tela já reage a DICA_DADA e REVELAR
            if (estadoJogo.equals(Protocolo.Jogo.DICA_VALIDA)
                    || estadoJogo.equals(Protocolo.Jogo.CHUTE_VALIDO)
                    || estadoJogo.equals(Protocolo.Jogo.PASSA_VALIDA)) {
                return true;
            }
            if (estadoJogo.equals(Protocolo.Jogo.ENCERRADO)) {
                estado.setFase(EstadoCliente.Fase.FIM);
            }
            return false; // iniciado / encerrado: quem chamou imprime "A partida começou!" etc.
        }

        return false; // INFO, ERRO e o resto: quem chamou traduz e imprime
    }

    private void redesenhar() {
        tela.atualizarTurno(textoDoTurno());
        tela.desenharTela();
        ultimoEvento = "";
    }

    private String textoDoTurno() {
        StringBuilder sb = new StringBuilder();
        if (!ultimoEvento.isEmpty()) sb.append(ultimoEvento).append("\n");

        if (estado.getFase() == EstadoCliente.Fase.PALPITE) {
            if (estado.getDicaAtual() != null) sb.append(estado.getDicaAtual()).append("\n");
            sb.append("Palpites restantes: ").append(estado.getPalpitesRestantes()).append("\n");
        }

        if (estado.ehMinhaVez()) {
            if (estado.getFase() == EstadoCliente.Fase.DICA) {
                sb.append("SUA VEZ! Digite a dica: <palavra> <número de 1 a 9>");
            } else {
                sb.append("SUA VEZ! Digite CHUTE <posição de 1 a ").append(TOTAL_CARTAS).append("> ou PASSA");
            }
        } else {
            sb.append(textoAguardando());
        }
        return sb.toString();
    }

    private String textoFinal() {
        String resultado = ultimoEvento.isEmpty() ? "Fim de jogo." : ultimoEvento;
        ultimoEvento = "";
        return "FIM DE JOGO\n" + resultado;
    }

    private String textoAguardando() {
        boolean ehDica = estado.getFase() == EstadoCliente.Fase.DICA;
        String time = estado.getTimeDaVez();
        boolean meuTime = estado.getTime() != null && estado.getTime().name().equals(time);

        if (meuTime) { // é a vez do meu time, mas de outro papel
            return ehDica ? "Aguardando o mestre-espião do seu time dar a dica..."
                          : "Aguardando o agente do seu time chutar...";
        }
        return "Aguardando o time " + time + (ehDica ? " dar a dica..." : " chutar...");
    }

    // ================================================================== jogador -> servidor

    public void processarEntradaTeclado(String linha) {
        EstadoCliente.Fase fase = estado.getFase();

        if (fase == EstadoCliente.Fase.LOBBY) {
            System.out.println("Aguarde: a partida ainda não começou.");
            return;
        }
        if (fase == EstadoCliente.Fase.FIM) {
            System.out.println("A partida acabou.");
            return;
        }
        if (!estado.ehMinhaVez()) { // fora da vez nada é enviado ao servidor
            System.out.println(textoAguardando());
            return;
        }

        if (estado.ehMestre()) enviarDica(linha);
        else enviarPalpite(linha);
    }

    /** Mestre: aceita "<palavra> <numero>" ou "DICA <palavra> <numero>". */
    private void enviarDica(String linha) {
        String[] partes = linha.trim().split("\\s+");
        int inicio = partes[0].equalsIgnoreCase(Protocolo.Cliente.DICA) ? 1 : 0;
        int quantidade = partes.length - inicio;

        if (quantidade < 2) {
            System.out.println("Faltou o número. Use: <palavra> <número de 1 a 9>  (exemplo: animal 2)");
            return;
        }
        if (quantidade > 2) {
            System.out.println("A dica deve ser uma única palavra, sem espaço. Use: <palavra> <número de 1 a 9>");
            return;
        }

        String palavra = partes[inicio];
        if (palavra.contains("_")) {
            System.out.println("A dica não pode ter '_'. Use uma palavra só.");
            return;
        }
        if (!palavra.matches("\\p{L}+")) {
            System.out.println("A dica deve ter só letras (sem números nem símbolos).");
            return;
        }

        int numero;
        try {
            numero = Integer.parseInt(partes[inicio + 1]);
        } catch (NumberFormatException e) {
            numero = -1;
        }
        if (numero < 1 || numero > 9) {
            System.out.println("O número da dica deve ser um inteiro de 1 a 9.");
            return;
        }

        out.println(Mensagem.dica(palavra, numero).paraLinha());
    }

    /** Agente: aceita "CHUTE <n>", "chute <n>", só "<n>" ou "PASSA". */
    private void enviarPalpite(String linha) {
        String[] partes = linha.trim().split("\\s+");
        String comando = partes[0].toUpperCase();

        if (comando.equals(Protocolo.Cliente.PASSA)) {
            if (partes.length != 1) {
                System.out.println("PASSA não tem argumentos. Digite só: PASSA");
                return;
            }
            out.println(Mensagem.passa().paraLinha());
            return;
        }

        String textoPosicao;
        if (comando.equals(Protocolo.Cliente.CHUTE) && partes.length == 2) {
            textoPosicao = partes[1];
        } else if (partes.length == 1 && partes[0].matches("\\d+")) {
            textoPosicao = partes[0];
        } else {
            System.out.println("Comando inválido. Use: CHUTE <posição de 1 a " + TOTAL_CARTAS + "> ou PASSA");
            return;
        }

        int posicao;
        try {
            posicao = Integer.parseInt(textoPosicao);
        } catch (NumberFormatException e) {
            posicao = -1;
        }
        if (posicao < 1 || posicao > TOTAL_CARTAS) {
            System.out.println("A posição deve ser um número de 1 a " + TOTAL_CARTAS + ".");
            return;
        }

        out.println(Mensagem.chute(posicao).paraLinha());
    }

    // ================================================================== comandos locais

    public void mostrarAjuda() {
        System.out.println("Comandos locais:\n  ajuda - mostra esta ajuda\n  sair  - fecha o cliente");

        if (!estado.temCargo()) {
            System.out.println("Escolha seu cargo digitando o número da opção do menu.");
        } else if (estado.ehMestre()) {
            System.out.println("Mestre-espião: na sua vez, digite a dica como <palavra> <número de 1 a 9> "
                    + "(exemplo: animal 2). A palavra deve ser uma só, apenas com letras.");
        } else {
            System.out.println("Agente: na sua vez, digite CHUTE <posição de 1 a " + TOTAL_CARTAS
                    + "> (ou só o número) para chutar uma carta, ou PASSA para encerrar o turno.");
        }
    }
}
