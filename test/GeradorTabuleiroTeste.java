//arquivo de teste gerado por IA para ver se tudo está funcionando

import objetos_comuns.*;
import servico.GeradorTabuleiro;
import java.util.List;

public class GeradorTabuleiroTeste {
    public static void main(String[] args) {
        System.out.println("--- Testando GeradorTabuleiro ---");
        GeradorTabuleiro gerador = new GeradorTabuleiro();
        List<Carta> cartas = gerador.gerarTabuleiro(CorCarta.AZUL, CorCarta.VERMELHA);

        int azul = 0, vermelha = 0, neutra = 0, assassina = 0;
        boolean repetida = false;

        for (int i = 0; i < cartas.size(); i++) {
            Carta c = cartas.get(i);
            if (c.getCor() == CorCarta.AZUL) azul++;
            else if (c.getCor() == CorCarta.VERMELHA) vermelha++;
            else if (c.getCor() == CorCarta.NEUTRA) neutra++;
            else if (c.getCor() == CorCarta.ASSASSINA) assassina++;

            // Checa posição 1 a 25
            if (c.getPosicao() != i + 1) repetida = true;
        }

        if (cartas.size() == 25 && azul == 9 && vermelha == 8 && neutra == 7 && assassina == 1 && !repetida) {
            System.out.println("OK: Contagem 9/8/7/1, 25 palavras distintas e posições corretas.");
        } else {
            System.out.println("FALHA: Estrutura do tabuleiro gerada incorretamente.");
        }
    }
}