//arquivo de teste gerado por IA para ver se tudo está funcionando

import logica_jogo.*;
import objetos_comuns.*;
import java.io.InputStream;
import java.util.Scanner;

public class PartidaTeste {
    public static void main(String[] args) {
        System.out.println("--- Testando Partida ---");
        
        // Inicializa o jogo do jeito padrão
        Tabuleiro tab = new Tabuleiro(); 
        Partida partida = new Partida(tab);

        // 1. descobrir UMA palavra que está oculta neste tabuleiro
        String palavraNoTabuleiro = "";
        InputStream input = PartidaTeste.class.getResourceAsStream("/resources/palavras.txt");
        
        if (input != null) {
            Scanner scanner = new Scanner(input, "UTF-8");
            while (scanner.hasNextLine()) {
                String palavra = scanner.nextLine().trim();
                // Usa o método que já sabemos que existe para achar uma palavra sorteada!
                if (tab.cartaOcultaNoTabuleiro(palavra)) {
                    palavraNoTabuleiro = palavra;
                    break; 
                }
            }
            scanner.close();
        }

        if (palavraNoTabuleiro.isEmpty()) {
            System.out.println("Erro no teste: não foi possível achar nenhuma palavra no tabuleiro.");
            return;
        }

        // 2. Teste: Dica em minúsculas igual a uma carta do tabuleiro
        // Simulando a palavra encontrada, mas toda em minúsculas
        String dicaMinuscula = palavraNoTabuleiro.toLowerCase(); 
        
        Cargo mestre = partida.cargoMestreDaVez(); // Pega quem é o mestre neste turno
        Resultado resultadoDica = partida.darDica(mestre, dicaMinuscula, 1);
        
        if (resultadoDica.deuSucesso()) {
            System.out.println("FALHA: A dica '" + dicaMinuscula + "' NÃO foi barrada, mas deveria conflitar com '" + palavraNoTabuleiro + "'.");
        } else {
            System.out.println("OK: Dica '" + dicaMinuscula + "' barrada corretamente (" + resultadoDica.getErro() + ").");
        }

        // 3. Teste: Fluxo Feliz (dar dica válida e o agente passar a vez)
        // Usamos uma palavra que garantidamente não está no jogo
        String dicaValida = "Zebra"; 
        
        Resultado resultadoDicaValida = partida.darDica(mestre, dicaValida, 2);
        
        if (resultadoDicaValida.deuSucesso()) {
            Cargo agente = partida.cargoAgenteDaVez(); // Pega o agente do mesmo time
            Resultado resultadoPassar = partida.passar(agente);
            
            if (resultadoPassar.deuSucesso()) {
                System.out.println("OK: Fluxo feliz (dica neutra -> agente passar a vez) executado com sucesso.");
            } else {
                System.out.println("FALHA: Erro ao tentar passar a vez.");
            }
        } else {
            System.out.println("FALHA: Não conseguiu dar a dica válida para o fluxo feliz.");
        }
    }
}