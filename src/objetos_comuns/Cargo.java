package objetos_comuns;

public enum Cargo {
    VERMELHA_MESTREESPIAO,
    VERMELHA_AGENTE,
    AZUL_MESTREESPIAO,
    AZUL_AGENTE;

    // A qual time este papel pertence
    public CorCarta time() {
        return (this == VERMELHA_MESTREESPIAO || this == VERMELHA_AGENTE) ? CorCarta.VERMELHA : CorCarta.AZUL;
    }

    public boolean eMestreEspiao() {
        return this == VERMELHA_MESTREESPIAO || this == AZUL_MESTREESPIAO;
    }
}


