package cliente;

import objetos_comuns.Mensagem;

public interface OuvinteMensagens {

    //Chamado uma vez para cada mensagem válida recebida do servidor, na ordem em que chegaram. */
    void aoReceber(Mensagem m);

    //Chamado uma vez quando a conexão termina (servidor caiu, "sair", Ctrl+D...)
    default void aoDesconectar() {}
}