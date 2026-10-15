# Validação de multiplayer — 0.9.2

A build compila e inicia no ambiente SERVER. Foram executados quatro GameTests em um servidor Minecraft real, com jogadores simulados conectados. Não foi aberta janela nem controlado o computador do usuário.

## Casos aprovados

- Codificação e decodificação dos comandos, estados e arma da fogueira, preservando componentes do item.
- Simulações independentes por jogador; contatos de ataque produzidos pelo controlador do servidor.
- Vários hits válidos sem bloqueio indevido do cooldown vanilla; ataque vanilla não duplica o golpe.
- Rolamento permite atravessamento de projéteis; comandos vencidos não mantêm invulnerabilidade.
- Saldo e atributos isolados, salvos e restaurados; saída da sessão remove o estado de combate e descanso.
- Compras só durante descanso válido; remoção da laje revoga essa autorização imediatamente.
- Visitante sem permissão não altera nem importa regras do anfitrião.
- Morte real de zumbi gera almas recolhidas apenas pelo autor, sem XP vanilla duplicado.
- Criativo não gasta nem aumenta o saldo de sobrevivência; compras gratuitas respeitam o limite de atributos.
- Fogueira afasta esqueleto distante e limpa agressão persistente de Enderman.

Logs: Logs/combat-world-v092-final.log, Logs/build-v092-anger.log e Logs/server-minimal-lifecycle.log.

A inicialização dedicada foi validada sem Controlify, Simply Swords e Vulkan. O runtime nativo distribuído é Windows x64.

## Validação manual ainda necessária

Uma sessão LAN em duas máquinas reais para observar animações remotas, arma na fogueira, latência, configurações compartilhadas e reconexão. Instruções em TESTAR-0.9.2.md. A 0.9.2 é uma prévia; esses testes automáticos não são apresentados como gameplay multiplayer já verificado.
