package cliente;

import objetos_comuns.Cargo;
import objetos_comuns.CorCarta;

/**
 * O que o cliente sabe sobre si e sobre a partida. É escrito pela thread que lê o socket
 * e lido pela thread do teclado, por isso os campos são volatile.
 */
public class EstadoCliente {

    /** Em que momento a partida está: LOBBY (ainda não começou), esperando DICA, esperando PALPITE, ou FIM. */
    public enum Fase { LOBBY, DICA, PALPITE, FIM }

    private volatile Cargo cargo;
    private volatile Fase fase = Fase.LOBBY;
    private volatile String timeDaVez;        // "VERMELHA" ou "AZUL", como vem em VEZ_DICA / VEZ_PALPITE
    private volatile int palpitesRestantes;
    private volatile String dicaAtual;        // texto já traduzido da última DICA_DADA

    // ---- cargo (Issue #12)
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }
    public boolean temCargo() { return cargo != null; }
    public boolean ehMestre() { return cargo != null && cargo.eMestreEspiao(); }
    public CorCarta getTime() { return cargo == null ? null : cargo.time(); }

    // ---- turno (Issue #14)
    public Fase getFase() { return fase; }
    public void setFase(Fase fase) { this.fase = fase; }

    public String getTimeDaVez() { return timeDaVez; }
    public void setTimeDaVez(String timeDaVez) { this.timeDaVez = timeDaVez; }

    public int getPalpitesRestantes() { return palpitesRestantes; }
    public void setPalpitesRestantes(int palpitesRestantes) { this.palpitesRestantes = palpitesRestantes; }

    public String getDicaAtual() { return dicaAtual; }
    public void setDicaAtual(String dicaAtual) { this.dicaAtual = dicaAtual; }

    /** Na fase de dica quem age é o mestre do time da vez; na de palpite, o agente do time da vez. */
    public boolean ehMinhaVez() {
        if (cargo == null || timeDaVez == null) return false;
        if (!cargo.time().name().equals(timeDaVez)) return false;
        if (fase == Fase.DICA) return ehMestre();
        if (fase == Fase.PALPITE) return !ehMestre();
        return false;
    }
}
