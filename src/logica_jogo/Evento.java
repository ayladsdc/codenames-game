package logica_jogo;

import objetos_comuns.Carta;
import objetos_comuns.CorCarta;

public abstract class Evento {

    private Evento(){
    }

    /**mestre espião deu uma dica */
    public static class DicaDada extends Evento {
        public final CorCarta time;
        public final String palavra;
        public final int numero;

        public DicaDada(CorCarta time, String palavra, int numero) {
            this.time = time;
            this.palavra = palavra;
            this.numero = numero;
        }
    }

    /**Uma carta foi revelada por chute */
    public static class Revelar extends Evento {
        public final Carta carta;

        public Revelar(Carta carta) {
            this.carta = carta;
        }        
    }

    /** O turno acabou (chute errado, passou, ou acabaram os palpites) e a vez passou pro time indicado. */
    public static final class FimTurno extends Evento {
        public final CorCarta proximoTime;
 
        public FimTurno(CorCarta proximoTime) {
            this.proximoTime = proximoTime;
        }
    }
 
    /** A partida acabou. */
    public static final class FimDeJogo extends Evento {
        public final CorCarta vencedor;
        public final String motivo; // ex.: "ASSASSINO", "TODAS_PALAVRAS"
 
        public FimDeJogo(CorCarta vencedor, String motivo) {
            this.vencedor = vencedor;
            this.motivo = motivo;
        }
    }
}
