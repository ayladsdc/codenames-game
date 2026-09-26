package objetos_comuns;

public class Carta {
    private String palavra;
    private CorCarta cor;
    private int posicao;
    private boolean revelada;

    public Carta(String palavra, CorCarta cor, int posicao){
        this.palavra = palavra;
        this.cor = cor;
        this.posicao = posicao;
        this.revelada = false;
    }

    public String getPalavra() {
        return palavra;
    }

    public CorCarta getCor() {
        return cor;
    }

    public int getPosicao(){
        return posicao;
    }

    public boolean estaRevelada() {
        return revelada;
    }

    public void relevarCartas() {
        this.revelada = true;
    }
}