package objetos_comuns;

public class Carta {
    private String palavra;
    private CorCarta cor;
    private boolean revelada;

    public Carta(String palavra, CorCarta cor){
        this.palavra = palavra;
        this.cor = cor;
        this.revelada = false;
    }

    public String getPalavra() {
        return palavra;
    }

    public CorCarta getCor() {
        return cor;
    }

    public boolean estaRevelada() {
        return revelada;
    }

    public void relevarCartas() {
        this.revelada = true;
    }
}