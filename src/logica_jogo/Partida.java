package logica_jogo;

import java.util.ArrayList;
import java.util.List;

import objetos_comuns.Cargo;
import objetos_comuns.Carta;
import objetos_comuns.CorCarta;
import objetos_comuns.Tabuleiro;

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

    public CorCarta getTimeDaVez() {
        return timeDaVez;
    }
 
    public Fase getFase() {
        return fase;
    }
 
    public CorCarta getVencedor() {
        return vencedor;
    }
 
    public String getMotivoFim() {
        return motivoFim;
    }
 
    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }
 
    public int getPalpitesRestantes() {
        return palpitesRestantes;
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Aqui vão ficar todas as ações que um jogador pode fazer, com todas suas exeções

    /**trata a entrada no i/o no servidor msm??? */
    public Resultado darDica(Cargo jogador, String palavra, int numero){
        //primeiro, trata todos os possiveis erros
        if(fase==Fase.JOGO_NAO_INICIADO){
            return Resultado.erro("fora_de_hora");
        }
        if(fase!=Fase.AGUARDANDO_DICA || timeDaVez !=jogador.time()){
            return Resultado.erro("fora_de_vez");
        }
        else if(!jogador.eMestreEspiao()){
            return Resultado.erro("papel_invalido");
        }
        
        // aqui ele ve o numero da palpites é valido: maior ou igual a 1 E menor ou igual ao que o numero de cartas restantes
        if(numero < 1 ||numero > tabuleiro.cartasRestantes(jogador.time())){ 
            return Resultado.erro("numero_invalido");
        } 

        // aqui ele ve se a palvra é uma que ainda ta oculta no tabuleiro, tem um numero no meio ou tem espaços
        String palavraLimpa = palavra.trim();
        if(tabuleiro.cartaOcultaNoTabuleiro(palavra) || palavra.matches(".*\\d.*") ||palavraLimpa.contains(" ") ){
            return Resultado.erro("dica_invalida");
        }

        fase = Fase.AGUARDANDO_PALPITE;
        palpitesRestantes = numero + 1;

        return Resultado.sucesso(new Evento.DicaDada(timeDaVez, palavraLimpa, numero));
    }

    public Resultado chutar(Cargo jogador, int numeroCarta){//colocar no PROTOCOLO.md tds os erros possiveis
        if(fase==Fase.JOGO_NAO_INICIADO){
            return Resultado.erro("fora_de_hora");
        }
        if(jogador.time() != timeDaVez || fase != Fase.AGUARDANDO_PALPITE){
            return Resultado.erro("fora_da_vez"); 
        }
        if(jogador.eMestreEspiao()){
            return Resultado.erro("papel_invalido");
        }
        
        Carta carta = tabuleiro.acharPeloNumero(numeroCarta);
        if(carta == null){
            return Resultado.erro("palpite_invalido");
        }
        if(carta.estaRevelada()){
            return Resultado.erro("carta_ja_revelada");
        }

        carta.revelar();

        List<Evento> eventos = new ArrayList<>();
        eventos.add(new Evento.Revelar(carta));
 
        CorCarta corRevelada = carta.getCor();

        //1. se for a carta do assassino
        if(corRevelada== CorCarta.ASSASSINO){
            CorCarta timeVencedor = (timeDaVez == CorCarta.VERMELHO) ? CorCarta.AZUL : CorCarta.VERMELHO; // seleciona o outro time como campeao
            finalizarJogo(timeVencedor, "ASSASSINO");
            eventos.add(new Evento.FimDeJogo(vencedor, motivoFim));
            return Resultado.sucesso(eventos);
        }

        //2. se for a cor do proprio time (cor certa)
        if(corRevelada == timeDaVez){
            if(tabuleiro.cartasRestantes(timeDaVez)==0){
                finalizarJogo(timeDaVez,"TODAS_PALAVRAS");
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
                finalizarJogo(timeDaCartaRevelada, "TODAS_PALAVRAS");
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
         if(fase==Fase.JOGO_NAO_INICIADO){
            return Resultado.erro("fora_de_hora");
        }
        if (fase != Fase.AGUARDANDO_PALPITE) {
            return Resultado.erro("Não é hora de passar");
        }
        if (autor.eMestreEspiao()) {
            return Resultado.erro("Mestre não passa, só agente");
        }
        if (autor.time() != timeDaVez) {
            return Resultado.erro("Não é a vez do seu time");
        }
 
        trocarTurno();
        return Resultado.sucesso(new Evento.FimTurno(timeDaVez));
    }

    private void trocarTurno(){
        timeDaVez = (timeDaVez == CorCarta.VERMELHO) ? CorCarta.AZUL : CorCarta.VERMELHO; 
        fase = Fase.AGUARDANDO_DICA;
        palpitesRestantes= 0;
    }

    private void finalizarJogo(CorCarta time, String motivo){
        this.vencedor = time;
        this.motivoFim = motivo;
        fase = Fase.FIM_DE_JOGO;
    }

    public Cargo cargoMestreDaVez() {
        return (getTimeDaVez() == CorCarta.VERMELHO) ? Cargo.VERMELHO_MESTREESPIAO : Cargo.AZUL_MESTREESPIAO;
    }

    public Cargo cargoAgenteDaVez() {
        return (getTimeDaVez() == CorCarta.VERMELHO)  ? Cargo.VERMELHO_AGENTE : Cargo.AZUL_AGENTE;
    }

    public static void main(String[] args) {
        // Tabuleiro tabuleiro = new Tabuleiro();
        // Partida partida = new Partida(tabuleiro);
        // Cargo cara; 
        // //Cargo cara = Cargo.AZUL_MESTREESPIAO;
        // if(tabuleiro.getTimeInicio()==CorCarta.AZUL)
        //     cara = Cargo.AZUL_MESTREESPIAO;
        // else
        //     cara = Cargo.VERMELHO_MESTREESPIAO;

        // Resultado r1 = partida.darDica(cara, "animal a", 1);
        // System.out.println(r1.deuSucesso() + " | fase=" + partida.getFase() + "  " + r1.getErro());
    }
}



