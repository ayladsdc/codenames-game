package objetos_comuns;

public class RenderizadorTabuleiro {
    
    // Memória local do cliente para saber o estado da tela
    private String[] cartasSalvas;
    private String placarAtual = "Placar: -";
    private String turnoAtual = "Aguardando turno...";

    // 1. Guarda o tabuleiro quando o jogo começa ou termina
    public void guardarTabuleiro(String[] partesDoComando) {
        this.cartasSalvas = partesDoComando;
    }

    // 2. Atualiza a cor de uma carta específica quando alguém chuta
    public void atualizarCarta(String posicao, String corRevelada) {
        if (cartasSalvas == null) return;
        
        for (int i = 1; i < cartasSalvas.length; i++) {
            String[] pedacos = cartasSalvas[i].split(":");
            if (pedacos[0].equals(posicao)) {
                // Monta a string de novo marcando a nova cor e o status de revelada (1)
                cartasSalvas[i] = pedacos[0] + ":" + pedacos[1] + ":" + corRevelada + ":1";
                break;
            }
        }
    }

    public void atualizarPlacar(String vermelhas, String azuis) {
        this.placarAtual = "CARTAS RESTANTES -> Vermelha: " + vermelhas + " | Azul: " + azuis;
    }

    public void atualizarTurno(String avisoDeTurno) {
        this.turnoAtual = avisoDeTurno;
    }

    // 3. Desenha a grade 5x5 com cores ANSI
    public void desenharTela() {
        if (cartasSalvas == null) return;

        // Aumentei a barra de "=" para cobrir a nova largura do tabuleiro
        System.out.println("\n===========================================================================================================================================");
        System.out.println(placarAtual);
        System.out.println(turnoAtual);
        System.out.println("===========================================================================================================================================");

        int coluna = 0;
        
        for (int i = 1; i < cartasSalvas.length; i++) {
            String[] pedacos = cartasSalvas[i].split(":");
            String posicao = pedacos[0];
            String palavra = pedacos[1].replace("_", " "); 
            String cor = pedacos[2];
            boolean revelada = pedacos[3].equals("1");

            if (palavra.length() > 12) {
                palavra = palavra.substring(0, 12);
            }

            String textoDaCarta = "[" + posicao + "] " + palavra;
            
            if (!cor.equals("?")) {
                textoDaCarta += " (" + cor.toLowerCase() + ")";
            }

            if (revelada) {
                textoDaCarta += " (X)"; 
            }

            // Aplica as cores ANSI pastéis
            String corAnsi = "\u001B[0m"; 
            if (cor.equals("VERMELHA")) corAnsi = "\u001B[38;5;203m"; 
            else if (cor.equals("AZUL")) corAnsi = "\u001B[38;5;111m"; 
            else if (cor.equals("NEUTRA")) corAnsi = "\u001B[38;5;180m"; 
            else if (cor.equals("ASSASSINA")) corAnsi = "\u001B[38;5;140m"; 

            // MUDANÇA 1: Aumentei de %-20s para %-28s para caber a palavra "(assassina)" sem empurrar a carta do lado
            System.out.print(corAnsi + String.format("%-28s", textoDaCarta) + "\u001B[0m");

            coluna++;
            if (coluna == 5) {
                System.out.println(); // Quebra a linha da fileira atual
                System.out.println(); // MUDANÇA 2: Pula uma linha extra (espaçamento vertical entre as linhas)
                coluna = 0;
            }
        }
        System.out.println("===========================================================================================================================================\n");
    }
}