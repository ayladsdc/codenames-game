package objetos_comuns;

public class Carta {
    private boolean revelada;
    private String palavra;
    private CorCarta cor;
    private int posicao;

    public Carta(String palavra, CorCarta cor, int posicao){
        this.palavra = palavra;
        this.cor = cor;
        this.posicao = posicao;
        this.revelada = false;
    }

    public boolean estaRevelada()   {return revelada;}
    public String getPalavra()      {return palavra;}
    public CorCarta getCor()        {return cor;}
    public int getPosicao()         {return posicao;}
    public void revelar()           {this.revelada = true;}
}