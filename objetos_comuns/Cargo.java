package objetos_comuns;

public enum Cargo {
    VERMELHO_MESTREESPIAO,
    VERMELHO_AGENTE,
    AZUL_MESTREESPIAO,
    AZUL_AGENTE;

    // A qual time este papel pertence
    public CorCarta time() {
        return (this == VERMELHO_MESTREESPIAO || this == VERMELHO_AGENTE) ? CorCarta.VERMELHO : CorCarta.AZUL;
    }

    public boolean eMestreEspiao() {
        return this == VERMELHO_MESTREESPIAO || this == AZUL_MESTREESPIAO;
    }
}

//so p falar q mudou
