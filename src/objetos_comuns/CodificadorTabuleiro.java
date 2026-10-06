package objetos_comuns;

import java.util.ArrayList;
import java.util.List;

// Transforma o tabuleiro em cartas de texto ("2:JET_SKI:?:0") e de volta.
// Não conhece a Mensagem: recebe e devolve só listas, quem monta/lê a linha é a Mensagem.
public class CodificadorTabuleiro {

    private static final String CARTA_REVELADA = "1";
    private static final String CARTA_NAO_REVELADA = "0";

    private CodificadorTabuleiro(){}

    // ------------------------------------------------------------------ codificar

    // visaoMestre = true  -> mostra a cor de todas as cartas
    // visaoMestre = false -> só mostra a cor das cartas já reveladas
    public static List<String> codificar(Tabuleiro tabuleiro, boolean visaoMestre) {
        List<String> cartas = new ArrayList<>();

        for (Carta c : tabuleiro.getCartasJogo()) {
            CartaVisivel visivel = CartaVisivel.de(c, visaoMestre); // é o CartaVisivel que tira a cor da oculta
            cartas.add(codificarCarta(visivel));
        }

        return cartas;
    }

    // posicao:palavra:cor:revelada   (ex.: 2:JET_SKI:?:0)
    private static String codificarCarta(CartaVisivel c) {
        String cor = Protocolo.CARTA_OCULTA;
        if (!c.corOculta()) {
            cor = ConversorProtocolo.corParaProtocolo(c.cor());
        }

        String revelada = c.revelada() ? CARTA_REVELADA : CARTA_NAO_REVELADA;
        String sep = Protocolo.SEPARADOR_CAMPOS_CARTA;

        return c.posicao() + sep + c.palavra().replace(' ', '_') + sep + cor + sep + revelada;
    }

    // ------------------------------------------------------------------ decodificar

    // Se alguma carta estiver malformada, lança IllegalArgumentException.
    public static List<CartaVisivel> decodificar(List<String> textos) {
        if (textos.size() != Tabuleiro.getTamanhotabuleiro()) {
            throw new IllegalArgumentException("Tabuleiro deve ter " + Tabuleiro.getTamanhotabuleiro() + " cartas, veio " + textos.size());
        }

        List<CartaVisivel> cartas = new ArrayList<>();
        for (int i = 0; i < textos.size(); i++) {
            CartaVisivel carta = decodificarCarta(textos.get(i));

            // as cartas vêm em ordem de posição (1 a 25)
            if (carta.posicao() != i + 1) {
                throw new IllegalArgumentException("Carta fora de ordem: esperava " + (i + 1) + ", veio " + carta.posicao());
            }
            cartas.add(carta);
        }
        return cartas;
    }

    private static CartaVisivel decodificarCarta(String texto) {
        String[] campos = texto.split(Protocolo.SEPARADOR_CAMPOS_CARTA, -1); // -1: não joga fora campo vazio no fim
        if (campos.length != 4) {
            throw new IllegalArgumentException("Carta malformada: '" + texto + "'");
        }

        int posicao;
        try {
            posicao = Integer.parseInt(campos[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Posição inválida: '" + campos[0] + "'");
        }

        String palavra = campos[1].replace('_', ' ');
        if (palavra.isBlank()) {
            throw new IllegalArgumentException("Carta sem palavra: '" + texto + "'");
        }

        CorCarta cor = null; // '?' = oculta
        if (!campos[2].equals(Protocolo.CARTA_OCULTA)) {
            cor = ConversorProtocolo.corDoProtocolo(campos[2]);
        }

        boolean revelada;
        if (campos[3].equals(CARTA_REVELADA))          revelada = true;
        else if (campos[3].equals(CARTA_NAO_REVELADA)) revelada = false;
        else throw new IllegalArgumentException("Campo 'revelada' inválido: '" + campos[3] + "'");

        return new CartaVisivel(posicao, palavra, cor, revelada);
    }
}
