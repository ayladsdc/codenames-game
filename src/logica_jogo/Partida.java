package logica_jogo;

import java.util.ArrayList;
import java.util.List;
import objetos_comuns.*;

/** A intenção disso tudo é fazer com que a a Partida controle toda a lógica do jogo, sendo ativada pelo servidor, que 
* lida com o cliente/sockets. Ela retorna um "Resultado", que é digerido pelo servidor.
*/

public class Partida {
    private final Tabuleiro tabuleiro;
    private CorCarta timeDaVez;
    private Fase fase;
    private int palpitesRestantes;
    private CorCarta vencedor;
    private String motivoFim;
 
    public Partida(Tabuleiro tabuleiro) {
        this.tabuleiro = tabuleiro;
        this.timeDaVez = tabuleiro.getTimeInicio();
        this.fase = Fase.AGUARDANDO_DICA;
        this.palpitesRestantes = 0;
        this.vencedor = null;
        this.motivoFim = null;
    }

    public CorCarta getTimeDaVez() {return timeDaVez;}
 
    public Fase getFase() {return fase;}
 
    public CorCarta getVencedor() {return vencedor;}
 
    public String getMotivoFim() {return motivoFim;}

    public Tabuleiro getTabuleiro() {return tabuleiro;}

    public int getPalpitesRestantes() {return palpitesRestantes;}

    public Resultado darDica(Cargo jogador, String palavra, int numero){
        
        //muitas verificações
        if(fase == Fase.FIM_DE_JOGO || fase == Fase.JOGO_NAO_INICIADO){return Resultado.erro(Protocolo.Erro.FORA_DE_HORA);}
        if(!jogador.eMestreEspiao()) {return Resultado.erro(Protocolo.Erro.PAPEL_INVALIDO);}
        if(fase != Fase.AGUARDANDO_DICA || timeDaVez !=jogador.time()){return Resultado.erro(Protocolo.Erro.FORA_DE_VEZ);}
        if(palavra == null || palavra.isEmpty()) {return Resultado.erro(Protocolo.Erro.ARGUMENTOS_INVALIDOS);}
        if(numero < 1 || numero > 9){return Resultado.erro(Protocolo.Erro.NUMERO_INVALIDO);} //limite do número de dicas é nove, e o mínimo é 1

        String palavraLimpa = palavra.trim();

        
        if(!palavraLimpa.matches("\\p{L}+") || tabuleiro.cartaOcultaNoTabuleiro(palavraLimpa)){ /*o "\\p{L}+" vê se a palavra tem algo além de letras com ou sem acento de qualquer idioma*/
            return Resultado.erro(Protocolo.Erro.DICA_INVALIDA);
        }

        

        fase = Fase.AGUARDANDO_PALPITE;
        palpitesRestantes = numero + 1;

        return Resultado.sucesso(new Evento.DicaDada(timeDaVez, palavraLimpa, numero));
    }

    public Resultado chutar(Cargo jogador, int numeroCarta){
        
        //verificações
        if(fase == Fase.FIM_DE_JOGO || fase == Fase.JOGO_NAO_INICIADO) return Resultado.erro(Protocolo.Erro.FORA_DE_HORA);
        if(jogador.eMestreEspiao()) return Resultado.erro(Protocolo.Erro.PAPEL_INVALIDO);
        if(fase != Fase.AGUARDANDO_PALPITE || jogador.time() != timeDaVez) return Resultado.erro(Protocolo.Erro.FORA_DE_VEZ);

        Carta carta;
        try {
            carta = tabuleiro.revelar(numeroCarta);
        } catch (IllegalArgumentException e) {
            return Resultado.erro(e.getMessage());
        }

        List<Evento> eventos = new ArrayList<>();
        eventos.add(new Evento.Revelar(carta));
 
        CorCarta corRevelada = carta.getCor();

        //1. se for a carta assassina
        if(corRevelada == CorCarta.ASSASSINA){
            CorCarta timeVencedor = (timeDaVez == CorCarta.VERMELHA) ? CorCarta.AZUL : CorCarta.VERMELHA; // seleciona o outro time como campeao
            finalizarJogo(timeVencedor, Protocolo.MotivoVencedor.ASSASSINA);
            eventos.add(new Evento.FimDeJogo(vencedor, motivoFim));
            return Resultado.sucesso(eventos);
        }

        //2. se for a cor do proprio time (cor certa)
        if(corRevelada == timeDaVez){
            if(tabuleiro.cartasRestantes(timeDaVez)==0){
                finalizarJogo(timeDaVez,Protocolo.MotivoVencedor.TODAS_CARTAS);
                eventos.add(new Evento.FimDeJogo(vencedor, motivoFim));
                return Resultado.sucesso(eventos);
            }

            palpitesRestantes--;
            if (palpitesRestantes <= 0) {
                trocarTurno();
                eventos.add(new Evento.FimTurno(timeDaVez));
            }

            return Resultado.sucesso(eventos);
        }

        //3. se for a neutra ou do outro time
        if (corRevelada != CorCarta.NEUTRA) {
            CorCarta timeDaCartaRevelada = corRevelada;
            if (tabuleiro.cartasRestantes(timeDaCartaRevelada) == 0) {
                finalizarJogo(timeDaCartaRevelada, Protocolo.MotivoVencedor.TODAS_CARTAS);
                eventos.add(new Evento.FimDeJogo(vencedor, motivoFim));
                return Resultado.sucesso(eventos);
            }
        }
 
        trocarTurno();
        eventos.add(new Evento.FimTurno(timeDaVez));
        return Resultado.sucesso(eventos);
        
    }

    /** Agente da vez desiste do restante dos palpites. */
    public Resultado passar(Cargo autor) {
        //verificações
        if(fase == Fase.FIM_DE_JOGO || fase == Fase.JOGO_NAO_INICIADO){return Resultado.erro(Protocolo.Erro.FORA_DE_HORA);}
        if(autor.eMestreEspiao()) {return Resultado.erro(Protocolo.Erro.PAPEL_INVALIDO);}
        if(fase != Fase.AGUARDANDO_PALPITE || autor.time() != timeDaVez) {return Resultado.erro(Protocolo.Erro.FORA_DE_VEZ);}
 
        trocarTurno();
        return Resultado.sucesso(new Evento.FimTurno(timeDaVez));
    }

    private void trocarTurno(){
        timeDaVez = (timeDaVez == CorCarta.VERMELHA) ? CorCarta.AZUL : CorCarta.VERMELHA; 
        fase = Fase.AGUARDANDO_DICA;
        palpitesRestantes= 0;
    }

    private void finalizarJogo(CorCarta time, String motivo){
        this.vencedor = time;
        this.motivoFim = motivo;
        fase = Fase.FIM_DE_JOGO;
    }

    public Cargo cargoMestreDaVez() {return (getTimeDaVez() == CorCarta.VERMELHA) ? Cargo.VERMELHA_MESTREESPIAO : Cargo.AZUL_MESTREESPIAO;}
    public Cargo cargoAgenteDaVez() {return (getTimeDaVez() == CorCarta.VERMELHA)  ? Cargo.VERMELHA_AGENTE : Cargo.AZUL_AGENTE;}
}