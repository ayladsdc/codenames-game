package objetos_comuns;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import servico.*;

public class Tabuleiro {
    private static final int tamanhoTabuleiro = 25;

    private List <Carta> cartasJogo = new ArrayList<>();
    private final CorCarta TimeInicio;
    private final CorCarta outroTime; 

    public Tabuleiro(){
        GeradorTabuleiro gerador = new GeradorTabuleiro();
        Random random = new Random();
        
        //escolhe a cor de cada time aleatoriamnete
        if (random.nextBoolean()) {
            TimeInicio = CorCarta.VERMELHA;
            outroTime = CorCarta.AZUL;}
        else {
            TimeInicio = CorCarta.AZUL;
            outroTime = CorCarta.VERMELHA;
        }
        
        cartasJogo = gerador.gerarTabuleiro(TimeInicio, outroTime);
    }

    public Tabuleiro(List<Carta> cartas, CorCarta timeInicio) {
        if (cartas == null || timeInicio == null) throw new IllegalArgumentException("Cartas e timeInicio não podem ser nulos.");
        
        this.cartasJogo = new ArrayList<>(cartas);
        this.TimeInicio = timeInicio;
        
        if (TimeInicio == CorCarta.VERMELHA) {outroTime = CorCarta.AZUL;}
        else                                 {outroTime = CorCarta.VERMELHA;}
    }


    //getters básicos
    public CorCarta getTimeInicio(){return TimeInicio;}
    public CorCarta getOutroTime(){return outroTime;}
    public List<Carta> getCartasJogo(){return cartasJogo;}
    public static int getTamanhotabuleiro() { return tamanhoTabuleiro;}

    //acha a carta pelo número dela
    public Carta acharPeloNumero(int numero) {
        for (int i = 0; i < cartasJogo.size(); i++) {
            
            Carta c = cartasJogo.get(i);
            if (c.getPosicao() == numero) {
                return c;
            }
        }
        
        return null;
    }

    //revela a carta, se ela existir e ainda não tiver sido revelada, revela
    public Carta revelar(int posicao){

        if (posicao < 1 || posicao > tamanhoTabuleiro) throw new IllegalArgumentException(Protocolo.Erro.POSICAO_INVALIDA);
        Carta c = acharPeloNumero(posicao);
        if (c == null) throw new IllegalArgumentException(Protocolo.Erro.POSICAO_INVALIDA);
        if(c.estaRevelada()) throw new IllegalArgumentException(Protocolo.Erro.CARTA_JA_REVELADA);

        //se posicao conseguiu passar por todos os testes, pode revelar a carta
        c.revelar();
        return c;
    }

    private static String normalizar(String s) {
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.replace('_', ' ').trim().toUpperCase(Locale.ROOT);
    }

    public boolean cartaOcultaNoTabuleiro(String palavra) {
        String alvo = normalizar(palavra);
        for (int i = 0; i < cartasJogo.size(); i++) {
            Carta c = cartasJogo.get(i);
            if (!c.estaRevelada() && normalizar(c.getPalavra()).equals(alvo)) {
                return true;
            }
        }
        return false;
    }

    /** calcula quantas cartas de COR ainda não foram reveladas. */
    public int cartasRestantes(CorCarta cor) {
        int qtd = 0;
        for (int i = 0; i < cartasJogo.size(); i++) {
            Carta c = cartasJogo.get(i);
            if (c.getCor() == cor && !c.estaRevelada()) {
                qtd++;
            }
        }
        return qtd;
    }
}
