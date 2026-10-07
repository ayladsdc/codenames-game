package objetos_comuns;

// aqui é o que o jogador "enxerga" de uma carta, o que realmente vai pela rede.
// Carta é do servidor (sempre sabe a cor); CartaVisivel é o que o servidor decide mostrar para cada cargo
public record CartaVisivel(int posicao, String palavra, CorCarta cor, boolean revelada) {

    public static CartaVisivel de(Carta carta, boolean visaoMestre) {
        CorCarta cor = null;
        if (visaoMestre || carta.estaRevelada()) cor = carta.getCor();
        return new CartaVisivel(carta.getPosicao(), carta.getPalavra(), cor, carta.estaRevelada());
    }

    public boolean corOculta() {return cor == null;}
}