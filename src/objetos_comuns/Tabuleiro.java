package objetos_comuns;
import servico.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random; // uso do nextRandom, mostrada aqui (https://www.devmedia.com.br/numeros-aleatorios-em-java-a-classe-java-util-random/26355)

public class Tabuleiro {
    public static int tamanhoTabuleiro = 25;

    private List <Carta> cartasJogo = new ArrayList<>();
    private final CorCarta TimeInicio;
    private final CorCarta outroTime; 

    public Tabuleiro(){
        GeradorTabuleiro gerador = new GeradorTabuleiro();
        Random random = new Random();
        
        //escolhe a cor de cada time aleatoriamnete
        TimeInicio = (random.nextBoolean()) ? CorCarta.VERMELHO : CorCarta.AZUL;
        outroTime = (TimeInicio == CorCarta.VERMELHO) ? CorCarta.AZUL : CorCarta.VERMELHO;
        
        cartasJogo = gerador.gerarTabuleiro(TimeInicio, outroTime);
        
    }

    public CorCarta getTimeInicio() {
        return TimeInicio;
    }
    public CorCarta getOutroTime(){
        return outroTime;
    }

    public List<Carta> getCartasJogo() {
        return cartasJogo;
    }

    /**procura e retorna a carta pelo numero de sua posição, null se n achar  */
    public Carta AcharPeloNumero(int numero) {
        for (Carta c : cartasJogo) {
            if (c.getPosicao() == numero) {
                return c;
            }
        }
        return null;
    }

    /** Quantas cartas dessa cor ainda não foram reveladas. */
    public int CartasRestantes(CorCarta cor) {
        int qtd = 0;
        for (Carta c : cartasJogo) {
            if (c.getCor() == cor && !c.estaRevelada()) {
                qtd++;
            }
        }
        return qtd;
    }

/**Função do gemini para exibição das cartas na visão do mestre ---------IA*/
    public String visaoMestre() { 
    StringBuilder gradeCompleta = new StringBuilder();
    int colunasPorLinha = 5; // Define quantas cartas aparecem por linha no console

    for (int i = 0; i < cartasJogo.size(); i += colunasPorLinha) {
        StringBuilder linhaTopo = new StringBuilder();
        StringBuilder linhaPosicao = new StringBuilder();
        StringBuilder linhaPalavra = new StringBuilder();
        StringBuilder linhaCor = new StringBuilder();
        StringBuilder linhaBase = new StringBuilder();

        // Processa até 5 cartas para formar uma linha horizontal da grade
        for (int j = i; j < i + colunasPorLinha && j < cartasJogo.size(); j++) {
            Carta c = cartasJogo.get(j);
            
            // Formatando o número da posição (ex: "01", "15") com preenchimento de espaços
            String numPosicao = String.format("[ %02d ]", j + 1);
            String posStr = String.format("%-12s", numPosicao);
            
            // Corta ou estica a palavra/cor para caber certinho em 12 caracteres
            String p = String.format("%-12s", c.getPalavra().length() > 12 ? c.getPalavra().substring(0, 12) : c.getPalavra());
            String corStr = String.format("%-12s", c.getCor().name().length() > 12 ? c.getCor().name().substring(0, 12) : c.getCor());

            // Monta cada andar do quadradinho da carta atual
            linhaTopo.append("+--------------+   ");
            linhaPosicao.append("| ").append(posStr).append(" |   ");
            linhaPalavra.append("| ").append(p).append(" |   ");
            linhaCor.append("| ").append(corStr).append(" |   ");
            linhaBase.append("+--------------+   ");
        }

        // Junta todos os andares para formar a linha de blocos no console
        gradeCompleta.append(linhaTopo).append("\n")
                     .append(linhaPosicao).append("\n")
                     .append(linhaPalavra).append("\n")
                     .append(linhaCor).append("\n")
                     .append(linhaBase).append("\n\n");
    }

    return gradeCompleta.toString();
    }

/** funçaõ do gemini para exibir o tabuleiro do agente --- IA */
    public String visaoAgente() {
        StringBuilder gradeCompleta = new StringBuilder();
        int colunasPorLinha = 5; // Mantém 5 cartas por linha

        for (int i = 0; i < cartasJogo.size(); i += colunasPorLinha) {
            StringBuilder linhaTopo = new StringBuilder();
            StringBuilder linhaPosicao = new StringBuilder();
            StringBuilder linhaPalavra = new StringBuilder();
            StringBuilder linhaBase = new StringBuilder();

            // Processa as cartas da linha atual
            for (int j = i; j < i + colunasPorLinha && j < cartasJogo.size(); j++) {
                Carta c = cartasJogo.get(j);
                
                String numPosicao = String.format("[ %02d ]", j + 1);
                String posStr = String.format("%-12s", numPosicao);
                
                String p = String.format("%-12s", c.getPalavra().length() > 12 ? c.getPalavra().substring(0, 12) : c.getPalavra());

                // Monta o quadradinho (sem o andar da cor)
                linhaTopo.append("+--------------+   ");
                linhaPosicao.append("| ").append(posStr).append(" |   ");
                linhaPalavra.append("| ").append(p).append(" |   ");
                linhaBase.append("+--------------+   ");
            }

            // Junta os andares da linha
            gradeCompleta.append(linhaTopo).append("\n")
                        .append(linhaPosicao).append("\n")
                        .append(linhaPalavra).append("\n")
                        .append(linhaBase).append("\n\n");
        }

        return gradeCompleta.toString();
    }


public static void main(String[] args) {
    Tabuleiro tabuleiro = new Tabuleiro();

    // System.out.print("Visão mestre------------------");
    // System.out.print(tabuleiro.visaoMestre());
    // System.out.print("Visão agente------------------");
    // System.out.print(tabuleiro.visaoAgente());

    //System.out.print(tabuleiro.getTimeInicio().name()+" : "+tabuleiro.CartasRestantes(tabuleiro.getTimeInicio())+ "\n "+ tabuleiro.getOutroTime().name() +" : "+ tabuleiro.CartasRestantes(tabuleiro.getOutroTime()) );

}

}
