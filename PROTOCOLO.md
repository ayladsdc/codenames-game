# Visão Geral
- Transporte: TCP pela confiabilidade do transporte, nada pode ser perdido e precisa ser recebido na ordem de sequência de envio
- Porta padrão: 1996, podendo ser trocada se estiver ocupada
- Jogadores: 4 por partida, dois times (azul e vermelho) com um agente e um mestre
- Codificação: UTF-8
- Quem inicia: O servidor fica parado escutando a porta 1996, e os clientes iniciam a conexão com ele
- Quem fala com quem: clientes conversam com o servidor, que verifica, valida e repassa informações para os outros jogadores
- Adendo: O servidor só aceita uma conexão por jogador, se uma partida estiver cheia (quinta conexão), o servidor rejeita a conexão e envia 'ERRO partida_cheia'

# Formato Mensagens
- Uma por linha, terminando com \n
- Estrutura: 'COMANDO arg1 arg2 ...'
- Comandos devem vir como maiúsculas, e argumentos separados com somente um espaço
- Palavras com espaço PRECISAM estar com '_' ao invés do espaço. Por exemplo 'SANCHO PANÇA' deve estar como 'SANCHO_PANÇA'
- Linhas vazias serão respondidas com 'ERRO linha_vazia' e comandos inválidos com 'ERRO comando_desconhecido'
- Erros serão respondidos com argumentos, sem acento e com '_' no lugar de espaços. Eles não encerram conexão e só vão para quem enviou comandos errados

# Cargos
- Deverão ser informados do cliente -> servidor durante o lobby antes de iniciar a partida
- Estrutura: 'CARGO <NOME_DO_CARGO>'
- Cargos disponíveis: 'VERMELHO_MESTREESPIAO', 'VERMELHO_AGENTE', 'AZUL_MESTREESPIAO', 'AZUL_AGENTE'
- Exemplos: 'CARGO VERMELHO_AGENTE', 'CARGO AZUL_MESTREESPIAO'
- Erros: 
    - 'ERRO cargo_invalido' (provavelmente esqueceu o _, errou a escrita ou cargo não existe), 
    - 'ERRO fora_de_hora' (partida já iniciada ou já encerrada)
    - 'ERRO cargo_ocupado <cargo>' (cargo já escolhido)

# Chutes
- Informado do cliente -> servidor, sendo o agente do time da rodada
- Estrutura: 'CHUTE <posicao>' (1 a 25, que é número de cartas no tabuleiro)
- Numeração: O tabuleiro tem 5 linhas tal como uma matriz com as linhas [1,2,3,4,5] , [6,7,8,9,10] , [11,12,13,14,15] , [16,17,18,19,20] , [21,22,23,24,25]. Cada um desses números é a posição dos chutes
- Exemplo: 'CHUTE 9'
- Erros: 
    - 'ERRO fora_de_vez' (caso mandado fora do turno, enquanto é dada a dica ou a partida não fora iniciada)
    - 'ERRO carta_ja_revelada'
    - 'ERRO posicao_invalida' (numero menor que 1 ou maior que 25)
    - 'ERRO papel_invalido'
    - 'ERRO argumentos_invalidos' (não foi valor int)

# Passa
- Informado de cliente -> servidor, por agentes e na fase de palpite. Pode ser depois de alguns chutes ou logo de cara no turno de palpites
- Serve para caso o agente não saiba o que chutar e não queira arriscar
- Estrutura: 'PASSA'
- Exemplo: 'PASSA'
- Erros: 
    - 'ERRO fora_de_vez' (caso mandado fora do turno, enquanto é dada a dica ou a partida não fora iniciada)
    - 'ERRO papel_invalido'

# Revelar Carta
- Logo após o chute válido, servidor -> todos os jogadores (incluindo quem chutou) com a cor da carta do chute
- Estrutura: 'REVELAR <posicao> <cor>'
- Argumentos: <posicao> é de 1-25, e cor são de 4 tipos: 'VERMELHA', 'AZUL', 'NEUTRA', 'ASSASSINA'
- Exemplo: 'REVELAR 9 VERMELHA'
- Resposta de sucesso dos jogadores para o servidor: 'REVELAR sucesso'

# Fases Jogo
- LOBBY: do início da conexão até todos os 4 cargos estarem ocupados
- DICA: o mestre do time da vez dá a dica
- PALPITE: o agente do time da vez chuta ou passa
- FIM: depois do 'JOGO encerrado'
- Mensagens aceitas (cliente -> servidor) em cada fase:
    - LOBBY: 'CARGO <NOME_DO_CARGO>'
    - DICA: 'DICA <dica>'
    - PALPITE: 'CHUTE <posicao>', 'PASSA'
    - FIM: (nada)

# Dica
- Informado de cliente -> servidor, pelo mestre do time da vez e na fase de dica
- Estrutura: 'DICA <palavra> <numero>'
- Exemplo: 'DICA Itália 2'
- A palavra deve ser uma única palavra (só letras, acentos permitidos, sem '_').
- A palavra não pode ser igual a nenhuma carta ainda oculta do tabuleiro (cartas já reveladas não contam)
- O número vai de 1 a 9. O número de palpites do turno é o número da dica + 1
- Resposta de sucesso: 'JOGO dica_valida' (só para o mestre), seguida de 'DICA_DADA' e 'VEZ_PALPITE' para todos
- Erros:
    - 'ERRO fora_de_hora' (partida não iniciada ou já encerrada)
    - 'ERRO fora_de_vez' (caso mandado fora do turno ou enquanto os agentes estão chutando)
    - 'ERRO papel_invalido' (quem enviou não é mestre)
    - 'ERRO argumentos_invalidos' (faltam ou sobram argumentos, ou o número não é inteiro)
    - 'ERRO dica_invalida' (palavra igual a uma carta oculta, ou com caracteres que não são letras)
    - 'ERRO numero_invalido' (número menor que 1 ou maior que 9)

# Tabuleiro
- É dado de servidor -> cada jogador, uma única vez, logo depois de 'JOGO iniciado'
- Estrutura: 'TABULEIRO_AGENTE <carta1> <carta2> ... <carta25>' e 'TABULEIRO_MESTRE <carta1> ... <carta25>'
- Cada carta é um único argumento: '<posicao>:<palavra>:<cor>:<revelada>'
    - Posicao: de 1 a 25
    - Palavra: com '_' no lugar de espaços
    - Cor: cor da carta; no 'TABULEIRO_AGENTE' vale '?' quando a carta ainda não foi revelada
    - Revelada: '0' (oculta) ou '1' (revelada)
- As 25 cartas vêm em ordem de posição (1 a 25)
- 'TABULEIRO_MESTRE' vai para os mestres; 'TABULEIRO_AGENTE' vai para os agentes
- Exemplo (abreviado): 'TABULEIRO_AGENTE 1:PIZZA:?:0 2:JET_SKI:?:0 ... 25:LUA:?:0'
- Regra de segurança: o servidor nunca envia a um agente a cor de uma carta oculta, em nenhuma mensagem
- Depois do envio inicial, o cliente mantém o tabuleiro e o atualiza a cada 'REVELAR'. O servidor não reenvia o tabuleiro durante a partida
- No fim da partida o servidor envia 'TABULEIRO_FINAL <carta1> ... <carta25>', no mesmo formato do mestre, para todos os jogadores. É a única exceção à regra de segurança, já que o jogo já acabou.
- Placar
   - Direção: servidor -> todos
   - Estrutura: 'PLACAR <vermelho_restantes> <azul_restantes>'
   - Os valores são o número de cartas de cada time que ainda não foram reveladas
   - Exemplo: 'PLACAR 8 8'
   - Enviado uma vez no início do jogo (depois dos tabuleiros) e depois de cada 'REVELAR'

# Placar
- É feito de servidor -> todos, no começo do jogo (depois dos tabuleiros) e depois de cada 'REVELAR'
- Estrutura: 'PLACAR <vermelho_restantes> <azul_restantes>'
- Os valores são o número de cartas de cada time que ainda não foram reveladas, ou seja, está na frente quem está com o menor placar.
- Exemplo: 'PLACAR 6 7'

# Turnos
- Enviadas de servidor -> todos os jogadores. Cada cliente decide o que mostrar a partir do próprio cargo
- 'VEZ_DICA <time>': começa a fase de dica.
- 'DICA_DADA <palavra> <numero>': repassa a dica aceita para os agentes.
- 'VEZ_PALPITE <time> <palpites_restantes>': começa (ou continua) a fase de palpite. Só o agente desse time pode enviar 'CHUTE' ou 'PASSA'. Isso aqui acontece depois de 'DICA_DADA' com número + 1 palpites. Depois de cada acerto que não é encerrado o turno, é apenas descontedo um palpite . Exemplo: 'VEZ_PALPITE VERMELHO 3'
- 'FIM_TURNO <motivo> <proximo_time>': encerra o turno. Sempre seguida de 'VEZ_DICA <proximo_time>'. Exemplo: 'FIM_TURNO errou AZUL'
    - motivo 'errou': a carta revelada era neutra ou do time adversário
    - motivo 'passou': o agente enviou 'PASSA'
    - motivo 'sem_palpites': o agente usou todos os palpites
- Ordem de uma jogada: 'REVELAR', 'PLACAR' e daí 'VEZ_PALPITE' (turno continua) ou 'FIM_TURNO' seguido de 'VEZ_DICA' (turno acabou) ou 'VENCEDOR' (jogo acabou)
- Para 'PASSA', a sequência é 'FIM_TURNO passou <proximo_time>' e 'VEZ_DICA <proximo_time>'

# Fim de jogo
- O jogo termina quando um time consegue todas as suas cartas ou quando um agente escolhe a carta assassina
- Revelar a carta assassina faz perder o time de quem chutou
- Direção: servidor -> todos
- Estrutura: 'VENCEDOR <time> <motivo>'
    - motivo 'todas_cartas': o time encontrou todas as suas cartas
    - motivo 'assassino': o time adversário revelou a carta assassina
- Exemplo: 'VENCEDOR VERMELHO assassino'
- Sequência final: 'VENCEDOR', 'TABULEIRO_FINAL', 'JOGO encerrado'. Depois disso o servidor fecha as conexões

# Mensagens informativas
- Estrutura: 'INFO <texto>'
- Direção: servidor -> um ou todos os jogadores
- Exceção ao formato geral: é a única mensagem em que o texto pode ter espaços. Tudo depois de 'INFO ' é o texto
- O cliente só exibe o texto, e nenhuma ação depende dele
- Exemplo: 'INFO Aguardando jogadores (2/4)'

# Desconexão
- No lobby: o cargo é liberado e 'CARGOS_LIVRES' é reenviada
- Durante a partida: o servidor envia 'INFO <cargo> desconectou. Partida encerrada' aos jogadores restantes, depois 'JOGO encerrado' (sem vencedor), e fecha todas as conexões
- Cliente que perde a conexão com o servidor: mostra uma mensagem e encerra

# Jogo
- Quando todos os 4 jogadores já estiverem conectados e todos tiverem escolhido seus cargos, o servidor inicia o jogo enviando 'JOGO iniciado'
- Assim que algum time tenha conseguido todas as cartas certas ou algum deles tenha escolhido a carta assassina, o jogo é automaticamente encerrado com o servidor enviando 'JOGO encerrado'
- Respostas de Sucesso:
    - CARGO: 'JOGO bem_vindo <cargo>'
    - CHUTE: 'JOGO chute_valido'
    - PASSA: 'JOGO passa_valida'
